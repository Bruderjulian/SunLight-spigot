package su.nightexpress.sunlight.moduleImpl.playtime;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.playtime.command.PlaytimeCommandProvider;
import su.nightexpress.sunlight.moduleImpl.playtime.config.PlaytimeLang;
import su.nightexpress.sunlight.moduleImpl.playtime.listener.PlaytimeListener;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.Utils;

public class PlaytimeModule extends Module {

    public static final int TOP_PAGE_SIZE = 10;
    public static final int TOP_CACHE_LIMIT = 100;

    private final PlaytimeSettings settings;
    private final Map<PlaytimePeriod, List<TopEntry>> topCache;

    private volatile long topCacheTimestamp;

    public PlaytimeModule(ModuleDefinition<PlaytimeModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new PlaytimeSettings();
        this.topCache = new ConcurrentHashMap<>();
        this.topCacheTimestamp = 0L;
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.settings.load(config);
        this.plugin.injectLang(PlaytimeLang.class);

        // Must happen before any SunUser is deserialised.
        UserPropertyRegistry.register(PlaytimeProperties.TOTAL);
        UserPropertyRegistry.register(PlaytimeProperties.YEAR);
        UserPropertyRegistry.register(PlaytimeProperties.MONTH);
        UserPropertyRegistry.register(PlaytimeProperties.WEEK);
        UserPropertyRegistry.register(PlaytimeProperties.DAY);
        UserPropertyRegistry.register(PlaytimeProperties.DAY_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.WEEK_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.MONTH_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.YEAR_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.LAST_SEEN);
        UserPropertyRegistry.register(PlaytimeProperties.DAILY_STREAK);
        UserPropertyRegistry.register(PlaytimeProperties.WEEKLY_STREAK);
        UserPropertyRegistry.register(PlaytimeProperties.GOAL_DAY);
        UserPropertyRegistry.register(PlaytimeProperties.GOAL_WEEK);
        UserPropertyRegistry.register(PlaytimeProperties.GOAL_MONTH);
        UserPropertyRegistry.register(PlaytimeProperties.REWARDED_DAY_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.REWARDED_WEEK_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.REWARDED_MONTH_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.REMINDED_DAY_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.REMINDED_WEEK_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.REMINDED_MONTH_KEY);

        this.addListener(new PlaytimeListener(this.plugin, this));
        this.addTask(this::tick, this.settings.getTickInterval());
        this.addAsyncTask(this::refreshTopCache, (int) Math.max(1L, this.settings.getTopRefreshInterval() / 20L));

        this.commandRegistry.addProvider(new PlaytimeCommandProvider(this));

        Utils.onlinePlayers().forEach(player -> this.ensureRollover(this.userManager.getOrFetch(player)));
        this.refreshTopCache();
    }

    @Override
    protected void unloadModule() {
        this.topCache.clear();
        this.topCacheTimestamp = 0L;
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("playtime_total", (player, payload) -> String.valueOf(this.getEffectivePlaytime(player, PlaytimePeriod.ALLTIME)));
        registry.register("playtime_total_formatted", (player, payload) -> this.format(this.getEffectivePlaytime(player, PlaytimePeriod.ALLTIME)));
        registry.register("playtime_year", (player, payload) -> String.valueOf(this.getEffectivePlaytime(player, PlaytimePeriod.YEAR)));
        registry.register("playtime_year_formatted", (player, payload) -> this.format(this.getEffectivePlaytime(player, PlaytimePeriod.YEAR)));
        registry.register("playtime_month", (player, payload) -> String.valueOf(this.getEffectivePlaytime(player, PlaytimePeriod.MONTH)));
        registry.register("playtime_month_formatted", (player, payload) -> this.format(this.getEffectivePlaytime(player, PlaytimePeriod.MONTH)));
        registry.register("playtime_week", (player, payload) -> String.valueOf(this.getEffectivePlaytime(player, PlaytimePeriod.WEEK)));
        registry.register("playtime_week_formatted", (player, payload) -> this.format(this.getEffectivePlaytime(player, PlaytimePeriod.WEEK)));
        registry.register("playtime_day", (player, payload) -> String.valueOf(this.getEffectivePlaytime(player, PlaytimePeriod.DAY)));
        registry.register("playtime_day_formatted", (player, payload) -> this.format(this.getEffectivePlaytime(player, PlaytimePeriod.DAY)));
        registry.register("playtime_streak_daily", (player, payload) -> String.valueOf(this.getStreak(player, true)));
        registry.register("playtime_streak_weekly", (player, payload) -> String.valueOf(this.getStreak(player, false)));
        registry.register("playtime_goal_day", (player, payload) -> String.valueOf(this.getEffectiveGoalMs(player, true, false, false)));
        registry.register("playtime_goal_week", (player, payload) -> String.valueOf(this.getEffectiveGoalMs(player, false, true, false)));
        registry.register("playtime_goal_month", (player, payload) -> String.valueOf(this.getEffectiveGoalMs(player, false, false, true)));
    }

    public PlaytimeSettings getSettings() {
        return this.settings;
    }

    public String format(long ms) {
        if (ms <= 0L) return TimeFormats.formatAmount(0L, TimeFormatType.LITERAL);
        return TimeFormats.formatAmount(ms, TimeFormatType.LITERAL);
    }

    // --- Tick ---

    private void tick() {
        long deltaMs = this.settings.getTickInterval() * 50L;
        if (deltaMs <= 0L) return;

        boolean excludeAfk = this.settings.isExcludeAfkEnabled();
        var afkProvider = this.plugin.afkProvider();

        for (Player player : Bukkit.getOnlinePlayers()) {
            SunUser user = this.userManager.getOrFetch(player);
            this.ensureRollover(user);
            user.setProperty(PlaytimeProperties.LAST_SEEN, System.currentTimeMillis());

            if (excludeAfk && afkProvider.isPresent() && afkProvider.get().isAfk(player)) {
                continue;
            }

            user.setProperty(PlaytimeProperties.TOTAL, user.getPropertyOrDefault(PlaytimeProperties.TOTAL) + deltaMs);
            user.setProperty(PlaytimeProperties.YEAR, user.getPropertyOrDefault(PlaytimeProperties.YEAR) + deltaMs);
            user.setProperty(PlaytimeProperties.MONTH, user.getPropertyOrDefault(PlaytimeProperties.MONTH) + deltaMs);
            user.setProperty(PlaytimeProperties.WEEK, user.getPropertyOrDefault(PlaytimeProperties.WEEK) + deltaMs);
            user.setProperty(PlaytimeProperties.DAY, user.getPropertyOrDefault(PlaytimeProperties.DAY) + deltaMs);

            this.checkGoals(player, user);
        }
    }

    // --- Rollover & streaks ---

    public record PeriodKeys(long day, long week, long month, long year) {
    }

    public PeriodKeys currentKeys() {
        ZoneId zone;
        try {
            zone = this.settings.getZoneId();
        } catch (Exception exception) {
            zone = ZoneId.systemDefault();
        }
        ZonedDateTime now = ZonedDateTime.now(zone);
        long dayKey = now.toLocalDate().toEpochDay();
        long weekKey = now.toLocalDate().with(DayOfWeek.MONDAY).toEpochDay();
        long monthKey = (long) now.getYear() * 12L + (now.getMonthValue() - 1);
        long yearKey = now.getYear();
        return new PeriodKeys(dayKey, weekKey, monthKey, yearKey);
    }

    public void ensureRollover(@NotNull SunUser user) {
        PeriodKeys keys = this.currentKeys();

        long storedDay = user.getPropertyOrDefault(PlaytimeProperties.DAY_KEY);
        if (storedDay != keys.day()) {
            if (storedDay == 0L) {
                user.setProperty(PlaytimeProperties.DAILY_STREAK, Math.max(1, user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK)));
            } else if (storedDay == keys.day() - 1L) {
                user.setProperty(PlaytimeProperties.DAILY_STREAK, user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK) + 1);
            } else {
                user.setProperty(PlaytimeProperties.DAILY_STREAK, 1);
            }
            user.setProperty(PlaytimeProperties.DAY, 0L);
            user.setProperty(PlaytimeProperties.DAY_KEY, keys.day());
        }

        long storedWeek = user.getPropertyOrDefault(PlaytimeProperties.WEEK_KEY);
        if (storedWeek != keys.week()) {
            if (storedWeek == 0L) {
                user.setProperty(PlaytimeProperties.WEEKLY_STREAK, Math.max(1, user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK)));
            } else if (storedWeek == keys.week() - 7L) {
                user.setProperty(PlaytimeProperties.WEEKLY_STREAK, user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK) + 1);
            } else {
                user.setProperty(PlaytimeProperties.WEEKLY_STREAK, 1);
            }
            user.setProperty(PlaytimeProperties.WEEK, 0L);
            user.setProperty(PlaytimeProperties.WEEK_KEY, keys.week());
        }

        if (user.getPropertyOrDefault(PlaytimeProperties.MONTH_KEY) != keys.month()) {
            user.setProperty(PlaytimeProperties.MONTH, 0L);
            user.setProperty(PlaytimeProperties.MONTH_KEY, keys.month());
        }

        if (user.getPropertyOrDefault(PlaytimeProperties.YEAR_KEY) != keys.year()) {
            user.setProperty(PlaytimeProperties.YEAR, 0L);
            user.setProperty(PlaytimeProperties.YEAR_KEY, keys.year());
        }
    }

    // --- Reads ---

    public long getStoredPlaytime(@NotNull SunUser user, @NotNull PlaytimePeriod period) {
        return switch (period) {
            case ALLTIME -> user.getPropertyOrDefault(PlaytimeProperties.TOTAL);
            case YEAR -> user.getPropertyOrDefault(PlaytimeProperties.YEAR);
            case MONTH -> user.getPropertyOrDefault(PlaytimeProperties.MONTH);
            case WEEK -> user.getPropertyOrDefault(PlaytimeProperties.WEEK);
            case DAY -> user.getPropertyOrDefault(PlaytimeProperties.DAY);
        };
    }

    public long getEffectivePlaytime(@NotNull SunUser user, @NotNull PlaytimePeriod period) {
        if (period == PlaytimePeriod.ALLTIME) return Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
        PeriodKeys keys = this.currentKeys();
        return switch (period) {
            case YEAR -> user.getPropertyOrDefault(PlaytimeProperties.YEAR_KEY) == keys.year() ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.YEAR)) : 0L;
            case MONTH -> user.getPropertyOrDefault(PlaytimeProperties.MONTH_KEY) == keys.month() ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.MONTH)) : 0L;
            case WEEK -> user.getPropertyOrDefault(PlaytimeProperties.WEEK_KEY) == keys.week() ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.WEEK)) : 0L;
            case DAY -> user.getPropertyOrDefault(PlaytimeProperties.DAY_KEY) == keys.day() ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.DAY)) : 0L;
            default -> Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
        };
    }

    private long getEffectivePlaytime(Player player, @NotNull PlaytimePeriod period) {
        if (player == null) return 0L;
        SunUser user = this.userManager.getOrFetch(player);
        if (user == null) return 0L;
        return this.getEffectivePlaytime(user, period);
    }

    private int getStreak(Player player, boolean daily) {
        if (player == null) return 0;
        SunUser user = this.userManager.getOrFetch(player);
        if (user == null) return 0;
        return daily ? Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK))
            : Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK));
    }

    public long getTotalPlaytimeMs(@NotNull UUID playerId) {
        return this.userManager.getOrFetch(playerId)
            .map(user -> Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL)))
            .orElse(0L);
    }

    public long getTotalPlaytimeMs(@NotNull Player player) {
        return Math.max(0L, this.userManager.getOrFetch(player).getPropertyOrDefault(PlaytimeProperties.TOTAL));
    }

    // --- Goals ---

    public long getEffectiveGoalMs(@NotNull SunUser user, boolean day, boolean week, boolean month) {
        if (day) {
            long override = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.GOAL_DAY));
            return override > 0L ? override : Math.max(0L, this.settings.getDailyGoalMs());
        }
        if (week) {
            long override = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.GOAL_WEEK));
            return override > 0L ? override : Math.max(0L, this.settings.getWeeklyGoalMs());
        }
        long override = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.GOAL_MONTH));
        return override > 0L ? override : Math.max(0L, this.settings.getMonthlyGoalMs());
    }

    public long getEffectiveGoalMs(Player player, boolean day, boolean week, boolean month) {
        if (player == null) return 0L;
        return this.getEffectiveGoalMs(this.userManager.getOrFetch(player), day, week, month);
    }

    private void checkGoals(@NotNull Player player, @NotNull SunUser user) {
        long day = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.DAY));
        long week = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.WEEK));
        long month = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.MONTH));

        long dayKey = user.getPropertyOrDefault(PlaytimeProperties.DAY_KEY);
        long weekKey = user.getPropertyOrDefault(PlaytimeProperties.WEEK_KEY);
        long monthKey = user.getPropertyOrDefault(PlaytimeProperties.MONTH_KEY);

        long dayGoal = this.getEffectiveGoalMs(user, true, false, false);
        long weekGoal = this.getEffectiveGoalMs(user, false, true, false);
        long monthGoal = this.getEffectiveGoalMs(user, false, false, true);

        if (dayGoal > 0L) {
            if (day >= dayGoal) {
                if (user.getPropertyOrDefault(PlaytimeProperties.REWARDED_DAY_KEY) != dayKey) {
                    user.setProperty(PlaytimeProperties.REWARDED_DAY_KEY, dayKey);
                    this.giveGoalReward(player, this.settings.getDailyRewards(), "Daily");
                }
            } else if (this.settings.isRemindersEnabled()) {
                long remaining = dayGoal - day;
                if (remaining > 0L && remaining <= this.settings.getDayReminderWindowMs()
                    && user.getPropertyOrDefault(PlaytimeProperties.REMINDED_DAY_KEY) != dayKey) {
                    user.setProperty(PlaytimeProperties.REMINDED_DAY_KEY, dayKey);
                    this.sendPrefixed(PlaytimeLang.REMINDER_GOAL, player, b -> b
                        .with(SLPlaceholders.GENERIC_NAME, () -> "Daily")
                        .with(SLPlaceholders.GENERIC_TIME, () -> this.format(remaining)));
                }
            }
        }

        if (weekGoal > 0L) {
            if (week >= weekGoal) {
                if (user.getPropertyOrDefault(PlaytimeProperties.REWARDED_WEEK_KEY) != weekKey) {
                    user.setProperty(PlaytimeProperties.REWARDED_WEEK_KEY, weekKey);
                    this.giveGoalReward(player, this.settings.getWeeklyRewards(), "Weekly");
                }
            } else if (this.settings.isRemindersEnabled()) {
                long remaining = weekGoal - week;
                if (remaining > 0L && remaining <= this.settings.getWeekReminderWindowMs()
                    && user.getPropertyOrDefault(PlaytimeProperties.REMINDED_WEEK_KEY) != weekKey) {
                    user.setProperty(PlaytimeProperties.REMINDED_WEEK_KEY, weekKey);
                    this.sendPrefixed(PlaytimeLang.REMINDER_GOAL, player, b -> b
                        .with(SLPlaceholders.GENERIC_NAME, () -> "Weekly")
                        .with(SLPlaceholders.GENERIC_TIME, () -> this.format(remaining)));
                }
            }
        }

        if (monthGoal > 0L) {
            if (month >= monthGoal) {
                if (user.getPropertyOrDefault(PlaytimeProperties.REWARDED_MONTH_KEY) != monthKey) {
                    user.setProperty(PlaytimeProperties.REWARDED_MONTH_KEY, monthKey);
                    this.giveGoalReward(player, this.settings.getMonthlyRewards(), "Monthly");
                }
            } else if (this.settings.isRemindersEnabled()) {
                long remaining = monthGoal - month;
                if (remaining > 0L && remaining <= this.settings.getMonthReminderWindowMs()
                    && user.getPropertyOrDefault(PlaytimeProperties.REMINDED_MONTH_KEY) != monthKey) {
                    user.setProperty(PlaytimeProperties.REMINDED_MONTH_KEY, monthKey);
                    this.sendPrefixed(PlaytimeLang.REMINDER_GOAL, player, b -> b
                        .with(SLPlaceholders.GENERIC_NAME, () -> "Monthly")
                        .with(SLPlaceholders.GENERIC_TIME, () -> this.format(remaining)));
                }
            }
        }
    }

    private void giveGoalReward(@NotNull Player player, @NotNull List<String> commands, @NotNull String goalName) {
        this.sendPrefixed(PlaytimeLang.REWARD_GOAL, player, b -> b
            .with(SLPlaceholders.GENERIC_NAME, () -> goalName));

        if (commands.isEmpty()) return;

        PlaceholderContext context = PlaceholderContext.builder()
            .with(CommonPlaceholders.PLAYER.resolver(player))
            .andThen(SLPlaceholders.forPlayerWithPAPI(player))
            .build();

        List<String> resolved = new ArrayList<>(commands);
        resolved.replaceAll(command -> context.apply(command
            .replace("%player_name%", player.getName())
            .replace("%player_display_name%", player.getDisplayName())));

        CommandSender console = this.plugin.getServer().getConsoleSender();
        this.plugin.runTask(() -> resolved.forEach(command -> this.plugin.getServer().dispatchCommand(console, command)));
    }

    // --- Display ---

    public void showStats(@NotNull CommandSender viewer, @NotNull SunUser user) {
        this.ensureRollover(user);

        this.sendPrefixed(PlaytimeLang.STATS_HEADER, viewer, b -> b
            .with(SLPlaceholders.PLAYER_NAME, user::getName));

        this.sendPrefixed(PlaytimeLang.STATS_TOTAL, viewer, b -> b
            .with(SLPlaceholders.GENERIC_TIME, () -> this.format(this.getEffectivePlaytime(user, PlaytimePeriod.ALLTIME))));

        this.sendPeriodLine(viewer, "Year", this.getEffectivePlaytime(user, PlaytimePeriod.YEAR));
        this.sendPeriodLine(viewer, "Month", this.getEffectivePlaytime(user, PlaytimePeriod.MONTH));
        this.sendPeriodLine(viewer, "Week", this.getEffectivePlaytime(user, PlaytimePeriod.WEEK));
        this.sendPeriodLine(viewer, "Day", this.getEffectivePlaytime(user, PlaytimePeriod.DAY));

        this.sendPrefixed(PlaytimeLang.STATS_STREAK_DAILY, viewer, b -> b
            .with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK)))));
        this.sendPrefixed(PlaytimeLang.STATS_STREAK_WEEKLY, viewer, b -> b
            .with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK)))));
    }

    public void showSingleStat(@NotNull CommandSender viewer, @NotNull SunUser user, @NotNull PlaytimePeriod period) {
        this.ensureRollover(user);

        this.sendPrefixed(PlaytimeLang.STATS_HEADER, viewer, b -> b
            .with(SLPlaceholders.PLAYER_NAME, user::getName));

        if (period == PlaytimePeriod.ALLTIME) {
            this.sendPrefixed(PlaytimeLang.STATS_TOTAL, viewer, b -> b
                .with(SLPlaceholders.GENERIC_TIME, () -> this.format(this.getEffectivePlaytime(user, PlaytimePeriod.ALLTIME))));
            return;
        }

        String label = period.name().substring(0, 1) + period.name().substring(1).toLowerCase();
        this.sendPeriodLine(viewer, label, this.getEffectivePlaytime(user, period));
    }

    private void sendPeriodLine(@NotNull CommandSender viewer, @NotNull String name, long ms) {
        this.sendPrefixed(PlaytimeLang.STATS_PERIOD, viewer, b -> b
            .with(SLPlaceholders.GENERIC_NAME, () -> name)
            .with(SLPlaceholders.GENERIC_TIME, () -> this.format(ms)));
    }

    public void showGoals(@NotNull CommandSender viewer, @NotNull SunUser user) {
        this.ensureRollover(user);
        this.sendPrefixed(PlaytimeLang.GOAL_HEADER, viewer);
        this.sendGoalLine(viewer, user, "Daily", this.getEffectivePlaytime(user, PlaytimePeriod.DAY), this.getEffectiveGoalMs(user, true, false, false));
        this.sendGoalLine(viewer, user, "Weekly", this.getEffectivePlaytime(user, PlaytimePeriod.WEEK), this.getEffectiveGoalMs(user, false, true, false));
        this.sendGoalLine(viewer, user, "Monthly", this.getEffectivePlaytime(user, PlaytimePeriod.MONTH), this.getEffectiveGoalMs(user, false, false, true));
    }

    private void sendGoalLine(@NotNull CommandSender viewer, @NotNull SunUser user, @NotNull String name, long current, long goal) {
        if (goal <= 0L) {
            this.sendPrefixed(PlaytimeLang.GOAL_LINE_INACTIVE, viewer, b -> b
                .with(SLPlaceholders.GENERIC_NAME, () -> name));
            return;
        }
        long percent = Math.min(100L, current * 100L / Math.max(1L, goal));
        this.sendPrefixed(PlaytimeLang.GOAL_LINE, viewer, b -> b
            .with(SLPlaceholders.GENERIC_NAME, () -> name)
            .with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(percent))
            .with(SLPlaceholders.GENERIC_CURRENT, () -> this.format(Math.max(0L, current)))
            .with(SLPlaceholders.GENERIC_MAX, () -> this.format(goal)));
    }

    // --- Top ---

    public record TopEntry(UUID id, String name, long value) {
    }

    public List<TopEntry> getTop(@NotNull PlaytimePeriod period) {
        long refreshMs = Math.max(1000L, this.settings.getTopRefreshInterval() * 50L);
        if (System.currentTimeMillis() - this.topCacheTimestamp > refreshMs || !this.topCache.containsKey(period)) {
            this.refreshTopCache();
        }
        return this.topCache.getOrDefault(period, List.of());
    }

    public void refreshTopCache() {
        PeriodKeys keys;
        try {
            keys = this.currentKeys();
        } catch (Exception exception) {
            return;
        }

        Collection<SunUser> users;
        try {
            users = this.userManager.getAll();
        } catch (Exception exception) {
            return;
        }
        if (users == null || users.isEmpty()) {
            for (PlaytimePeriod period : PlaytimePeriod.values()) {
                this.topCache.put(period, List.of());
            }
            this.topCacheTimestamp = System.currentTimeMillis();
            return;
        }

        for (PlaytimePeriod period : PlaytimePeriod.values()) {
            List<TopEntry> entries = new ArrayList<>();
            for (SunUser user : users) {
                long value;
                try {
                    value = switch (period) {
                        case ALLTIME -> Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
                        case YEAR -> user.getPropertyOrDefault(PlaytimeProperties.YEAR_KEY) == keys.year() ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.YEAR)) : 0L;
                        case MONTH -> user.getPropertyOrDefault(PlaytimeProperties.MONTH_KEY) == keys.month() ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.MONTH)) : 0L;
                        case WEEK -> user.getPropertyOrDefault(PlaytimeProperties.WEEK_KEY) == keys.week() ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.WEEK)) : 0L;
                        case DAY -> user.getPropertyOrDefault(PlaytimeProperties.DAY_KEY) == keys.day() ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.DAY)) : 0L;
                    };
                } catch (Exception exception) {
                    continue;
                }
                if (value <= 0L) continue;
                String name = user.getName();
                if (name == null || name.isBlank()) continue;
                entries.add(new TopEntry(user.getId(), name, value));
            }
            entries.sort(Comparator.comparingLong(TopEntry::value).reversed());
            if (entries.size() > TOP_CACHE_LIMIT) entries = new ArrayList<>(entries.subList(0, TOP_CACHE_LIMIT));
            this.topCache.put(period, List.copyOf(entries));
        }
        this.topCacheTimestamp = System.currentTimeMillis();
    }

    public void showTop(@NotNull CommandSender viewer, @NotNull PlaytimePeriod period, int page) {
        List<TopEntry> entries = new ArrayList<>(this.getTop(period));
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) TOP_PAGE_SIZE));
        int currentPage = Math.min(Math.max(1, page), totalPages);

        String label = period == PlaytimePeriod.ALLTIME ? "Alltime"
            : period.name().substring(0, 1) + period.name().substring(1).toLowerCase();

        this.sendPrefixed(PlaytimeLang.TOP_TITLE, viewer, b -> b
            .with(SLPlaceholders.GENERIC_NAME, () -> label + "  " + currentPage + "/" + totalPages));

        int start = (currentPage - 1) * TOP_PAGE_SIZE;
        int end = Math.min(entries.size(), start + TOP_PAGE_SIZE);
        for (int index = start; index < end; index++) {
            TopEntry entry = entries.get(index);
            String rank = String.valueOf(index + 1);
            String formatted = this.format(entry.value());
            this.sendPrefixed(PlaytimeLang.TOP_ENTRY, viewer, b -> b
                .with(SLPlaceholders.GENERIC_PAGE, () -> rank)
                .with(SLPlaceholders.PLAYER_NAME, entry::name)
                .with(SLPlaceholders.GENERIC_TOTAL, () -> formatted));
        }
    }
}
