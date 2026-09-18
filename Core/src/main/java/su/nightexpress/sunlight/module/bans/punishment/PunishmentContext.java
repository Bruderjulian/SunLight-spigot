package su.nightexpress.sunlight.module.bans.punishment;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.sunlight.module.bans.time.BanTime;

public record PunishmentContext(PunishmentType type,
    PunishmentReason reason,
    BanTime time,
    boolean silent) {

}
