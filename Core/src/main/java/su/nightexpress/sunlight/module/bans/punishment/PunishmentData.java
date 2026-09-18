package su.nightexpress.sunlight.module.bans.punishment;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PunishmentData(UUID id,
    PunishmentType type,
    String reason,
    String who,
    long duration,
    long creationTimestamp,
    long expirationTimestamp) {

}
