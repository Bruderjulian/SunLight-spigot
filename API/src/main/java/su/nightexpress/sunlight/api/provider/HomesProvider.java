package su.nightexpress.sunlight.api.provider;

import java.util.Set;

public interface HomesProvider {

    Set<String> getHomeIds(java.util.UUID playerId);

    default int countHomes(java.util.UUID playerId) {
        return this.getHomeIds(playerId).size();
    }
}
