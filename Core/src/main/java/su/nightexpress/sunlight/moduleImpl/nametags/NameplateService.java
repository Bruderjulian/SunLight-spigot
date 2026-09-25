package su.nightexpress.sunlight.moduleImpl.nametags;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.moduleImpl.nametags.hook.GroupSource;
import su.nightexpress.sunlight.moduleImpl.nametags.hook.PermissionGroupSource;
import su.nightexpress.sunlight.moduleImpl.nametags.hook.TeamInfo;
import su.nightexpress.sunlight.moduleImpl.nametags.hook.TeamSource;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;
import su.nightexpress.sunlight.moduleImpl.nametags.model.RankDefinition;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;
import su.nightexpress.sunlight.moduleImpl.nametags.render.Nameplate;
import su.nightexpress.sunlight.moduleImpl.nametags.render.Part;
import su.nightexpress.sunlight.moduleImpl.nametags.render.PrefixComposer;
import su.nightexpress.sunlight.moduleImpl.nametags.render.TabNameTagBackend;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single entry point for building and applying a player's nameplate.
 * <p>
 * TAB owns the actual rendering, so this class never writes packets. It resolves the four
 * contributing parts (team, rank, tag, glow), composes them and hands the result to the
 * backend, which drops redundant writes.
 * <p>
 * Group and team lookups are asynchronous, so recomputation returns a future and only ever
 * touches Bukkit state on the main thread.
 */
public class NameplateService {

    private final Module module;
    private final NametagsSettings settings;
    private final NametagsCatalog catalog;
    private final AccessService access;
    private final UserManager userManager;
    private final TabNameTagBackend backend;

    private volatile @NotNull GroupSource groupSource;
    private volatile @Nullable TeamSource teamSource;
    private final @Nullable GlowColorSource glowSource;

    /** Last composed nameplate per player, kept so placeholder requests stay cheap. */
    private final Map<UUID, Nameplate> lastComposed = new ConcurrentHashMap<>();

    public NameplateService(@NotNull Module module,
            @NotNull NametagsSettings settings,
            @NotNull NametagsCatalog catalog,
            @NotNull AccessService access,
            @NotNull UserManager userManager,
            @NotNull TabNameTagBackend backend,
            @Nullable GroupSource groupSource,
            @Nullable TeamSource teamSource,
            @Nullable GlowColorSource glowSource
    ) {
        this.module = module;
        this.settings = settings;
        this.catalog = catalog;
        this.access = access;
        this.userManager = userManager;
        this.backend = backend;
        this.groupSource = groupSource;
        this.teamSource = teamSource;
        this.glowSource = glowSource;
    }

    /**
     * Swaps the group and team sources, used when a reload re-resolves them from the
     * settings. Both are read per recompute, so no cached nameplate goes stale.
     */
    public void setSources(@NotNull GroupSource groupSource, @Nullable TeamSource teamSource) {
        this.groupSource = groupSource;
        this.teamSource = teamSource;
    }

    /**
     * Supplies the player's current glow colour name. Implemented by the glow module so the
     * nametags module does not depend on it.
     */
    public interface GlowColorSource {
        @Nullable String getGlowColor(@NotNull Player player);
    }

    // -----------------------------------------------------
    // Computation
    // -----------------------------------------------------

    /**
     * Recomputes a player's nameplate and pushes it to TAB.
     * <p>
     * Group and team lookups may be async, so the chain returns futures and always lands
     * back on the main thread before touching Bukkit or TAB state.
     */
    public void recompute(@NotNull Player player) {
        UUID id = player.getUniqueId();
        if (!player.isOnline()) {
            this.lastComposed.remove(id);
            return;
        }

        GroupSource groupSource = this.groupSource;
        if (groupSource == null) return;

        groupSource.resolveGroups(player)
                .thenCombine(this.resolveTeam(player), (groups, team) -> new Resolved(groups, team))
                .exceptionally(throwable -> new Resolved(Set.of(), TeamInfo.EMPTY))
                .thenAccept(resolved -> this.module.plugin().runTask(
                        () -> this.apply(player, resolved.groups(), resolved.team())));
    }

    private record Resolved(Set<String> groups, TeamInfo team) {
    }

    private java.util.concurrent.CompletableFuture<TeamInfo> resolveTeam(@NotNull Player player) {
        TeamSource teamSource = this.teamSource;
        if (teamSource == null || !this.settings.isUseUltimateTeams() || !teamSource.isAvailable()) {
            return java.util.concurrent.CompletableFuture.completedFuture(TeamInfo.EMPTY);
        }
        return teamSource.resolveTeam(player)
                .thenApply(info -> info == null ? TeamInfo.EMPTY : info)
                .exceptionally(throwable -> TeamInfo.EMPTY);
    }

    private void apply(@NotNull Player player, @NotNull Set<String> groups, @NotNull TeamInfo team) {
        if (!player.isOnline()) return;

        SunUser user = this.userManager.getOrFetch(player);
        long now = System.currentTimeMillis();

        Profile profile = this.catalog.resolveProfile(player, user.getPropertyOrDefault(NametagsProperties.PROFILE));

        RankDefinition rank = this.resolveRank(player, user, profile, groups);
        TagDefinition tag = this.resolveTag(player, user, profile, now);

        Part teamPart = Part.of(team.prefix(), team.suffix(), team.color());
        Part rankPart = rank == null ? Part.EMPTY : Part.of(rank.getPrefix(), rank.getSuffix(), rank.getColor());
        Part tagPart = tag == null ? Part.EMPTY : Part.of(tag.getPrefix(), tag.getSuffix(), tag.getColor());

        String glow = this.resolveGlow(player, user, tag, now);

        Nameplate composed = PrefixComposer.compose(teamPart, rankPart, tagPart, glow,
                this.settings.isTagColorOverridesRank());
        composed = this.resolvePlaceholders(player, composed);

        this.lastComposed.put(player.getUniqueId(), composed);
        this.backend.apply(player, composed);
    }

    private @Nullable RankDefinition resolveRank(@NotNull Player player,
            @NotNull SunUser user,
            @Nullable Profile profile,
            @NotNull Set<String> groups
    ) {
        // A profile may force a rank, overriding both the stored pick and group resolution.
        if (profile != null && profile.hasRank()) {
            RankDefinition forced = this.catalog.getRank(profile.getRankId());
            if (forced != null) return forced;
        }

        String stored = user.getPropertyOrDefault(NametagsProperties.RANK);
        if (stored != null && !stored.isBlank()) {
            RankDefinition chosen = this.catalog.getRank(stored);
            if (chosen != null) return chosen;
        }

        return this.catalog.resolveRank(player, groups);
    }

    private @Nullable TagDefinition resolveTag(@NotNull Player player,
            @NotNull SunUser user,
            @Nullable Profile profile,
            long nowMillis
    ) {
        String id = null;
        if (profile != null && profile.hasTag()) {
            id = profile.getTagId();
        } else {
            id = user.getPropertyOrDefault(NametagsProperties.TAG);
        }
        if (id == null || id.isBlank()) return null;

        TagDefinition tag = this.catalog.getTag(id);
        if (tag == null) return null;
        return this.access.hasAccess(player, user, tag, nowMillis) ? tag : null;
    }

    private @Nullable String resolveGlow(@NotNull Player player,
            @NotNull SunUser user,
            @Nullable TagDefinition tag,
            long nowMillis
    ) {
        if (!this.settings.isResolveGlowColor() || this.glowSource == null) return null;

        String color = this.glowSource.getGlowColor(player);
        if (color == null || color.isBlank()) return null;

        // A tag that opted out of glow keeps its own colour instead.
        if (tag != null && !this.access.allowsGlow(user, tag, nowMillis)) return null;

        return color;
    }

    private @NotNull Nameplate resolvePlaceholders(@NotNull Player player, @NotNull Nameplate nameplate) {
        String prefix = this.applyPlaceholders(player, nameplate.prefix());
        String suffix = this.applyPlaceholders(player, nameplate.suffix());
        return new Nameplate(prefix, suffix, nameplate.color());
    }

    private @NotNull String applyPlaceholders(@NotNull Player player, @NotNull String text) {
        if (text.isEmpty()) return text;
        return PlaceholderContext.builder()
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .andThen(SLPlaceholders.forPlaceholderAPI(player))
                .build()
                .apply(text);
    }

    // -----------------------------------------------------
    // Placeholder source
    // -----------------------------------------------------

    /** Feeds TAB's registered placeholder, which runs on the TAB refresh thread. */
    public @NotNull String getPrefixFor(@NotNull UUID playerId) {
        Nameplate nameplate = this.lastComposed.get(playerId);
        return nameplate == null ? "" : nameplate.prefix();
    }

    public @NotNull String getSuffixFor(@NotNull UUID playerId) {
        Nameplate nameplate = this.lastComposed.get(playerId);
        return nameplate == null ? "" : nameplate.suffix();
    }

    public @NotNull Nameplate getLastComposed(@NotNull UUID playerId) {
        return this.lastComposed.getOrDefault(playerId, Nameplate.EMPTY);
    }

    /** Recomputes every online player, e.g. after a reload or a rank change. */
    public void recomputeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) this.recompute(player);
    }

    public void forget(@NotNull UUID playerId) {
        this.lastComposed.remove(playerId);
        this.backend.remove(playerId);
    }

    public void clear() {
        this.lastComposed.clear();
        this.backend.clearCache();
    }
}
