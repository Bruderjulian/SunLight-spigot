package su.nightexpress.sunlight.moduleImpl.nametagsOld;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.NametagsProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.command.NametagsCommandProvider;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsPerms;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.event.PlayerNametagChangeEvent;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.handler.NametagPacketHandler;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.handler.NametagPacketsHandler;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.handler.NametagProtocolHandler;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.listener.NametagsListener;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.menu.TagsMenu;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.model.NametagRule;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.model.NametagTag;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.EconomyUtils;
import su.nightexpress.sunlight.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NametagsModule extends Module implements NametagsProvider {

    private final NametagsSettings settings;
    private final Map<UUID, NametagState> states;
    private final Set<UUID> tablistManaged;

    private NametagPacketHandler packetHandler;
    private TagsMenu menu;

    public NametagsModule(ModuleDefinition<NametagsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new NametagsSettings();
        this.states = new ConcurrentHashMap<>();
        this.tablistManaged = ConcurrentHashMap.newKeySet();
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        this.settings.load(config);
        this.plugin.injectLang(su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsLang.class);
        UserPropertyRegistry.register(NametagProperties.TAG_SELECTED);
        UserPropertyRegistry.register(NametagProperties.RANK_ENABLED);
        UserPropertyRegistry.register(NametagProperties.TAG_ENABLED);

        if (Utils.isInstalled(HookId.PACKET_EVENTS)) {
            this.packetHandler = new NametagPacketsHandler(this.plugin);
        } else if (Utils.isInstalled(HookId.PROTOCOL_LIB)) {
            this.packetHandler = new NametagProtocolHandler(this.plugin);
        } else {
            throw new ModuleLoadException("No packet library installed. Install packetevents or ProtocolLib.");
        }

        this.menu = new TagsMenu(this.plugin, this);
        this.menu.load(this.plugin, FileConfig.load(this.getLocalUIPath(), "tags.yml"));

        this.addListener(new NametagsListener(this.plugin, this));
        this.addTask(this::refresh, this.settings.getUpdateInterval());

        this.plugin.runTask(() -> Utils.onlinePlayers().forEach(player -> {
            this.applyNametag(player);
            this.sendAllTagsTo(player);
        }));
    }

    @Override
    protected void unloadModule() {
        this.clearVisuals();
        this.packetHandler = null;
        this.menu = null;
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(NametagsPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("nametags", new NametagsCommandProvider(this.plugin, this, this.userManager), this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("nametags_prefix", (player, payload) -> {
            return this.applyPlaceholders(player, this.computePrefix(player));
        });

        registry.register("nametags_suffix", (player, payload) -> {
            return this.applyPlaceholders(player, this.computeSuffix(player));
        });

        registry.register("nametags_color", (player, payload) -> {
            String color = this.computeColor(player);
            return color == null ? "white" : color;
        });

        registry.register("nametags_tag", (player, payload) -> {
            NametagTag tag = this.getSelectedTag(player);
            return tag == null || !this.hasTagPartEnabled(player) ? "" : tag.getDisplay();
        });
    }

    // -----------------------------------------------------
    // Configuration / model accessors
    // -----------------------------------------------------

    public NametagsSettings getSettings() {
        return this.settings;
    }

    public Map<String, NametagTag> getTags() {
        return this.settings.getTags();
    }

    public Map<String, NametagRule> getRules() {
        return this.settings.getRules();
    }

    public @Nullable NametagTag getTag(@Nullable String tagId) {
        if (tagId == null || tagId.isBlank())
            return null;
        return this.settings.getTags().get(Utils.lowercase(tagId));
    }

    public @Nullable NametagRule getRankRule(@NotNull Player player) {
        return this.settings.getRules().values().stream()
            .filter(rule -> rule.isRankAvailable(player))
            .max(Comparator.comparingInt(NametagRule::getPriority))
            .orElse(null);
    }

    // -----------------------------------------------------
    // Nametag composition
    // -----------------------------------------------------

    public boolean hasRankPartEnabled(@NotNull Player player) {
        return this.userOf(player).getPropertyOrDefault(NametagProperties.RANK_ENABLED);
    }

    public boolean hasTagPartEnabled(@NotNull Player player) {
        return this.userOf(player).getPropertyOrDefault(NametagProperties.TAG_ENABLED);
    }

    public boolean hasRankPartEnabled(@NotNull SunUser user) {
        return user.getPropertyOrDefault(NametagProperties.RANK_ENABLED);
    }

    public boolean hasTagPartEnabled(@NotNull SunUser user) {
        return user.getPropertyOrDefault(NametagProperties.TAG_ENABLED);
    }

    /**
     * Primary getter of the combined rank+tag prefix (before placeholder
     * resolution).
     */
    public @NotNull String computePrefix(@NotNull Player player) {
        StringBuilder sb = new StringBuilder();
        if (this.hasRankPartEnabled(player)) {
            NametagRule rule = this.getRankRule(player);
            if (rule != null)
                sb.append(rule.getPrefix());
        }
        if (this.hasTagPartEnabled(player)) {
            NametagTag tag = this.getSelectedTag(player);
            if (tag != null)
                sb.append(tag.getPrefix());
        }
        return sb.toString();
    }

    public @NotNull String computeSuffix(@NotNull Player player) {
        StringBuilder sb = new StringBuilder();
        if (this.hasTagPartEnabled(player)) {
            NametagTag tag = this.getSelectedTag(player);
            if (tag != null)
                sb.append(tag.getSuffix());
        }
        if (this.hasRankPartEnabled(player)) {
            NametagRule rule = this.getRankRule(player);
            if (rule != null)
                sb.append(rule.getSuffix());
        }
        return sb.toString();
    }

    /**
     * Returns the effective nametag color, or {@code null} when none set.
     */
    public @Nullable String computeColor(@NotNull Player player) {
        NametagTag tag = this.hasTagPartEnabled(player) ? this.getSelectedTag(player) : null;
        String tagColor = tag == null ? null : tag.getColor();
        String rankColor = this.hasRankPartEnabled(player) ? this.rankColorOf(player) : null;

        if (this.settings.isTagColorOverridesRank())
            return this.firstNonBlank(tagColor, rankColor);
        return this.firstNonBlank(rankColor, tagColor);
    }

    private @Nullable String rankColorOf(@NotNull Player player) {
        NametagRule rule = this.getRankRule(player);
        return rule == null ? null : rule.getColor();
    }

    private @Nullable String firstNonBlank(@Nullable String primary, @Nullable String fallback) {
        if (primary != null && !primary.isBlank())
            return primary;
        if (fallback != null && !fallback.isBlank())
            return fallback;
        return null;
    }

    // -----------------------------------------------------
    // Tag selection
    // -----------------------------------------------------

    public boolean isTagAccessible(@NotNull Player player, @NotNull NametagTag tag) {
        if (!NametagsPerms.hasTagAccess(player, tag.getId()))
            return false;
        String permission = tag.getPermission();
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }

    public @Nullable NametagTag getSelectedTag(@NotNull Player player) {
        SunUser user = this.userOf(player);
        String tagId = user.getPropertyOrDefault(NametagProperties.TAG_SELECTED);
        if (tagId == null || tagId.isBlank())
            return null;
        return this.settings.getTags().get(Utils.lowercase(tagId));
    }

    @Override
    public boolean hasTag(@NotNull Player player) {
        return this.getSelectedTag(player) != null;
    }

    @Override
    public @Nullable String getSelectedTagId(@NotNull Player player) {
        NametagTag tag = this.getSelectedTag(player);
        return tag == null ? null : tag.getId();
    }

    @Override
    public boolean selectTag(@NotNull Player player, @Nullable String tagId) {
        SunUser user = this.userOf(player);
        return this.selectTag(user, player, tagId);
    }

    public boolean selectTag(@NotNull SunUser user, @NotNull Player player, @Nullable String tagId) {
        if (tagId == null || tagId.isBlank() || "none".equalsIgnoreCase(tagId)) {
            return this.clearTag(user, player);
        }

        String normalizedId = Utils.lowercase(tagId);
        NametagTag tag = this.settings.getTags().get(normalizedId);
        if (tag == null) {
            this.sendPrefixed(su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsLang.COMMAND_TAG_ERROR_INVALID,
                    player, replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> normalizedId));
            return false;
        }

        return this.changeTag(user, player, tag, false);
    }

    private boolean changeTag(@NotNull SunUser user, @NotNull Player player, @NotNull NametagTag requestedTag,
            boolean bypassChecks) {
        if (!bypassChecks && !this.validateTagSelection(player, requestedTag)) {
            return false;
        }

        String oldTagId = user.getPropertyOrDefault(NametagProperties.TAG_SELECTED);
        if (oldTagId != null && oldTagId.equalsIgnoreCase(requestedTag.getId())) {
            return true;
        }

        PlayerNametagChangeEvent event = new PlayerNametagChangeEvent(player, oldTagId, requestedTag.getId());
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        String newTagId = event.getNewTagId();
        if (newTagId == null || newTagId.isBlank()) {
            return this.clearTag(user, player, false);
        }

        NametagTag effective = this.settings.getTags().get(Utils.lowercase(newTagId));
        if (effective == null) {
            return false;
        }

        if (!bypassChecks && !effective.getId().equals(requestedTag.getId())
                && !this.validateTagSelection(player, effective)) {
            return false;
        }

        if (!bypassChecks && effective.hasCost() && !EconomyUtils.hasBypass(player, this)) {
            EconomyUtils.withdraw(player, effective.getCost());
        }

        user.setProperty(NametagProperties.TAG_SELECTED, effective.getId());
        user.markDirty();

        this.sendPrefixed(su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsLang.COMMAND_TAG_SELECTED, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, effective::getDisplay));
        this.applyNametag(player);
        return true;
    }

    private boolean validateTagSelection(@NotNull Player player, @NotNull NametagTag tag) {
        if (!this.isTagAccessible(player, tag)) {
            this.sendPrefixed(su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsLang.COMMAND_TAG_ERROR_NO_ACCESS,
                    player, replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, tag::getDisplay));
            return false;
        }

        if (tag.hasCost() && !EconomyUtils.hasBypass(player, this) && !EconomyUtils.canAfford(player, tag.getCost())) {
            this.sendPrefixed(su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsLang.COMMAND_TAG_ERROR_NO_EQUITY,
                    player, replacer -> replacer
                        .with(SLPlaceholders.GENERIC_NAME, tag::getDisplay)
                        .with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(tag.getCost())));
            return false;
        }

        return true;
    }

    public boolean setTag(@NotNull SunUser user, @NotNull Player player, @Nullable String tagId) {
        if (tagId == null || tagId.isBlank() || "none".equalsIgnoreCase(tagId)) {
            String oldTagId = user.getPropertyOrDefault(NametagProperties.TAG_SELECTED);
            return oldTagId == null || oldTagId.isBlank() || this.clearTag(user, player);
        }

        String normalizedId = Utils.lowercase(tagId);
        NametagTag tag = this.settings.getTags().get(normalizedId);
        return tag != null && this.changeTag(user, player, tag, true);
    }

    public boolean clearTag(@NotNull SunUser user, @NotNull Player player) {
        return this.clearTag(user, player, true);
    }

    private boolean clearTag(@NotNull SunUser user, @NotNull Player player, boolean fireEvent) {
        String oldTagId = user.getPropertyOrDefault(NametagProperties.TAG_SELECTED);
        if (oldTagId == null || oldTagId.isBlank()) {
            return false;
        }

        if (fireEvent) {
            PlayerNametagChangeEvent event = new PlayerNametagChangeEvent(player, oldTagId, null);
            this.plugin.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
        }

        user.removeProperty(NametagProperties.TAG_SELECTED);
        user.markDirty();

        this.sendPrefixed(su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsLang.COMMAND_TAG_CLEARED, player);
        this.applyNametag(player);
        return true;
    }

    public void setRankEnabled(@NotNull Player player, boolean enabled) {
        SunUser user = this.userOf(player);
        user.setProperty(NametagProperties.RANK_ENABLED, enabled);
        user.markDirty();

        this.sendPrefixed(su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsLang.NAMETAG_RANK_CHANGED, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_STATE,
                        () -> su.nightexpress.nightcore.core.config.CoreLang.STATE_ON_OFF.get(enabled)));
        this.applyNametag(player);
    }

    public void setTagEnabled(@NotNull Player player, boolean enabled) {
        SunUser user = this.userOf(player);
        user.setProperty(NametagProperties.TAG_ENABLED, enabled);
        user.markDirty();

        this.sendPrefixed(su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsLang.NAMETAG_TAG_CHANGED, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_STATE,
                        () -> su.nightexpress.nightcore.core.config.CoreLang.STATE_ON_OFF.get(enabled)));
        this.applyNametag(player);
    }

    // -----------------------------------------------------
    // Rendering
    // -----------------------------------------------------

    public void applyNametag(@NotNull Player target) {
        if (this.packetHandler == null)
            return;

        List<Player> viewers = snapshotViewersOf(target);
        String prefix = this.applyPlaceholders(target, this.computePrefix(target));
        String suffix = this.applyPlaceholders(target, this.computeSuffix(target));
        String colorRaw = this.computeColor(target);

        NametagState state = this.states.get(target.getUniqueId());
        if (prefix.isEmpty() && suffix.isEmpty() && colorRaw == null) {
            this.removeNametag(target);
            return;
        }

        NamedTextColor color = NamedTextColor.NAMES.valueOr(colorRaw == null ? "white" : colorRaw, NamedTextColor.WHITE);

        if (state != null && state.matches(prefix, suffix, color)) {
            return;
        }

        if (state == null) {
            state = new NametagState();
            this.states.put(target.getUniqueId(), state);
        }
        state.update(prefix, suffix, color);
        this.packetHandler.applyTeam(target, prefix, suffix, color, viewers);
        this.updateTablist(target);
    }

    public void removeNametag(@NotNull Player target) {
        NametagState state = this.states.remove(target.getUniqueId());
        if (this.packetHandler != null && state != null) {
            this.packetHandler.removeTeam(target, snapshotViewers());
        }
        this.updateTablist(target);
    }

    public void handleQuit(@NotNull Player player) {
        NametagState state = this.states.remove(player.getUniqueId());
        if (this.packetHandler != null && state != null) {
            this.packetHandler.removeTeam(player, snapshotViewers());
        }
        this.tablistManaged.remove(player.getUniqueId());
    }

    public void handleWorldChange(@NotNull Player player, @NotNull World previousWorld) {
        if (this.packetHandler != null) {
            this.packetHandler.removeTeam(player, snapshotViewersInWorld(previousWorld));

            World currentWorld = player.getWorld();
            List<Player> viewer = List.of(player);
            this.states.keySet().forEach(uuid -> {
                if (uuid.equals(player.getUniqueId())) return;

                Player target = Utils.getPlayer(uuid);
                if (target == null || !target.isOnline() || target.getWorld().equals(currentWorld)) return;

                this.packetHandler.removeTeam(target, viewer);
            });
        }

        this.states.remove(player.getUniqueId());
        this.applyNametag(player);
        this.sendAllTagsTo(player);
    }

    /**
     * (Re)sends all nametag teams to a viewer. Needed on join / world change, as
     * team packets are client-side and a fresh client knows none of them.
     */
    public void sendAllTagsTo(@NotNull Player viewer) {
        if (this.packetHandler == null || this.states.isEmpty())
            return;

        World world = viewer.getWorld();
        List<Player> single = List.of(viewer);
        this.states.forEach((uuid, state) -> {
            if (uuid.equals(viewer.getUniqueId()))
                return;
            Player target = Utils.getPlayer(uuid);
            if (target == null || !target.isOnline())
                return;
            if (!target.getWorld().equals(world))
                return;
            this.packetHandler.applyTeam(target, state.prefix, state.suffix, state.color, single);
        });
    }

    private void refresh() {
        if (this.packetHandler == null)
            return;

        List<Player> viewers = snapshotViewers();
        if (viewers.isEmpty())
            return;

        this.states.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            Player player = Utils.getPlayer(uuid);
            if (player == null || !player.isOnline())
                return true;

            this.applyNametag(player);
            return false;
        });
    }

    private void updateTablist(@NotNull Player player) {
        if (!this.settings.isTablistEnabled()) {
            this.restoreTablistIfManaged(player);
            return;
        }

        if (this.plugin.nickProvider().map(provider -> provider.hasNickname(player)).orElse(false)) {
            this.tablistManaged.remove(player.getUniqueId());
            return;
        }

        String prefix = this.applyPlaceholders(player, this.computePrefix(player));
        String suffix = this.applyPlaceholders(player, this.computeSuffix(player));
        NametagState state = this.states.get(player.getUniqueId());

        if (state == null) {
            this.restoreTablistIfManaged(player);
            return;
        }

        String combined = prefix + SLPlaceholders.PLAYER_NAME + suffix;
        String name = PlaceholderContext.builder()
            .with(CommonPlaceholders.PLAYER.resolver(player))
            .andThen(SLPlaceholders.forPlaceholderAPI(player))
            .build()
            .apply(combined);
        Players.setPlayerListName(player, name);
        this.tablistManaged.add(player.getUniqueId());
    }

    private void restoreTablistIfManaged(@NotNull Player player) {
        if (!this.tablistManaged.remove(player.getUniqueId())) return;
        if (this.plugin.nickProvider().map(provider -> provider.hasNickname(player)).orElse(false)) return;

        Players.setPlayerListName(player, player.getName());
    }

    private @NotNull String applyPlaceholders(@NotNull Player player, @NotNull String text) {
        if (text.isEmpty())
            return text;
        return PlaceholderContext.builder()
            .with(CommonPlaceholders.PLAYER.resolver(player))
            .andThen(SLPlaceholders.forPlaceholderAPI(player))
            .build()
            .apply(text);
    }

    // -----------------------------------------------------
    // Misc
    // -----------------------------------------------------

    public void openTagsMenu(@NotNull Player player) {
        if (this.menu != null) {
            this.menu.open(player);
            return;
        }
        this.sendPrefixed(su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsLang.ERROR_MENU_UNAVAILABLE, player);
    }

    public void reload() {
        this.clearVisuals();
        this.settings.load(this.getConfig());
        Utils.onlinePlayers().forEach(player -> {
            this.applyNametag(player);
            this.sendAllTagsTo(player);
        });
    }

    private void clearVisuals() {
        if (this.packetHandler != null) {
            List<Player> viewers = snapshotViewers();
            this.states.keySet().forEach(uuid -> {
                Player player = Utils.getPlayer(uuid);
                if (player == null || !player.isOnline()) return;
                this.packetHandler.removeTeam(player, viewers);
            });
        }

        this.states.clear();
        Utils.onlinePlayers().forEach(this::restoreTablistIfManaged);
    }

    private @NotNull SunUser userOf(@NotNull Player player) {
        return this.userManager.getOrFetch(player);
    }

    private static @NotNull List<Player> snapshotViewers() {
        return List.copyOf(Bukkit.getServer().getOnlinePlayers());
    }

    private static @NotNull List<Player> snapshotViewersOf(@NotNull Player target) {
        return snapshotViewersInWorld(target.getWorld());
    }

    private static @NotNull List<Player> snapshotViewersInWorld(@NotNull World world) {
        List<Player> viewers = new ArrayList<>();
        for (Player viewer : Bukkit.getServer().getOnlinePlayers()) {
            if (viewer.getWorld().equals(world)) {
                viewers.add(viewer);
            }
        }
        return viewers;
    }

    private static final class NametagState {
        private String prefix = "";
        private String suffix = "";
        private NamedTextColor color = NamedTextColor.WHITE;

        private boolean matches(String prefix, String suffix, NamedTextColor color) {
            return this.prefix.equals(prefix) && this.suffix.equals(suffix) && this.color == color;
        }

        private void update(String prefix, String suffix, NamedTextColor color) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.color = color;
        }
    }
}