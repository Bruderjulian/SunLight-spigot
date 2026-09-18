package su.nightexpress.sunlight.module.homes.repository;

import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.sunlight.module.homes.impl.Home;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UserHomeRepository {

    private final Map<String, Home> homByIdMap;

    public UserHomeRepository() {
        this.homByIdMap = new ConcurrentHashMap<>();
    }

    void clear() {
        this.homByIdMap.clear();
    }

    synchronized void add(Home home) {
        this.homByIdMap.put(home.getId(), home);
    }

    synchronized void remove(Home home) {
        this.remove(home.getId());
    }

    synchronized void remove(String id) {
        this.homByIdMap.remove(LowerCase.INTERNAL.apply(id));
    }

    public Set<Home> getAll() {
        return Set.copyOf(this.homByIdMap.values());
    }

    public Set<Home> getAll(Predicate<Home> predicate) {
        return this.homByIdMap.values().stream().filter(predicate).collect(Collectors.toSet());
    }

    public Home getById(String id) {
        return this.homByIdMap.get(LowerCase.INTERNAL.apply(id));
    }
}
