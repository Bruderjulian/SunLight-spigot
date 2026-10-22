package su.nightexpress.sunlight.api.provider;

import java.util.UUID;

public interface BansProvider {

    boolean isBanned(UUID playerId);

    boolean isMuted(UUID playerId);
}
