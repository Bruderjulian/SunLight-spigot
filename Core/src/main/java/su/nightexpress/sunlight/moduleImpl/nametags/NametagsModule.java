package su.nightexpress.sunlight.moduleImpl.nametags;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.NametagsProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.nametags.command.NametagsCommandProvider;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsPerms;
import su.nightexpress.sunlight.moduleImpl.nametags.event.PlayerTagChangeEvent;
import su.nightexpress.sunlight.moduleImpl.nametags.event.PlayerTagPurchaseEvent;
import su.nightexpress.sunlight.moduleImpl.nametags.hook.GroupSource;
import su.nightexpress.sunlight.moduleImpl.nametags.hook.LuckPermsGroupSource;
import su.nightexpress.sunlight.moduleImpl.nametags.hook.PermissionGroupSource;
import su.nightexpress.sunlight.moduleImpl.nametags.hook.TeamSource;
import su.nightexpress.sunlight.moduleImpl.nametags.hook.UltimateTeamsSource;
import su.nightexpress.sunlight.moduleImpl.nametags.menu.AdminMainMenu;
import su.nightexpress.sunlight.moduleImpl.nametags.menu.ProfilesMenu;
import su.nightexpress.sunlight.moduleImpl.nametags.menu.TagsMenu;
import su.nightexpress.sunlight.moduleImpl.nametags.model.NameplateVisibility;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;
import su.nightexpress.sunlight.moduleImpl.nametags.model.PurchaseResult;
import su.nightexpress.sunlight.moduleImpl.nametags.model.RankDefinition;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;
import su.nightexpress.sunlight.moduleImpl.nametags.render.Nameplate;
import su.nightexpress.sunlight.moduleImpl.nametags.render.TabNameTagBackend;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.EconomyUtils;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Set;
import java.util.function.Function;

/**
 * Owns the nametag feature and wires its collaborators together.
 * <p>
 * Requires TAB: it is the single renderer of nametags, and the module refuses
 * to load without it rather than silently falling back to packet teams that
 * would then fight TAB for control of the same scoreboard.
 */
public class NametagsModule extends Module implements NametagsProvider {

    private final NametagsSettings settings;
    private NametagsCatalog catalog;
    private AccessService access;
    private NameplateService nameplates;
    private TabNameTagBackend backend;
    private TagsMenu menu;

    private GroupSource groupSource;
    private TeamSource teamSource;

    /** Placeholder payload meaning "give me the raw id, not the display name". */
    private static final String PAYLOAD_ID = "id";
    /** Placeholder payload meaning "give me the display name, not the raw id". */
    private static final String PAYLOAD_DISPLAY = "display";

    public NametagsModule(ModuleDefinition<NametagsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new NametagsSettings();
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        this.settings.load(config);

        this.plugin.injectLang(NametagsLang.class);

        UserPropertyRegistry.register(NametagsProperties.PROFILE);
        UserPropertyRegistry.register(NametagsProperties.RANK);
        UserPropertyRegistry.register(NametagsProperties.TAG);
        UserPropertyRegistry.register(NametagsProperties.GRANTS);
        UserPropertyRegistry.register(NametagsProperties.VISIBILITY);

        this.catalog = new NametagsCatalog(this.settings);
        this.catalog.reload();

        this.access = new AccessService(this.userManager);

        this.backend = new TabNameTagBackend(this.plugin,
                this::placeholderPrefix,
                this::placeholderSuffix,
                this::placeholderName);
        this.backend.registerPlaceholders();

        this.nameplates = new NameplateService(this, this.settings, this.catalog, this.access,
                this.userManager, this.backend, null, null, this::resolveGlowColor);
        this.refreshSources();

        this.addListener(new NametagsListener(this.plugin, this));
        this.addTask(this.nameplates::recomputeAll, this.settings.getUpdateInterval());
        this.addTask(this::sweepExpired, this.settings.getExpiryCheckInterval());

        this.menu = new TagsMenu(this.plugin, this);
        this.menu.load(this.plugin, FileConfig.load(this.getLocalUIPath(), "tags.yml"));

        this.plugin.runTask(() -> {
            this.nameplates.recomputeAll();
            this.plugin.debug("Nametags module ready (TAB backend active).");
        });
    }

    @Override
    protected void unloadModule() {
        if (this.nameplates != null) {
            this.nameplates.clear();
        }
        if (this.backend != null) {
            this.backend.unregisterPlaceholders();
        }
        if (this.teamSource != null) {
            this.teamSource.shutdown();
        }
        if (this.groupSource != null) {
            this.groupSource.shutdown();
        }
        this.nameplates = null;
        this.backend = null;
        this.menu = null;
        this.teamSource = null;
        this.groupSource = null;
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(NametagsPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("nametags",
                new NametagsCommandProvider(this.plugin, this, this.userManager), this);
    }

    /**
     * {@link PlaceholderRegistry} strips trailing underscore segments until it
     * finds a registered key, so {@code nametags_tag_id} resolves to the
     * {@code nametags_tag} handler with the payload {@code "id"} rather than to a
     * key of its own. A new key is therefore either unique, or a suffix of an
     * existing one and must be handled by its parent. That is why the tag, rank and
     * profile handlers below look at the payload.
     */
    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        NameplateService nameplates = this.nameplates;

        registry.register("nametags_prefix", (player, payload) -> nameplates.getPrefixFor(player.getUniqueId()));
        registry.register("nametags_suffix", (player, payload) -> nameplates.getSuffixFor(player.getUniqueId()));
        registry.register("nametags_color",
                (player, payload) -> nameplates.getLastComposed(player.getUniqueId()).color());
        registry.register("nametags_glow",
                (player, payload) -> nameplates.getLastComposed(player.getUniqueId()).glow());
        registry.register("nametags_formatted_name", (player, payload) -> this.formattedName(player));
        registry.register("nametags_team", (player, payload) -> this.teamDisplay(player));

        registry.register("nametags_tag", (player, payload) -> {
            TagDefinition tag = this.getSelectedTag(player);
            if (tag == null)
                return "";
            return PAYLOAD_ID.equals(payload) ? tag.getId() : tag.getDisplay();
        });
        registry.register("nametags_rank", (player, payload) -> {
            String id = this.getSelectedRankId(player);
            if (id == null || id.isEmpty())
                return "";
            // The bare key has always meant the id, so it is left alone for compatibility.
            return PAYLOAD_DISPLAY.equals(payload) ? this.rankDisplay(id) : id;
        });
        registry.register("nametags_profile", (player, payload) -> {
            Profile profile = this.getSelectedProfile(player);
            if (profile == null)
                return "";
            return PAYLOAD_ID.equals(payload) ? profile.getId() : profile.getDisplay();
        });

        registry.register("nametags_show_all", (player, payload) -> String.valueOf(this.isNameplateVisible(player)));
        registry.register("nametags_show_rank", (player, payload) -> String.valueOf(this.isRankVisible(player)));
        registry.register("nametags_show_tag", (player, payload) -> String.valueOf(this.isTagVisible(player)));
        registry.register("nametags_show_team", (player, payload) -> String.valueOf(this.isTeamVisible(player)));
        // A profile is a source rather than a rendered part, so this reports the
        // explicit
        // choice and whether it is currently being rendered at all.
        registry.register("nametags_show_profile", (player, payload) -> String
                .valueOf(this.isNameplateVisible(player) && this.getSelectedProfile(player) != null));
    }

    /**
     * Prefix, name and suffix as one string. The prefix already ends in the winning
     * colourcode, so the name renders coloured, which is what makes this usable on
     * a tab list.
     */
    private @NotNull String formattedName(@NotNull Player player) {
        Nameplate nameplate = this.nameplates.getLastComposed(player.getUniqueId());
        return nameplate.prefix() + player.getName() + nameplate.suffix();
    }

    /** The resolved team prefix, or the team display name when there is one. */
    private @NotNull String teamDisplay(@NotNull Player player) {
        return this.nameplates.getLastTeam(player.getUniqueId()).display();
    }

    private @NotNull String rankDisplay(@NotNull String rankId) {
        RankDefinition rank = this.catalog.getRank(rankId);
        return rank == null ? rankId : rank.getDisplay();
    }

    // -----------------------------------------------------
    // Sources
    // -----------------------------------------------------

    private @NotNull GroupSource resolveGroupSource() {
        if (this.groupSource != null)
            return this.groupSource;

        if (this.settings.isUseLuckPerms()) {
            LuckPermsGroupSource luckPerms = new LuckPermsGroupSource(this.plugin);
            if (luckPerms.isAvailable()) {
                this.groupSource = luckPerms;
                this.plugin.debug("Ranks resolved from LuckPerms.");
                return this.groupSource;
            }
        }

        this.groupSource = new PermissionGroupSource();
        this.plugin.debug("LuckPerms unavailable, falling back to permission groups.");
        return this.groupSource;
    }

    private @Nullable TeamSource resolveTeamSource() {
        if (this.teamSource != null)
            return this.teamSource;
        if (!this.settings.isUseUltimateTeams())
            return null;

        UltimateTeamsSource source = new UltimateTeamsSource(this.plugin);
        if (source.isAvailable()) {
            this.plugin.debug("UltimateTeams team prefixes enabled.");
            this.teamSource = source;
            return this.teamSource;
        }
        return null;
    }

    /**
     * Discards the current hook sources and re-resolves them from the settings, so
     * toggling a hook in the config takes effect on reload. Every source is cached,
     * because its cache and its listeners must be the same instance the nameplate
     * service reads through.
     */
    private void refreshSources() {
        if (this.groupSource != null) {
            this.groupSource.shutdown();
            this.groupSource = null;
        }
        if (this.teamSource != null) {
            this.teamSource.shutdown();
            this.teamSource = null;
        }
        if (this.nameplates == null)
            return;

        GroupSource groupSource = this.resolveGroupSource();
        TeamSource teamSource = this.resolveTeamSource();

        this.nameplates.setSources(groupSource, teamSource);
        groupSource.registerListeners(this.nameplates::recomputeAll);
        if (teamSource != null)
            teamSource.registerListeners(this.nameplates::recomputeAll);
    }

    /**
     * Resolves the player's active glow colour through the API provider, so the
     * nametags module never compiles against the glow implementation.
     * <p>
     * Resolved per call rather than bound once, because module load order is not
     * guaranteed and the glow module may be disabled entirely.
     */
    private @Nullable String resolveGlowColor(@NotNull Player player) {
        if (!this.settings.isResolveGlowColor())
            return null;

        return this.plugin.glowProvider()
                .map(provider -> provider.getGlowColor(player))
                .orElse(null);
    }

    // -----------------------------------------------------
    // TAB placeholder suppliers
    // -----------------------------------------------------

    private @NotNull String placeholderPrefix(@NotNull java.util.UUID playerId) {
        return this.nameplates == null ? "" : this.nameplates.getPrefixFor(playerId);
    }

    private @NotNull String placeholderSuffix(@NotNull java.util.UUID playerId) {
        return this.nameplates == null ? "" : this.nameplates.getSuffixFor(playerId);
    }

    /**
     * Feeds TAB's formatted-name placeholder. TAB hands us the player name
     * alongside the UUID, so no Bukkit lookup is needed on whatever thread it
     * refreshes on.
     */
    private @NotNull String placeholderName(@NotNull java.util.UUID playerId, @NotNull String playerName) {
        if (this.nameplates == null)
            return "";

        Nameplate nameplate = this.nameplates.getLastComposed(playerId);
        return nameplate.prefix() + playerName + nameplate.suffix();
    }

    private void sweepExpired() {
        if (this.access == null)
            return;

        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            SunUser user = this.userManager.getOrFetch(player);
            Set<String> expired = this.access.sweepExpired(user, now);
            if (expired.isEmpty())
                continue;

            this.notifyExpired(player, expired);
            this.nameplates.recompute(player);
        }
    }

    private void notifyExpired(@NotNull Player player, @NotNull Set<String> tagIds) {
        for (String tagId : tagIds) {
            TagDefinition tag = this.catalog.getTag(tagId);
            // A tag deleted from the config can still have a stale grant, so fall back to
            // the id.
            String name = tag == null ? tagId : tag.getDisplay();
            this.sendPrefixed(NametagsLang.COMMAND_TAG_EXPIRED, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> name));
        }
    }

    /**
     * Re-reads the settings file, re-resolves the hook sources and recomputes every
     * player.
     */
    public void reload() {
        this.settings.load(this.getConfig());
        this.catalog.reload();
        this.refreshSources();
        this.nameplates.clear();
        this.nameplates.recomputeAll();
    }

    /**
     * Persists the in-memory catalog to settings.yml and recomputes every player.
     * Called by the admin GUI after each edit so changes survive a restart.
     */
    public void saveCatalog() {
        FileConfig config = this.getConfig();
        this.settings.save(config, this.catalog.getRanksById(), this.catalog.getTagsById(),
                this.catalog.getProfilesById());
        config.saveChanges();

        this.settings.load(config);
        this.catalog.reload();
        this.nameplates.clear();
        this.nameplates.recomputeAll();
    }

    // -----------------------------------------------------
    // API
    // -----------------------------------------------------

    public NametagsSettings getSettings() {
        return this.settings;
    }

    public NametagsCatalog getCatalog() {
        return this.catalog;
    }

    public AccessService getAccess() {
        return this.access;
    }

    public NameplateService getNameplates() {
        return this.nameplates;
    }

    public void openTagsMenu(@NotNull Player player) {
        if (this.menu == null) {
            this.sendPrefixed(NametagsLang.ERROR_MENU_UNAVAILABLE, player);
            return;
        }
        this.menu.open(player);
    }

    public void openProfilesMenu(@NotNull Player player) {
        new ProfilesMenu(this.plugin, this).open(player);
    }

    public void openAdminMenu(@NotNull Player player) {
        new AdminMainMenu(this.plugin, this).open(player);
    }

    /**
     * Buys or subscribes to a tag on the player's behalf, firing
     * {@link PlayerTagPurchaseEvent} on success.
     *
     * @return {@link PurchaseResult#SUCCESS} or the reason the purchase was refused
     */
    public @NotNull PurchaseResult purchaseTag(@NotNull Player player,
            @NotNull TagDefinition tag) {
        PurchaseResult result = this.access.purchase(player, tag);
        if (!result.isSuccess())
            return result;

        boolean bypassCost = EconomyUtils.hasBypass(player, NametagsPerms.BYPASS_COST);

        var grant = this.access.getGrant(this.userManager.getOrFetch(player), tag.getId());
        if (grant != null) {
            this.plugin.getPluginManager().callEvent(new PlayerTagPurchaseEvent(player, tag,
                    (bypassCost || !tag.hasPrice()) ? 0D : tag.getPrice(), grant.getExpiresAt()));
        }
        return result;
    }

    @Override
    public boolean hasTag(@NotNull Player player) {
        return this.getSelectedTag(player) != null;
    }

    @Override
    public @Nullable String getSelectedTagId(@NotNull Player player) {
        TagDefinition tag = this.getSelectedTag(player);
        return tag == null ? null : tag.getId();
    }

    @Override
    public boolean selectTag(@NotNull Player player, @Nullable String tagId) {
        return this.selectTag(this.userManager.getOrFetch(player), tagId);
    }

    public boolean selectTag(@NotNull SunUser user, @Nullable String tagId) {
        Player target = user.player().orElse(null);
        String id = tagId == null || tagId.isBlank() ? null : Utils.lowercase(tagId);
        if (id != null && !this.catalog.hasTag(id))
            return false;

        // Only online players can be observed by listeners, so offline edits skip the
        // event.
        if (target != null) {
            String oldId = user.getPropertyOrDefault(NametagsProperties.TAG);
            if (oldId != null && oldId.equals(id))
                return true;
            if (oldId == null && id == null)
                return true;

            PlayerTagChangeEvent event = new PlayerTagChangeEvent(target,
                    oldId == null || oldId.isBlank() ? null : oldId, id);
            this.plugin.getPluginManager().callEvent(event);
            if (event.isCancelled())
                return false;

            // A listener may rewrite the replacement, so re-validate before persisting it.
            String replaced = event.getNewTag();
            if (replaced == null || replaced.isBlank()) {
                id = null;
            } else {
                id = Utils.lowercase(replaced);
                if (!this.catalog.hasTag(id))
                    return false;
            }
        }

        if (id == null) {
            user.removeProperty(NametagsProperties.TAG);
        } else {
            user.setProperty(NametagsProperties.TAG, id);
        }
        if (target != null)
            this.nameplates.recompute(target);
        return true;
    }

    public @Nullable TagDefinition getSelectedTag(@NotNull Player player) {
        SunUser user = this.userManager.getOrFetch(player);
        return this.catalog.getTag(user.getPropertyOrDefault(NametagsProperties.TAG));
    }

    public @Nullable String getSelectedProfileId(@NotNull Player player) {
        return this.userManager.getOrFetch(player).getPropertyOrDefault(NametagsProperties.PROFILE);
    }

    public boolean selectProfile(@NotNull Player player, @Nullable String profileId) {
        SunUser user = this.userManager.getOrFetch(player);
        if (profileId == null || profileId.isBlank()) {
            user.removeProperty(NametagsProperties.PROFILE);
            this.nameplates.recompute(player);
            return true;
        }
        String id = Utils.lowercase(profileId);
        if (this.catalog.getProfile(id) == null)
            return false;

        user.setProperty(NametagsProperties.PROFILE, id);
        this.nameplates.recompute(player);
        return true;
    }

    public @Nullable Profile getSelectedProfile(@NotNull Player player) {
        return this.catalog.getProfile(this.getSelectedProfileId(player));
    }

    public @Nullable String getSelectedRankId(@NotNull Player player) {
        return this.userManager.getOrFetch(player).getPropertyOrDefault(NametagsProperties.RANK);
    }

    public boolean selectRank(@NotNull Player player, @Nullable String rankId) {
        SunUser user = this.userManager.getOrFetch(player);
        if (rankId == null || rankId.isBlank()) {
            user.removeProperty(NametagsProperties.RANK);
            this.nameplates.recompute(player);
            return true;
        }
        String id = Utils.lowercase(rankId);
        if (this.catalog.getRank(id) == null)
            return false;

        user.setProperty(NametagsProperties.RANK, id);
        this.nameplates.recompute(player);
        return true;
    }

    @Override
    public boolean hasAccess(@NotNull Player player, @NotNull String tagId) {
        TagDefinition tag = this.catalog.getTag(tagId);
        return tag != null && this.access.hasAccess(player, tag);
    }

    @Override
    public boolean grantTag(@NotNull Player player, @NotNull String tagId) {
        TagDefinition tag = this.catalog.getTag(tagId);
        if (tag == null)
            return false;

        SunUser user = this.userManager.getOrFetch(player);
        boolean granted = this.access.grant(user, tag, System.currentTimeMillis());
        if (granted)
            this.nameplates.recompute(player);
        return granted;
    }

    @Override
    public boolean revokeTag(@NotNull Player player, @NotNull String tagId) {
        SunUser user = this.userManager.getOrFetch(player);
        boolean revoked = this.access.revoke(user, Utils.lowercase(tagId));
        if (revoked)
            this.nameplates.recompute(player);
        return revoked;
    }

    @Override
    public void recompute(@NotNull Player player) {
        if (this.nameplates != null)
            this.nameplates.recompute(player);
    }

    @Override
    public void recomputeAll() {
        if (this.nameplates != null)
            this.nameplates.recomputeAll();
    }

    // -----------------------------------------------------
    // Visibility
    // -----------------------------------------------------

    public @NotNull NameplateVisibility getVisibility(@NotNull Player player) {
        return this.getVisibility(this.userManager.getOrFetch(player));
    }

    public @NotNull NameplateVisibility getVisibility(@NotNull SunUser user) {
        NameplateVisibility visibility = user.getPropertyOrDefault(NametagsProperties.VISIBILITY);
        return visibility == null ? NameplateVisibility.DEFAULT : visibility.normalize();
    }

    @Override
    public boolean isRankVisible(@NotNull Player player) {
        return this.getVisibility(player).wantsRank();
    }

    @Override
    public boolean isTagVisible(@NotNull Player player) {
        return this.getVisibility(player).wantsTag();
    }

    @Override
    public boolean isTeamVisible(@NotNull Player player) {
        return this.getVisibility(player).wantsTeam();
    }

    @Override
    public boolean isNameplateVisible(@NotNull Player player) {
        return this.getVisibility(player).showAll();
    }

    public void setVisibility(@NotNull Player player, @NotNull NameplateVisibility visibility) {
        this.setVisibility(this.userManager.getOrFetch(player), visibility);
    }

    public void setVisibility(@NotNull SunUser user, @NotNull NameplateVisibility visibility) {
        user.setProperty(NametagsProperties.VISIBILITY, visibility);
        user.player().ifPresent(this.nameplates::recompute);
    }

    public @NotNull NameplateVisibility toggleRank(@NotNull Player player) {
        return this.mutateVisibility(player, NameplateVisibility::toggleRank);
    }

    public @NotNull NameplateVisibility toggleTag(@NotNull Player player) {
        return this.mutateVisibility(player, NameplateVisibility::toggleTag);
    }

    public @NotNull NameplateVisibility toggleTeam(@NotNull Player player) {
        return this.mutateVisibility(player, NameplateVisibility::toggleTeam);
    }

    public @NotNull NameplateVisibility toggleAll(@NotNull Player player) {
        return this.mutateVisibility(player, NameplateVisibility::toggleAll);
    }

    @Override
    public void setNameplateVisible(@NotNull Player player, boolean visible) {
        this.setVisibility(player, this.getVisibility(player).withAll(visible));
    }

    private @NotNull NameplateVisibility mutateVisibility(@NotNull Player player,
            @NotNull Function<NameplateVisibility, NameplateVisibility> mutator) {
        SunUser user = this.userManager.getOrFetch(player);
        NameplateVisibility updated = mutator.apply(this.getVisibility(user));
        this.setVisibility(user, updated);
        return updated;
    }
}
