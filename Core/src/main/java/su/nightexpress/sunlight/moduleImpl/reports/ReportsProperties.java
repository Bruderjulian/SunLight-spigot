package su.nightexpress.sunlight.moduleImpl.reports;

import su.nightexpress.sunlight.user.property.UserProperty;

/**
 * Per-player counters for the reports module.
 * <p>
 * Report state itself is relational and lives in tables. Only things that cannot be derived from
 * those tables belong here: reward accounting, because a report is created at one moment and
 * rewarded at another so "rewarded today" has no single timestamp on the row.
 */
public class ReportsProperties {

    /** {@code yyyy-MM-dd} of the day the reward counters below refer to. */
    public static final UserProperty<String> REWARD_DAY_KEY = UserProperty.create("reportsRewardDayKey", String.class,
            "", true);

    public static final UserProperty<Integer> REWARD_DAY_COUNT = UserProperty.create("reportsRewardDayCount",
            Integer.class, 0, true);

    /** Epoch millis of the last payout, for the reward cooldown. */
    public static final UserProperty<Long> REWARD_LAST_PAID = UserProperty.create("reportsRewardLastPaid", Long.class,
            0L, true);

    /** Set by a player to stop being able to file reports at all. */
    public static final UserProperty<Boolean> OPT_OUT = UserProperty.create("reportsOptOut", Boolean.class, false,
            true);

    private ReportsProperties() {
    }
}
