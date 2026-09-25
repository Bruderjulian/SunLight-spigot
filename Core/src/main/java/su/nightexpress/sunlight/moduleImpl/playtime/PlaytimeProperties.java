package su.nightexpress.sunlight.moduleImpl.playtime;

import su.nightexpress.sunlight.user.property.UserProperty;

public class PlaytimeProperties {

    public static final UserProperty<Long> TOTAL = UserProperty.create("playtime_total", Long.class, 0L, true);
    public static final UserProperty<Long> YEAR = UserProperty.create("playtime_year", Long.class, 0L, true);
    public static final UserProperty<Long> MONTH = UserProperty.create("playtime_month", Long.class, 0L, true);
    public static final UserProperty<Long> WEEK = UserProperty.create("playtime_week", Long.class, 0L, true);
    public static final UserProperty<Long> DAY = UserProperty.create("playtime_day", Long.class, 0L, true);

    public static final UserProperty<Long> DAY_KEY = UserProperty.create("playtime_day_key", Long.class, 0L, true);
    public static final UserProperty<Long> WEEK_KEY = UserProperty.create("playtime_week_key", Long.class, 0L, true);
    public static final UserProperty<Long> MONTH_KEY = UserProperty.create("playtime_month_key", Long.class, 0L, true);
    public static final UserProperty<Long> YEAR_KEY = UserProperty.create("playtime_year_key", Long.class, 0L, true);

    public static final UserProperty<Long> LAST_SEEN = UserProperty.create("playtime_last_seen", Long.class, 0L, true);

    public static final UserProperty<Integer> DAILY_STREAK = UserProperty.create("playtime_daily_streak", Integer.class, 0, true);
    public static final UserProperty<Integer> WEEKLY_STREAK = UserProperty.create("playtime_weekly_streak", Integer.class, 0, true);

    public static final UserProperty<Long> GOAL_DAY = UserProperty.create("playtime_goal_day", Long.class, 0L, true);
    public static final UserProperty<Long> GOAL_WEEK = UserProperty.create("playtime_goal_week", Long.class, 0L, true);
    public static final UserProperty<Long> GOAL_MONTH = UserProperty.create("playtime_goal_month", Long.class, 0L, true);

    public static final UserProperty<Long> REWARDED_DAY_KEY = UserProperty.create("playtime_rewarded_day_key", Long.class, 0L, true);
    public static final UserProperty<Long> REWARDED_WEEK_KEY = UserProperty.create("playtime_rewarded_week_key", Long.class, 0L, true);
    public static final UserProperty<Long> REWARDED_MONTH_KEY = UserProperty.create("playtime_rewarded_month_key", Long.class, 0L, true);

    public static final UserProperty<Long> REMINDED_DAY_KEY = UserProperty.create("playtime_reminded_day_key", Long.class, 0L, true);
    public static final UserProperty<Long> REMINDED_WEEK_KEY = UserProperty.create("playtime_reminded_week_key", Long.class, 0L, true);
    public static final UserProperty<Long> REMINDED_MONTH_KEY = UserProperty.create("playtime_reminded_month_key", Long.class, 0L, true);
}