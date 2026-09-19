package su.nightexpress.sunlight.moduleImpl.playerwarps.featuring;

import su.nightexpress.sunlight.utils.TimeUtil;

public record FeaturedData(String slotId, int slotIndex, long endTimestamp) {

    public boolean isActive() {
        return !TimeUtil.isPassed(this.endTimestamp);
    }
}
