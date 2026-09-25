package su.nightexpress.sunlight.moduleImpl.playtime;

import java.time.ZoneId;
import java.util.List;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;

public class PlaytimeSettings extends AbstractConfig {

    private final ConfigProperty<Boolean> excludeAfk = this.addProperty(ConfigTypes.BOOLEAN,
            "Playtime.Tracking.Exclude-Afk",
            true,
            "Sets whether time spent AFK (AFK module) does not count as playtime.");

    private final ConfigProperty<Long> tickInterval = this.addProperty(ConfigTypes.LONG,
            "Playtime.Tracking.Tick-Interval",
            20L,
            "Sets how often (in game ticks) playtime is accumulated. [1 second = 20 ticks]");

    private final ConfigProperty<String> timezone = this.addProperty(ConfigTypes.STRING,
            "Playtime.Tracking.Timezone",
            "system",
            "Sets the timezone used for day/week/month rollovers.",
            "Use 'system' to follow the server timezone, or any IANA id, e.g. 'Europe/Berlin', 'America/New_York'.");

    private final ConfigProperty<Long> topRefreshInterval = this.addProperty(ConfigTypes.LONG,
            "Playtime.Top.Refresh-Interval",
            600L,
            "Sets how often (in game ticks) the /playtop leaderboard cache is rebuilt. [1 second = 20 ticks]");

    private final ConfigProperty<Integer> goalMinMinutes = this.addProperty(ConfigTypes.INT,
            "Playtime.Goals.Override.Min-Minutes",
            1,
            "Sets the minimal (and negative value = disable) minute amount for in-game goal changes.");

    private final ConfigProperty<Integer> goalMaxMinutes = this.addProperty(ConfigTypes.INT,
            "Playtime.Goals.Override.Max-Minutes",
            24 * 60,
            "Sets the maximal minute amount for in-game goal changes.");

    private final ConfigProperty<Long> dailyGoal = this.addProperty(ConfigTypes.LONG,
            "Playtime.Goals.Daily.Minutes",
            60L,
            "Sets the daily playtime goal in minutes (0 = disabled).",
            "Players who reach it get the reward below. Can be overridden in-game with /playtime goal.");

    private final ConfigProperty<Long> weeklyGoal = this.addProperty(ConfigTypes.LONG,
            "Playtime.Goals.Weekly.Minutes",
            8 * 60L,
            "Sets the weekly playtime goal in minutes (0 = disabled).");

    private final ConfigProperty<Long> monthlyGoal = this.addProperty(ConfigTypes.LONG,
            "Playtime.Goals.Monthly.Minutes",
            40 * 60L,
            "Sets the monthly playtime goal in minutes (0 = disabled).");

    private final ConfigProperty<List<String>> dailyRewards = this.addProperty(ConfigTypes.STRING_LIST,
            "Playtime.Rewards.Day",
            List.of(
                    "eco give %player_name% 1000",
                    "give %player_name% diamond 8"),
            "Console commands executed once when the daily goal is reached.",
            "Placeholders: %player_name%, %player_display_name% and PlaceholderAPI.");

    private final ConfigProperty<List<String>> weeklyRewards = this.addProperty(ConfigTypes.STRING_LIST,
            "Playtime.Rewards.Week",
            List.of(
                    "eco give %player_name% 5000",
                    "give %player_name% netherite_ingot 4"),
            "Console commands executed once when the weekly goal is reached.");

    private final ConfigProperty<List<String>> monthlyRewards = this.addProperty(ConfigTypes.STRING_LIST,
            "Playtime.Rewards.Month",
            List.of(
                    "eco give %player_name% 20000",
                    "give %player_name% netherite_block 2"),
            "Console commands executed once when the monthly goal is reached.");

    private final ConfigProperty<Boolean> remindersEnabled = this.addProperty(ConfigTypes.BOOLEAN,
            "Playtime.Reminders.Enabled",
            true,
            "Sets whether goal reminders are sent.");

    private final ConfigProperty<Long> dayReminderWindow = this.addProperty(ConfigTypes.LONG,
            "Playtime.Reminders.Daily.Window-Minutes",
            15L,
            "When the remaining time to the daily goal is within this window, a reminder is sent (once per day).");

    private final ConfigProperty<Long> weekReminderWindow = this.addProperty(ConfigTypes.LONG,
            "Playtime.Reminders.Weekly.Window-Minutes",
            60L,
            "Reminder window for the weekly goal, in minutes.");

    private final ConfigProperty<Long> monthReminderWindow = this.addProperty(ConfigTypes.LONG,
            "Playtime.Reminders.Monthly.Window-Minutes",
            240L,
            "Reminder window for the monthly goal, in minutes.");

    public void load(FileConfig config) {
        super.load(config);
    }

    public boolean isExcludeAfkEnabled() {
        return this.excludeAfk.get();
    }

    public long getTickInterval() {
        return Math.max(1L, this.tickInterval.get());
    }

    public ZoneId getZoneId() {
        String value = this.timezone.get();
        if (value == null || value.isBlank() || value.equalsIgnoreCase("system"))
            return ZoneId.systemDefault();
        try {
            return ZoneId.of(value);
        } catch (Exception exception) {
            return ZoneId.systemDefault();
        }
    }

    public long getTopRefreshInterval() {
        return Math.max(20L, this.topRefreshInterval.get());
    }

    public long getDailyGoalMs() {
        return this.minutesToMs(this.dailyGoal.get());
    }

    public long getWeeklyGoalMs() {
        return this.minutesToMs(this.weeklyGoal.get());
    }

    public long getMonthlyGoalMs() {
        return this.minutesToMs(this.monthlyGoal.get());
    }

    public List<String> getDailyRewards() {
        return this.dailyRewards.get();
    }

    public List<String> getWeeklyRewards() {
        return this.weeklyRewards.get();
    }

    public List<String> getMonthlyRewards() {
        return this.monthlyRewards.get();
    }

    public boolean isRemindersEnabled() {
        return this.remindersEnabled.get();
    }

    public long getDayReminderWindowMs() {
        return this.minutesToMs(this.dayReminderWindow.get());
    }

    public long getWeekReminderWindowMs() {
        return this.minutesToMs(this.weekReminderWindow.get());
    }

    public long getMonthReminderWindowMs() {
        return this.minutesToMs(this.monthReminderWindow.get());
    }

    public int getGoalMinMinutes() {
        return this.goalMinMinutes.get();
    }

    public int getGoalMaxMinutes() {
        return this.goalMaxMinutes.get();
    }

    private long minutesToMs(long minutes) {
        return Math.max(0L, minutes) * 60_000L;
    }
}