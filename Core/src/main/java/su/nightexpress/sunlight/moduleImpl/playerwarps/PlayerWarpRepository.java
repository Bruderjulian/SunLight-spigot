package su.nightexpress.sunlight.moduleImpl.playerwarps;

import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.sunlight.moduleImpl.playerwarps.category.WarpCategory;
import su.nightexpress.sunlight.moduleImpl.playerwarps.featuring.FeaturedSlot;
import su.nightexpress.sunlight.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerWarpRepository {

    private final Map<String, PlayerWarp> byIdMap;
    private final Map<UUID, Set<PlayerWarp>> byOwnerMap;

    private final Set<PlayerWarp> featuredWarps;
    private final List<PlayerWarp> popularWarps;

    public PlayerWarpRepository() {
        this.byIdMap = new HashMap<>();
        this.byOwnerMap = new HashMap<>();

        this.featuredWarps = new HashSet<>();
        this.popularWarps = new ArrayList<>();
    }

    public void add(PlayerWarp warp) {
        this.byIdMap.put(warp.getId(), warp);
        this.byOwnerMap.computeIfAbsent(warp.getOwnerId(), k -> new HashSet<>()).add(warp);
    }

    public void remove(PlayerWarp warp) {
        this.byIdMap.remove(warp.getId());
        this.byOwnerMap.getOrDefault(warp.getOwnerId(), Collections.emptySet()).remove(warp);

        this.featuredWarps.remove(warp);
        this.popularWarps.remove(warp);
    }

    public void clear() {
        this.byIdMap.clear();
        this.byOwnerMap.clear();
    }

    public int size() {
        return this.byIdMap.size();
    }

    public boolean hasWarp(String id) {
        return this.byIdMap.containsKey(Utils.lowercase(id));
    }

    public boolean isFeatured(FeaturedSlot slot, int slotIndex) {
        return this.getFeaturedWarps().stream().anyMatch(warp -> warp.isFeatured(slot, slotIndex));
    }

    public void updateFeaturedWarps() {
        this.featuredWarps.clear();

        this.stream().filter(PlayerWarp::isFeatured).forEach(this.featuredWarps::add);
    }

    public void updatePopularWarps(int limit) {
        this.popularWarps.clear();

        this.stream().sorted(Comparator.comparingLong(PlayerWarp::getTotalVisits).reversed()).limit(limit)
                .forEach(this.popularWarps::add);
    }

    public int countWarps(WarpCategory category) {
        return this.getByCategory(category).size();
    }

    public int countOwnedWarps(UUID playerId) {
        return this.getByOwner(playerId).size();
    }

    public Stream<PlayerWarp> stream() {
        return this.byIdMap.values().stream();
    }

    public Map<String, PlayerWarp> getByIdMap() {
        return this.byIdMap;
    }

    public Map<UUID, Set<PlayerWarp>> getByOwnerMap() {
        return this.byOwnerMap;
    }

    public PlayerWarp getById(String id) {
        return this.byIdMap.get(Utils.lowercase(id));
    }

    public Set<PlayerWarp> getByOwner(UUID playerId) {
        return Set.copyOf(this.byOwnerMap.getOrDefault(playerId, Collections.emptySet()));
    }

    public Set<PlayerWarp> getAll() {
        return Set.copyOf(this.byIdMap.values());
    }

    public Set<PlayerWarp> getByCategory(WarpCategory category) {
        return this.stream().filter(category::isWarpOfThis).collect(Collectors.toSet());
    }

    public Set<PlayerWarp> getFeaturedWarps() {
        return this.featuredWarps;
    }

    public List<PlayerWarp> getPopularWarps() {
        return this.popularWarps;
    }
}
