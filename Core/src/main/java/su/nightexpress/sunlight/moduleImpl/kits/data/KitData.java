package su.nightexpress.sunlight.moduleImpl.kits.data;

import su.nightexpress.nightcore.util.TimeUtil;

import java.util.UUID;

public class KitData {

    private final UUID playerId;
    private final String kitId;

    private long cooldownDate;

    private boolean dirty;

    public KitData(UUID playerId, String kitId, long cooldownDate) {
        this.playerId = playerId;
        this.kitId = kitId;
        this.cooldownDate = cooldownDate;
    }

    public static KitData create(UUID playerId, String kitId) {
        return new KitData(playerId, kitId, 0L);
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }

    public boolean hasCooldown() {
        return !this.isCooldownExpired();
    }

    public boolean isCooldownExpirable() {
        return this.cooldownDate >= 0L;
    }

    public boolean isCooldownExpired() {
        return TimeUtil.isPassed(this.cooldownDate);
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public String getKitId() {
        return this.kitId;
    }

    public long getCooldownDate() {
        return this.cooldownDate;
    }

    public void setCooldownDate(long cooldownDate) {
        this.cooldownDate = cooldownDate;
    }
}
