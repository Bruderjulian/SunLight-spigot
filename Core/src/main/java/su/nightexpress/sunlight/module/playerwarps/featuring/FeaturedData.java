package su.nightexpress.sunlight.module.playerwarps.featuring;

import su.nightexpress.nightcore.util.TimeUtil;

public record FeaturedData(String slotId, int slotIndex, long endTimestamp) {

    public boolean isActive() {
        return !TimeUtil.isPassed(this.endTimestamp);
    }
}
