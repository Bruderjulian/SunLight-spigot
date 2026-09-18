package su.nightexpress.sunlight.moduleImpl.bans.punishment;

import su.nightexpress.sunlight.moduleImpl.bans.time.BanTime;

public record PunishmentContext(PunishmentType type,
        PunishmentReason reason,
        BanTime time,
        boolean silent) {

}
