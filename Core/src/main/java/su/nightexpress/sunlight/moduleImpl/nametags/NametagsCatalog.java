package su.nightexpress.sunlight.moduleImpl.nametags;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;
import su.nightexpress.sunlight.moduleImpl.nametags.model.RankDefinition;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read model over the configured ranks, tags and profiles.
 * <p>
 * Tag and rank lookups use a nested map keyed by group name, so resolving a player's rank
 * does not require scanning every definition. Mutations go through {@link #reload}, which
 * rebuilds the indices atomically.
 */
public class NametagsCatalog {

    private final NametagsSettings settings;

    private volatile Map<String, TagDefinition> tags = Map.of();
    private volatile Map<String, Profile> profiles = Map.of();
    private volatile List<RankDefinition> ranks = List.of();

    /** group name -> ranks declaring it, used to resolve a rank in O(matches). */
    private volatile Map<String, List<RankDefinition>> ranksByGroup = Map.of();

    public NametagsCatalog(@NotNull NametagsSettings settings) {
        this.settings = settings;
    }

    /** Immutable, order-preserving snapshot, so config writes keep the author's ordering. */
    private static <K, V> Map<K, V> freeze(@NotNull Map<K, V> source) {
        return java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(source));
    }

    /** Rebuilds every index from the loaded settings. */
    public void reload() {
        Map<String, TagDefinition> tags = new java.util.LinkedHashMap<>(this.settings.getTags());
        Map<String, Profile> profiles = new java.util.LinkedHashMap<>(this.settings.getProfiles());

        List<RankDefinition> ranks = new ArrayList<>(this.settings.getRanks().values());
        ranks.sort(Comparator.comparingInt(RankDefinition::getPriority).reversed());

        Map<String, List<RankDefinition>> byGroup = new java.util.LinkedHashMap<>();
        for (RankDefinition rank : ranks) {
            for (String group : rank.getRanks()) {
                byGroup.computeIfAbsent(group, key -> new ArrayList<>()).add(rank);
            }
        }

        this.tags = freeze(tags);
        this.profiles = freeze(profiles);
        this.ranks = List.copyOf(ranks);
        this.ranksByGroup = java.util.Collections.unmodifiableMap(byGroup);
    }

    /**
     * Adds or replaces a tag at runtime. Call {@link NametagsModule#saveCatalog()} to persist
     * the change, which the admin GUI does after every edit.
     */
    public void putTag(@NotNull TagDefinition tag) {
        Map<String, TagDefinition> updated = new java.util.LinkedHashMap<>(this.tags);
        updated.put(tag.getId(), tag);
        this.tags = freeze(updated);
    }

    public boolean removeTag(@NotNull String tagId) {
        Map<String, TagDefinition> updated = new java.util.LinkedHashMap<>(this.tags);
        if (updated.remove(tagId.toLowerCase(java.util.Locale.ROOT)) == null) return false;

        this.tags = freeze(updated);
        return true;
    }

    public void putProfile(@NotNull Profile profile) {
        Map<String, Profile> updated = new java.util.LinkedHashMap<>(this.profiles);
        updated.put(profile.getId(), profile);
        this.profiles = freeze(updated);
    }

    public boolean removeProfile(@NotNull String profileId) {
        Map<String, Profile> updated = new java.util.LinkedHashMap<>(this.profiles);
        if (updated.remove(profileId.toLowerCase(java.util.Locale.ROOT)) == null) return false;

        this.profiles = freeze(updated);
        return true;
    }

    // -----------------------------------------------------
    // Tags
    // -----------------------------------------------------

    public @NotNull Collection<TagDefinition> getTags() {
        return this.tags.values();
    }

    /** Live, order-preserving view keyed by tag id, for persisting admin edits. */
    public @NotNull Map<String, TagDefinition> getTagsById() {
        return this.tags;
    }

    public @NotNull Map<String, Profile> getProfilesById() {
        return this.profiles;
    }

    /** Tags in GUI order: highest sort priority first, then alphabetically by id. */
    public @NotNull List<TagDefinition> getTagsSorted() {
        List<TagDefinition> sorted = new ArrayList<>(this.tags.values());
        sorted.sort(Comparator.comparingInt(TagDefinition::getSortPriority).reversed()
                .thenComparing(TagDefinition::getId));
        return sorted;
    }

    public @Nullable TagDefinition getTag(@Nullable String id) {
        if (id == null || id.isBlank()) return null;
        return this.tags.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean hasTag(@Nullable String id) {
        return this.getTag(id) != null;
    }

    // -----------------------------------------------------
    // Profiles
    // -----------------------------------------------------

    public @NotNull Collection<Profile> getProfiles() {
        return this.profiles.values();
    }

    public @Nullable Profile getProfile(@Nullable String id) {
        if (id == null || id.isBlank()) return null;
        return this.profiles.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Picks the best profile a player qualifies for, either by explicit choice or by
     * world membership and permission. Returns {@code null} when nothing matches.
     */
    public @Nullable Profile resolveProfile(@NotNull Player player, @Nullable String chosenId) {
        Profile chosen = this.getProfile(chosenId);
        if (chosen != null && this.canUseProfile(player, chosen)) return chosen;

        Profile best = null;
        for (Profile profile : this.profiles.values()) {
            if (!this.canUseProfile(player, profile)) continue;
            if (best == null || profile.getPriority() > best.getPriority()) best = profile;
        }
        return best;
    }

    private boolean canUseProfile(@NotNull Player player, @NotNull Profile profile) {
        if (!profile.appliesTo(player.getWorld().getName())) return false;

        // A profile that declares no worlds is a global default, so everyone may use it.
        // A world-scoped profile still needs the explicit node unless the player is an admin.
        if (profile.getWorlds().isEmpty()) return true;
        return player.hasPermission("nametags.profile." + profile.getId()) || player.hasPermission("nametags.admin");
    }

    // -----------------------------------------------------
    // Ranks
    // -----------------------------------------------------

    public @NotNull Collection<RankDefinition> getRanks() {
        return this.ranks;
    }

    /** Live, id-keyed rank view, for persisting admin edits. */
    public @NotNull Map<String, RankDefinition> getRanksById() {
        Map<String, RankDefinition> byId = new java.util.LinkedHashMap<>();
        this.ranks.forEach(rank -> byId.put(rank.getId(), rank));
        return byId;
    }

    public @Nullable RankDefinition getRank(@Nullable String id) {
        if (id == null || id.isBlank()) return null;
        return this.ranks.stream()
                .filter(rank -> rank.getId().equals(id.toLowerCase(java.util.Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves the best rank for a player from their resolved group names.
     * Falls back to the default rank when the player matches nothing.
     */
    public @Nullable RankDefinition resolveRank(@NotNull Player player, @NotNull Set<String> groups) {
        RankDefinition best = null;
        for (String group : groups) {
            for (RankDefinition rank : this.ranksByGroup.getOrDefault(group, List.of())) {
                if (!rank.isAvailable(player)) continue;
                if (best == null || rank.getPriority() > best.getPriority()) best = rank;
            }
        }
        if (best != null) return best;
        return this.getDefaultRank();
    }

    /** Resolves a rank purely from group names, for offline players. */
    public @Nullable RankDefinition resolveRank(@NotNull Set<String> groups) {
        RankDefinition best = null;
        for (String group : groups) {
            for (RankDefinition rank : this.ranksByGroup.getOrDefault(group, List.of())) {
                if (best == null || rank.getPriority() > best.getPriority()) best = rank;
            }
        }
        return best != null ? best : this.getDefaultRank();
    }

    public @Nullable RankDefinition getDefaultRank() {
        return this.ranks.stream().filter(RankDefinition::isDefault).findFirst().orElse(null);
    }
}
