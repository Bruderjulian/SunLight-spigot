package su.nightexpress.sunlight.module.bans.punishment;

import su.nightexpress.sunlight.module.bans.time.BanTime;

public record PunishmentContext(PunishmentType type,
        PunishmentReason reason,
        BanTime time,
        boolean silent) {

}
