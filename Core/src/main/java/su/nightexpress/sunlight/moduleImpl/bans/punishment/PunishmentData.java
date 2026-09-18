package su.nightexpress.sunlight.moduleImpl.bans.punishment;

import java.util.UUID;

public record PunishmentData(UUID id,
        PunishmentType type,
        String reason,
        String who,
        long duration,
        long creationTimestamp,
        long expirationTimestamp) {

}
