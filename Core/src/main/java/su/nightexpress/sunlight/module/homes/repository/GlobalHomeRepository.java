package su.nightexpress.sunlight.module.homes.repository;

import org.bukkit.entity.Player;

import su.nightexpress.sunlight.module.homes.impl.Home;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GlobalHomeRepository {

    private final Map<UUID, UserHomeRepository> homesByOwnerMap;

    public GlobalHomeRepository() {
        this.homesByOwnerMap = new ConcurrentHashMap<>();
    }

    public void clear() {
        this.homesByOwnerMap.values().forEach(UserHomeRepository::clear);
        this.homesByOwnerMap.clear();
    }

    public UserHomeRepository getUserRepository(UUID playerId) {
        return this.homesByOwnerMap.computeIfAbsent(playerId, k -> new UserHomeRepository());
    }

    public synchronized void add(Home home) {
        UUID ownerId = home.getOwner().id();
        this.getUserRepository(ownerId).add(home);

    }

    public synchronized void remove(Home home) {
        UUID ownerId = home.getOwner().id();
        this.getUserRepository(ownerId).remove(home);
    }

    public Set<Home> getAll() {
        return this.getAll(home -> true);
    }

    public Set<Home> getAll(Predicate<Home> predicate) {
        return this.homesByOwnerMap.values().stream().flatMap(repository -> repository.getAll().stream())
                .collect(Collectors.toSet());
    }

    @Deprecated
    public Set<Home> getAvailableForVisit(Player player) {
        return this.getAll(home -> home.canVisit(player));
    }
}
