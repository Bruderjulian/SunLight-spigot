package su.nightexpress.sunlight.moduleImpl.playtime.model;

import su.nightexpress.sunlight.utils.Utils;

/**
 * Time buckets used by the playtime module.
 */
public enum PlaytimePeriod {

    ALLTIME, YEAR, MONTH, WEEK, DAY;

    public static PlaytimePeriod fromString(String raw) {
        return Utils.enumOptionalValueOf(raw, PlaytimePeriod.class).orElse(ALLTIME);
    }

    public String getConfigName() {
        return this.name().toLowerCase();
    }
}