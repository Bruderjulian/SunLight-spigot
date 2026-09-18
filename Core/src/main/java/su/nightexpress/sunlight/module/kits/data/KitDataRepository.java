package su.nightexpress.sunlight.module.kits.data;

import su.nightexpress.nightcore.util.LowerCase;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class KitDataRepository {

    private final Map<UUID, Map<String, KitData>> dataMapById;

    public KitDataRepository() {
        this.dataMapById = new ConcurrentHashMap<>();
    }

    public synchronized void clear() {
        this.dataMapById.clear();
    }

    public synchronized void add(KitData data) {
        this.getUserDataMap(data.getPlayerId()).put(data.getKitId(), data);
    }

    public synchronized void remove(UUID playerId, String kitId) {
        this.getUserDataMap(playerId).remove(LowerCase.INTERNAL.apply(kitId));
    }

    public Map<String, KitData> getUserDataMap(UUID playerId) {
        return this.dataMapById.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
    }

    public KitData getKitData(UUID playerId, String kitId) {
        return this.getUserDataMap(playerId).get(LowerCase.INTERNAL.apply(kitId));
    }

    public Optional<KitData> kitData(UUID playerId, String kitId) {
        return Optional.ofNullable(this.getKitData(playerId, kitId));
    }

    public Set<KitData> getAll() {
        return this.dataMapById.values().stream().flatMap(dataMap -> dataMap.values().stream())
                .collect(Collectors.toSet());
    }
}
