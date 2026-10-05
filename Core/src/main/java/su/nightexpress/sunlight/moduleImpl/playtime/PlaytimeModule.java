package su.nightexpress.sunlight.moduleImpl.playtime;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.PlaytimeProvider;
import su.nightexpress.sunlight.data.DataQueries;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.playtime.command.PlaytimeCommandProvider;
import su.nightexpress.sunlight.moduleImpl.playtime.config.PlaytimeLang;
import su.nightexpress.sunlight.moduleImpl.playtime.listener.PlaytimeListener;
import su.nightexpress.sunlight.moduleImpl.playtime.menu.PlaytimeStatsMenu;
import su.nightexpress.sunlight.moduleImpl.playtime.menu.PlaytimeTopMenu;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimeMilestone;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.Utils;

public class PlaytimeModule extends Module implements PlaytimeProvider {

    public static final int TOP_PAGE_SIZE = 10;
    public static final int TOP_CACHE_LIMIT = 100;

    private final PlaytimeSettings settings;
    private final Map<PlaytimePeriod, List<TopEntry>> topCache;

    private volatile Map<String, PlaytimeMilestone> milestones;
    private volatile List<PlaytimeMilestone> milestoneList;
    private volatile ZoneId zoneId;
    private volatile PeriodKeys keysCache;
    private volatile long keysCacheTime;

    private final AtomicBoolean topScanRunning = new AtomicBoolean(false);
    private volatile long topCacheTimestamp;
    private volatile long lastDbScan;
    private volatile boolean topRequested;

    public PlaytimeModule(ModuleDefinition<PlaytimeModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new PlaytimeSettings();
        this.milestones = Map.of();
        this.milestoneList = List.of();
        this.topCache = new ConcurrentHashMap<>();
        this.topCacheTimestamp = 0L;
        this.lastDbScan = 0L;
        this.topRequested = false;
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.settings.load(config);
        this.zoneId = this.settings.getZoneId();
        this.loadMilestones(config);
        config.saveChanges();
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
        UserPropertyRegistry.register(PlaytimeProperties.GOAL_YEAR);
        UserPropertyRegistry.register(PlaytimeProperties.GOAL_ALLTIME);
        UserPropertyRegistry.register(PlaytimeProperties.REWARDED_DAY_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.REWARDED_WEEK_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.REWARDED_MONTH_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.REMINDED_DAY_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.REMINDED_WEEK_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.REMINDED_MONTH_KEY);
        UserPropertyRegistry.register(PlaytimeProperties.SESSION);
        UserPropertyRegistry.register(PlaytimeProperties.SESSION_START);
        UserPropertyRegistry.register(PlaytimeProperties.MILESTONE_PROGRESS);

        this.addListener(new PlaytimeListener(this.plugin, this));
        this.addTask(this::tick, this.settings.getTickInterval());
        this.addAsyncTask(this::refreshTopCache, (int) Math.max(1L, this.settings.getTopRefreshInterval() / 20L));

        this.commandRegistry.addProvider(new PlaytimeCommandProvider(this));

        Utils.onlinePlayers().forEach(player -> {
            try {
                SunUser user = this.userManager.getOrFetch(player);
                if (user == null) return;
                this.ensureRollover(user);
                if (user.getPropertyOrDefault(PlaytimeProperties.SESSION_START) <= 0L) {
                    user.setProperty(PlaytimeProperties.SESSION_START, System.currentTimeMillis());
                }
            } catch (Exception exception) {
                this.debug("Playtime init failed for " + player.getName() + ": " + exception.getMessage());
            }
        });
        this.refreshTopCacheAsync();
    }

    @Override
    protected void unloadModule() {
        this.topCache.clear();
        this.topCacheTimestamp = 0L;
        this.lastDbScan = 0L;
        this.topRequested = false;
        this.milestones = Map.of();
        this.milestoneList = List.of();
    }

    public void reloadModule() {
        FileConfig config = this.getConfig();
        this.settings.load(config);
        this.zoneId = this.settings.getZoneId();
        this.loadMilestones(config);
        config.saveChanges();
        this.refreshTopCacheAsync();
    }

    /**
     * All placeholders served by this module ({@code %sunlight_<key>%}).
     * <p>
     * Counters: {@code playtime_total|year|month|week|day|session}, each with a
     * {@code _formatted} variant. Streaks: {@code playtime_streak_daily},
     * {@code playtime_streak_weekly}.
     * <p>
     * Goals, for {@code day|week|month}: {@code playtime_goal_<scope>} (ms),
     * {@code playtime_goal_<scope>_formatted},
     * {@code playtime_goal_<scope>_remaining[_formatted]},
     * {@code playtime_goal_<scope>_progress} (0-100). Generic form:
     * {@code playtime_goal_<scope>[_metric]}, e.g.
     * {@code %sunlight_playtime_goal_day_remaining%}.
     * <p>
     * Milestones: {@code playtime_milestones} (one-shots claimed),
     * {@code playtime_milestones_total} (one-shots configured),
     * {@code playtime_milestone_next} (name),
     * {@code playtime_milestone_next_remaining[_formatted]},
     * {@code playtime_milestone_next_progress}.
     * <p>
     * Leaderboard: {@code playtime_top_<rank>_<name|time|formatted>} and
     * {@code playtime_top_<period>_<rank>_<field>}, e.g.
     * {@code %sunlight_playtime_top_day_1_name%}.
     * Personal rank (1-based, {@code 0} when unranked):
     * {@code playtime_rank[_<period>]}.
     * <p>
     * Reset countdowns: {@code playtime_reset[_<day|week|month|year>][_formatted]},
     * e.g. {@code %sunlight_playtime_reset_day_formatted%}.
     */
    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("playtime_total",
                (player, payload) -> String.valueOf(this.getEffectivePlaytime(player, PlaytimePeriod.ALLTIME)));
        registry.register("playtime_total_formatted",
                (player, payload) -> this.format(this.getEffectivePlaytime(player, PlaytimePeriod.ALLTIME)));
        registry.register("playtime_year",
                (player, payload) -> String.valueOf(this.getEffectivePlaytime(player, PlaytimePeriod.YEAR)));
        registry.register("playtime_year_formatted",
                (player, payload) -> this.format(this.getEffectivePlaytime(player, PlaytimePeriod.YEAR)));
        registry.register("playtime_month",
                (player, payload) -> String.valueOf(this.getEffectivePlaytime(player, PlaytimePeriod.MONTH)));
        registry.register("playtime_month_formatted",
                (player, payload) -> this.format(this.getEffectivePlaytime(player, PlaytimePeriod.MONTH)));
        registry.register("playtime_week",
                (player, payload) -> String.valueOf(this.getEffectivePlaytime(player, PlaytimePeriod.WEEK)));
        registry.register("playtime_week_formatted",
                (player, payload) -> this.format(this.getEffectivePlaytime(player, PlaytimePeriod.WEEK)));
        registry.register("playtime_day",
                (player, payload) -> String.valueOf(this.getEffectivePlaytime(player, PlaytimePeriod.DAY)));
        registry.register("playtime_day_formatted",
                (player, payload) -> this.format(this.getEffectivePlaytime(player, PlaytimePeriod.DAY)));
        registry.register("playtime_session", (player, payload) -> String.valueOf(this.getSessionPlaytime(player)));
        registry.register("playtime_session_formatted",
                (player, payload) -> this.format(this.getSessionPlaytime(player)));
        registry.register("playtime_streak_daily", (player, payload) -> String.valueOf(this.getStreak(player, true)));
        registry.register("playtime_streak_weekly", (player, payload) -> String.valueOf(this.getStreak(player, false)));
        registry.register("playtime_goal_day", (player, payload) -> this.goalResponse(player, "day", payload));
        registry.register("playtime_goal_week", (player, payload) -> this.goalResponse(player, "week", payload));
        registry.register("playtime_goal_month", (player, payload) -> this.goalResponse(player, "month", payload));
        registry.register("playtime_goal", (player, payload) -> this.goalResponse(player, null, payload));
        registry.register("playtime_milestones", (player, payload) -> this.milestonesResponse(player, payload));
        registry.register("playtime_milestone_next", (player, payload) -> this.milestoneNextResponse(player, "name"));
        registry.register("playtime_milestone_next_remaining",
                (player, payload) -> this.milestoneNextResponse(player, "remaining"));
        registry.register("playtime_milestone_next_remaining_formatted",
                (player, payload) -> this.milestoneNextResponse(player, "remaining_formatted"));
        registry.register("playtime_milestone_next_progress",
                (player, payload) -> this.milestoneNextResponse(player, "progress"));
        registry.register("playtime_top", (player, payload) -> this.topResponse(payload));
        registry.register("playtime_rank", (player, payload) -> this.rankResponse(player, payload));
        registry.register("playtime_reset", (player, payload) -> this.resetResponse(payload));
    }

    private String goalResponse(Player player, String scopeOrNull, String payload) {
        String scope = scopeOrNull;
        String metric = payload == null ? "" : Utils.lowercase(payload.trim());

        if (scope == null) {
            if (metric == null || metric.isBlank())
                return "";
            int separator = metric.indexOf('_');
            if (separator < 0) {
                scope = metric;
                metric = "";
            } else {
                scope = metric.substring(0, separator);
                metric = metric.substring(separator + 1);
            }
        }
        if (scope == null || scope.isBlank())
            return "";
        if (player == null)
            return "0";

        boolean day = scope.equals("day") || scope.equals("daily");
        boolean week = scope.equals("week") || scope.equals("weekly");
        boolean month = !day && !week && (scope.equals("month") || scope.equals("monthly"));
        if (!day && !week && !month)
            return "";

        SunUser user = this.userManager.getOrFetch(player);
        if (user == null)
            return "0";

        long goal = this.getEffectiveGoalMs(user, day, week, month);
        PlaytimePeriod period = day ? PlaytimePeriod.DAY : week ? PlaytimePeriod.WEEK : PlaytimePeriod.MONTH;
        long current = this.getEffectivePlaytime(user, period);

        return switch (metric == null ? "" : metric) {
            case "" -> String.valueOf(Math.max(0L, goal));
            case "formatted" -> this.format(Math.max(0L, goal));
            case "remaining" -> String.valueOf(goal <= 0L ? 0L : Math.max(0L, goal - current));
            case "remaining_formatted" -> this.format(goal <= 0L ? 0L : Math.max(0L, goal - current));
            case "progress" -> String.valueOf(goal <= 0L || current <= 0L ? 0L : Math.min(100L, current * 100L / goal));
            default -> "";
        };
    }

    private String milestonesResponse(Player player, String payload) {
        if (player == null)
            return "0";
        String metric = payload == null ? "" : Utils.lowercase(payload.trim());
        if (!metric.isEmpty() && !metric.equals("total"))
            return "";

        Map<String, PlaytimeMilestone> all = this.milestones;
        if (metric.equals("total")) {
            int total = 0;
            for (PlaytimeMilestone milestone : all.values()) {
                if (!milestone.repeatable())
                    total++;
            }
            return String.valueOf(total);
        }

        SunUser user = this.userManager.getOrFetch(player);
        if (user == null)
            return "0";
        return String.valueOf(this.getMilestonesClaimedCount(user));
    }

    private PlaytimeMilestone nextMilestone(SunUser user) {
        Map<String, Integer> progress = user.getPropertyOrDefault(PlaytimeProperties.MILESTONE_PROGRESS);
        for (PlaytimeMilestone milestone : this.milestoneList) {
            if (!milestone.repeatable() && progress.getOrDefault(milestone.id(), 0) <= 0) {
                return milestone;
            }
        }
        return null;
    }

    private String milestoneNextResponse(Player player, String metric) {
        if (player == null)
            return metric.equals("name") ? "" : "0";
        SunUser user = this.userManager.getOrFetch(player);
        if (user == null)
            return metric.equals("name") ? "" : "0";

        PlaytimeMilestone next = this.nextMilestone(user);
        if (next == null)
            return metric.equals("progress") ? "100" : metric.equals("name") ? "" : "0";

        long total = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
        return switch (metric) {
            case "name" -> next.name();
            case "remaining" -> String.valueOf(Math.max(0L, next.thresholdMs() - total));
            case "remaining_formatted" -> this.format(Math.max(0L, next.thresholdMs() - total));
            case "progress" -> String.valueOf(Math.min(100L, total * 100L / Math.max(1L, next.thresholdMs())));
            default -> "";
        };
    }

    private String topResponse(String payload) {
        this.topRequested = true;
        if (payload == null || payload.isBlank())
            return "";

        String[] tokens = Utils.lowercase(payload.trim()).split("_");
        PlaytimePeriod period = PlaytimePeriod.ALLTIME;
        String rankRaw;
        String field;

        if (tokens.length == 2) {
            rankRaw = tokens[0];
            field = tokens[1];
        } else if (tokens.length == 3) {
            period = PlaytimePeriod.fromString(tokens[0]);
            rankRaw = tokens[1];
            field = tokens[2];
        } else if (tokens.length == 4 && (tokens[2].equals("time") && tokens[3].equals("formatted"))) {
            period = PlaytimePeriod.fromString(tokens[0]);
            rankRaw = tokens[1];
            field = "time_formatted";
        } else {
            return "";
        }

        int rank;
        try {
            rank = Integer.parseInt(rankRaw);
        } catch (NumberFormatException exception) {
            return "";
        }
        if (rank < 1 || rank > TOP_CACHE_LIMIT)
            return "";

        List<TopEntry> board = this.topCache.getOrDefault(period, List.of());
        if (rank > board.size())
            return "";
        TopEntry entry = board.get(rank - 1);

        return switch (field) {
            case "name" -> entry.name();
            case "time" -> String.valueOf(Math.max(0L, entry.value()));
            case "formatted", "time_formatted" -> this.format(Math.max(0L, entry.value()));
            default -> "";
        };
    }

    private String rankResponse(Player player, String payload) {
        this.topRequested = true;
        if (player == null)
            return "0";

        PlaytimePeriod period = payload == null || payload.isBlank()
                ? PlaytimePeriod.ALLTIME
                : PlaytimePeriod.fromString(payload.trim());

        List<TopEntry> board = this.topCache.getOrDefault(period, List.of());
        UUID id = player.getUniqueId();
        for (int index = 0; index < board.size(); index++) {
            if (board.get(index).id().equals(id)) {
                return String.valueOf(index + 1);
            }
        }
        return "0";
    }

    private String resetResponse(String payload) {
        String raw = payload == null ? "" : Utils.lowercase(payload.trim());
        boolean formatted = raw.endsWith("_formatted");
        if (formatted)
            raw = raw.substring(0, raw.length() - "_formatted".length());

        String period = raw.isBlank() ? "day" : raw;
        ZoneId zone = this.zoneId;
        if (zone == null)
            zone = ZoneId.systemDefault();

        ZonedDateTime now;
        ZonedDateTime next;
        try {
            now = ZonedDateTime.now(zone);
            next = switch (period) {
                case "day", "daily" -> now.toLocalDate().plusDays(1).atStartOfDay(zone);
                case "week", "weekly" ->
                    now.toLocalDate().with(java.time.DayOfWeek.MONDAY).plusWeeks(1).atStartOfDay(zone);
                case "month", "monthly" -> now.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay(zone);
                case "year", "yearly" -> now.toLocalDate().withDayOfYear(1).plusYears(1).atStartOfDay(zone);
                default -> null;
            };
        } catch (Exception exception) {
            return formatted ? this.format(0L) : "0";
        }
        if (next == null)
            return formatted ? this.format(0L) : "0";

        long remaining = Math.max(0L, next.toInstant().toEpochMilli() - now.toInstant().toEpochMilli());
        return formatted ? this.format(remaining) : String.valueOf(remaining);
    }

    public PlaytimeSettings getSettings() {
        return this.settings;
    }

    public Map<String, PlaytimeMilestone> getMilestones() {
        return Map.copyOf(this.milestones);
    }

    public String format(long ms) {
        if (ms <= 0L)
            return TimeFormats.formatAmount(0L, TimeFormatType.LITERAL);
        return TimeFormats.formatAmount(ms, TimeFormatType.LITERAL);
    }

    // --- Milestones config ---

    private void loadMilestones(FileConfig config) {
        if (config.getConfigurationSection("Playtime.Milestones") == null) {
            config.addMissing("Playtime.Milestones.starter.Name", "Starter");
            config.addMissing("Playtime.Milestones.starter.Minutes", 60L);
            config.addMissing("Playtime.Milestones.starter.Repeatable", false);
            config.addMissing("Playtime.Milestones.starter.Rewards", List.of("eco give %player_name% 500"));
            config.addMissing("Playtime.Milestones.veteran.Name", "Veteran");
            config.addMissing("Playtime.Milestones.veteran.Minutes", 600L);
            config.addMissing("Playtime.Milestones.veteran.Repeatable", false);
            config.addMissing("Playtime.Milestones.veteran.Rewards", List.of("eco give %player_name% 5000"));
            config.addMissing("Playtime.Milestones.grinder.Name", "Grinder");
            config.addMissing("Playtime.Milestones.grinder.Minutes", 120L);
            config.addMissing("Playtime.Milestones.grinder.Repeatable", true);
            config.addMissing("Playtime.Milestones.grinder.Rewards", List.of("eco give %player_name% 250"));
        }

        ConfigurationSection section = config.getConfigurationSection("Playtime.Milestones");
        if (section == null) {
            this.milestones = Map.of();
            this.milestoneList = List.of();
            return;
        }

        Map<String, PlaytimeMilestone> parsed = new LinkedHashMap<>();
        for (String id : section.getKeys(false)) {
            String key = Utils.lowercase(id);
            if (key == null || key.isBlank())
                continue;
            String base = "Playtime.Milestones." + id + ".";
            long minutes = Math.max(0L, config.getLong(base + "Minutes", 0L));
            if (minutes <= 0L)
                continue;
            String name = config.getString(base + "Name", id);
            if (name == null || name.isBlank())
                name = id;
            boolean repeatable = config.getBoolean(base + "Repeatable", false);
            List<String> rewards = config.getStringList(base + "Rewards");
            parsed.put(key, new PlaytimeMilestone(key, name, minutes * 60_000L, repeatable, List.copyOf(rewards)));
        }

        List<PlaytimeMilestone> ordered = new ArrayList<>(parsed.values());
        ordered.sort(Comparator.comparingLong(PlaytimeMilestone::thresholdMs));
        this.milestones = Map.copyOf(parsed);
        this.milestoneList = List.copyOf(ordered);
    }

    // --- Tick ---

    private void tick() {
        long deltaMs = this.settings.getTickInterval() * 50L;
        if (deltaMs <= 0L)
            return;

        boolean excludeAfk = this.settings.isExcludeAfkEnabled();
        var afkProvider = this.plugin.afkProvider();
        PeriodKeys keys = this.currentKeys();
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                SunUser user = this.userManager.getOrFetch(player);
                if (user == null) continue;
                this.ensureRollover(user, keys);
                // LAST_SEEN is write-only telemetry; throttle it so the hot tick
                // path does one less map write per player per second.
                long lastSeen = user.getPropertyOrDefault(PlaytimeProperties.LAST_SEEN);
                if (now - lastSeen >= 60_000L) {
                    user.setProperty(PlaytimeProperties.LAST_SEEN, now);
                }

                if (excludeAfk && afkProvider.isPresent() && afkProvider.get().isAfk(player)) {
                    continue;
                }

                user.setProperty(PlaytimeProperties.TOTAL,
                        user.getPropertyOrDefault(PlaytimeProperties.TOTAL) + deltaMs);
                user.setProperty(PlaytimeProperties.YEAR, user.getPropertyOrDefault(PlaytimeProperties.YEAR) + deltaMs);
                user.setProperty(PlaytimeProperties.MONTH,
                        user.getPropertyOrDefault(PlaytimeProperties.MONTH) + deltaMs);
                user.setProperty(PlaytimeProperties.WEEK, user.getPropertyOrDefault(PlaytimeProperties.WEEK) + deltaMs);
                user.setProperty(PlaytimeProperties.DAY, user.getPropertyOrDefault(PlaytimeProperties.DAY) + deltaMs);
                user.setProperty(PlaytimeProperties.SESSION,
                        user.getPropertyOrDefault(PlaytimeProperties.SESSION) + deltaMs);

                this.checkGoals(player, user);
                this.checkMilestones(player, user);
            } catch (Exception exception) {
                // One corrupt/edge-case record must never kill the tick for everyone else.
                this.debug("Playtime tick failed for " + player.getName() + ": " + exception.getMessage());
            }
        }
    }

    // --- Rollover & streaks ---

    public record PeriodKeys(long day, long week, long month, long year) {
    }

    public PeriodKeys currentKeys() {
        long now = System.currentTimeMillis();
        PeriodKeys cached = this.keysCache;
        if (cached != null && now - this.keysCacheTime < 1000L)
            return cached;

        ZoneId zone = this.zoneId;
        if (zone == null) {
            zone = ZoneId.systemDefault();
            this.zoneId = zone;
        }
        ZonedDateTime dateTime = ZonedDateTime.now(zone);
        PeriodKeys keys = new PeriodKeys(
                dateTime.toLocalDate().toEpochDay(),
                dateTime.toLocalDate().with(DayOfWeek.MONDAY).toEpochDay(),
                (long) dateTime.getYear() * 12L + (dateTime.getMonthValue() - 1),
                dateTime.getYear());
        this.keysCache = keys;
        this.keysCacheTime = now;
        return keys;
    }

    public void ensureRollover(SunUser user) {
        this.ensureRollover(user, this.currentKeys());
    }

    public void ensureRollover(SunUser user, PeriodKeys keys) {

        long storedDay = user.getPropertyOrDefault(PlaytimeProperties.DAY_KEY);
        if (storedDay != keys.day()) {
            if (storedDay == 0L) {
                user.setProperty(PlaytimeProperties.DAILY_STREAK,
                        Math.max(1, user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK)));
            } else if (storedDay == keys.day() - 1L) {
                user.setProperty(PlaytimeProperties.DAILY_STREAK,
                        user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK) + 1);
            } else {
                user.setProperty(PlaytimeProperties.DAILY_STREAK, 1);
            }
            user.setProperty(PlaytimeProperties.DAY, 0L);
            user.setProperty(PlaytimeProperties.DAY_KEY, keys.day());
        }

        long storedWeek = user.getPropertyOrDefault(PlaytimeProperties.WEEK_KEY);
        if (storedWeek != keys.week()) {
            if (storedWeek == 0L) {
                user.setProperty(PlaytimeProperties.WEEKLY_STREAK,
                        Math.max(1, user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK)));
            } else if (storedWeek == keys.week() - 7L) {
                user.setProperty(PlaytimeProperties.WEEKLY_STREAK,
                        user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK) + 1);
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

    public void startSession(SunUser user) {
        this.ensureRollover(user);
        user.setProperty(PlaytimeProperties.SESSION, 0L);
        user.setProperty(PlaytimeProperties.SESSION_START, System.currentTimeMillis());
        user.setProperty(PlaytimeProperties.LAST_SEEN, System.currentTimeMillis());
    }

    // --- Reads ---

    public long getStoredPlaytime(SunUser user, PlaytimePeriod period) {
        return switch (period) {
            case ALLTIME -> user.getPropertyOrDefault(PlaytimeProperties.TOTAL);
            case YEAR -> user.getPropertyOrDefault(PlaytimeProperties.YEAR);
            case MONTH -> user.getPropertyOrDefault(PlaytimeProperties.MONTH);
            case WEEK -> user.getPropertyOrDefault(PlaytimeProperties.WEEK);
            case DAY -> user.getPropertyOrDefault(PlaytimeProperties.DAY);
        };
    }

    public long getEffectivePlaytime(SunUser user, PlaytimePeriod period) {
        if (period == PlaytimePeriod.ALLTIME)
            return Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
        PeriodKeys keys = this.currentKeys();
        return switch (period) {
            case YEAR -> user.getPropertyOrDefault(PlaytimeProperties.YEAR_KEY) == keys.year()
                    ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.YEAR))
                    : 0L;
            case MONTH -> user.getPropertyOrDefault(PlaytimeProperties.MONTH_KEY) == keys.month()
                    ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.MONTH))
                    : 0L;
            case WEEK -> user.getPropertyOrDefault(PlaytimeProperties.WEEK_KEY) == keys.week()
                    ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.WEEK))
                    : 0L;
            case DAY -> user.getPropertyOrDefault(PlaytimeProperties.DAY_KEY) == keys.day()
                    ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.DAY))
                    : 0L;
            default -> Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
        };
    }

    private long getEffectivePlaytime(Player player, PlaytimePeriod period) {
        if (player == null)
            return 0L;
        SunUser user = this.userManager.getOrFetch(player);
        if (user == null)
            return 0L;
        return this.getEffectivePlaytime(user, period);
    }

    public long getSessionPlaytime(SunUser user) {
        return Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.SESSION));
    }

    private long getSessionPlaytime(Player player) {
        if (player == null)
            return 0L;
        SunUser user = this.userManager.getOrFetch(player);
        if (user == null)
            return 0L;
        return this.getSessionPlaytime(user);
    }

    private int getStreak(Player player, boolean daily) {
        if (player == null)
            return 0;
        SunUser user = this.userManager.getOrFetch(player);
        if (user == null)
            return 0;
        return daily ? Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK))
                : Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK));
    }

    public int getMilestonesClaimedCount(SunUser user) {
        Map<String, PlaytimeMilestone> all = this.milestones;
        if (all.isEmpty())
            return 0;
        Map<String, Integer> progress = user.getPropertyOrDefault(PlaytimeProperties.MILESTONE_PROGRESS);
        int claimed = 0;
        for (PlaytimeMilestone milestone : all.values()) {
            if (!milestone.repeatable() && progress.getOrDefault(milestone.id(), 0) > 0)
                claimed++;
        }
        return claimed;
    }

    public long getTotalPlaytimeMs(UUID playerId) {
        return this.userManager.getOrFetch(playerId)
                .map(user -> Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL)))
                .orElse(0L);
    }

    public long getTotalPlaytimeMs(Player player) {
        if (player == null) return 0L;
        SunUser user = this.userManager.getOrFetch(player);
        if (user == null) return 0L;
        return Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
    }

    // --- PlaytimeProvider ---

    @Override
    public long getPlaytimeMs(UUID playerId, String period) {
        PlaytimePeriod parsed;
        try {
            parsed = PlaytimePeriod.fromString(period);
        } catch (Exception exception) {
            parsed = PlaytimePeriod.ALLTIME;
        }
        final PlaytimePeriod resolved = parsed;
        return this.userManager.getOrFetch(playerId)
                .map(user -> this.getEffectivePlaytime(user, resolved))
                .orElse(0L);
    }

    @Override
    public long getSessionPlaytimeMs(UUID playerId) {
        return this.userManager.getOrFetch(playerId)
                .map(user -> Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.SESSION)))
                .orElse(0L);
    }

    @Override
    public int getDailyStreak(UUID playerId) {
        return this.userManager.getOrFetch(playerId)
                .map(user -> Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK)))
                .orElse(0);
    }

    @Override
    public int getWeeklyStreak(UUID playerId) {
        return this.userManager.getOrFetch(playerId)
                .map(user -> Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK)))
                .orElse(0);
    }

    @Override
    public long getGoalMs(UUID playerId, String scope) {
        String normalized = Utils.lowercase(scope);
        boolean day = normalized != null && (normalized.equals("day") || normalized.equals("daily"));
        boolean week = normalized != null && (normalized.equals("week") || normalized.equals("weekly"));
        boolean month = normalized != null && (normalized.equals("month") || normalized.equals("monthly"));
        if (!day && !week && !month) return 0L;
        return this.userManager.getOrFetch(playerId)
                .map(user -> this.getEffectiveGoalMs(user, day, week, month))
                .orElse(0L);
    }

    @Override
    public List<PlaytimeProvider.TopEntry> getTop(String period, int limit) {
        PlaytimePeriod parsed;
        try {
            parsed = PlaytimePeriod.fromString(period);
        } catch (Exception exception) {
            parsed = PlaytimePeriod.ALLTIME;
        }
        int max = Math.max(1, Math.min(limit <= 0 ? TOP_CACHE_LIMIT : limit, TOP_CACHE_LIMIT));
        List<TopEntry> cached = new ArrayList<>(this.getTop(parsed));
        List<PlaytimeProvider.TopEntry> result = new ArrayList<>(Math.min(max, cached.size()));
        for (int index = 0; index < Math.min(max, cached.size()); index++) {
            TopEntry entry = cached.get(index);
            result.add(new PlaytimeProvider.TopEntry(entry.id(), entry.name(), entry.value()));
        }
        return result;
    }

    // --- Goals ---

    public long getEffectiveGoalMs(SunUser user, boolean day, boolean week, boolean month) {
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
        if (player == null)
            return 0L;
        SunUser user = this.userManager.getOrFetch(player);
        if (user == null) return 0L;
        return this.getEffectiveGoalMs(user, day, week, month);
    }

    private void checkGoals(Player player, SunUser user) {
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

    private void giveGoalReward(Player player, List<String> commands, String goalName) {
        this.sendPrefixed(PlaytimeLang.REWARD_GOAL, player, b -> b
                .with(SLPlaceholders.GENERIC_NAME, () -> goalName));
        this.dispatchRewardCommands(player, commands);
    }

    // --- Milestones ---

    private void checkMilestones(Player player, SunUser user) {
        List<PlaytimeMilestone> ordered = this.milestoneList;
        if (ordered.isEmpty())
            return;

        long total = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
        if (total <= 0L)
            return;

        // Read-only fast path: no map copy unless something actually triggers.
        Map<String, Integer> progress = user.getPropertyOrDefault(PlaytimeProperties.MILESTONE_PROGRESS);
        Map<String, Integer> updated = null;

        for (PlaytimeMilestone milestone : ordered) {
            if (milestone.thresholdMs() <= 0L || total < milestone.thresholdMs())
                continue;

            if (milestone.repeatable()) {
                int reached = (int) Math.min(Integer.MAX_VALUE, total / milestone.thresholdMs());
                int claimed = Math.max(0, progress.getOrDefault(milestone.id(), 0));
                if (reached > claimed) {
                    if (updated == null)
                        updated = new HashMap<>(progress);
                    updated.put(milestone.id(), reached);
                    this.sendPrefixed(PlaytimeLang.MILESTONE_REWARD, player, b -> b
                            .with(SLPlaceholders.GENERIC_NAME, milestone::name));
                    this.dispatchRewardCommands(player, milestone.rewards());
                }
            } else if (!progress.containsKey(milestone.id())
                    && (updated == null || !updated.containsKey(milestone.id()))) {
                if (updated == null)
                    updated = new HashMap<>(progress);
                updated.put(milestone.id(), 1);
                this.sendPrefixed(PlaytimeLang.MILESTONE_REWARD, player, b -> b
                        .with(SLPlaceholders.GENERIC_NAME, milestone::name));
                this.dispatchRewardCommands(player, milestone.rewards());
            }
        }

        if (updated != null) {
            user.setProperty(PlaytimeProperties.MILESTONE_PROGRESS, updated);
        }
    }

    private void dispatchRewardCommands(Player player, List<String> commands) {
        if (commands.isEmpty())
            return;

        PlaceholderContext context = PlaceholderContext.builder()
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .andThen(SLPlaceholders.forPlayerWithPAPI(player))
                .build();

        List<String> resolved = new ArrayList<>(commands);
        resolved.replaceAll(command -> context.apply(command
                .replace("%player_name%", player.getName())
                .replace("%player_display_name%", player.getDisplayName())));

        CommandSender console = this.plugin.getServer().getConsoleSender();
        this.plugin
                .runTask(() -> resolved.forEach(command -> this.plugin.getServer().dispatchCommand(console, command)));
    }

    // --- Admin operations ---

    public void addPlaytime(SunUser user, PlaytimePeriod period, long deltaMs) {
        if (deltaMs == 0L || user == null)
            return;
        this.ensureRollover(user);
        if (period == null) {
            // TOTAL must grow exactly once; the old loop over values() added it 5x.
            user.setProperty(PlaytimeProperties.TOTAL,
                    Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL) + deltaMs));
            user.setProperty(PlaytimeProperties.YEAR,
                    Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.YEAR) + deltaMs));
            user.setProperty(PlaytimeProperties.MONTH,
                    Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.MONTH) + deltaMs));
            user.setProperty(PlaytimeProperties.WEEK,
                    Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.WEEK) + deltaMs));
            user.setProperty(PlaytimeProperties.DAY,
                    Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.DAY) + deltaMs));
            return;
        }
        this.addBucket(user, period, deltaMs);
    }

    private void addBucket(SunUser user, PlaytimePeriod period, long deltaMs) {
        switch (period) {
            case ALLTIME -> user.setProperty(PlaytimeProperties.TOTAL,
                    Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL) + deltaMs));
            case YEAR -> {
                user.setProperty(PlaytimeProperties.TOTAL,
                        Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL) + deltaMs));
                user.setProperty(PlaytimeProperties.YEAR,
                        Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.YEAR) + deltaMs));
            }
            case MONTH -> {
                user.setProperty(PlaytimeProperties.TOTAL,
                        Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL) + deltaMs));
                user.setProperty(PlaytimeProperties.MONTH,
                        Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.MONTH) + deltaMs));
            }
            case WEEK -> {
                user.setProperty(PlaytimeProperties.TOTAL,
                        Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL) + deltaMs));
                user.setProperty(PlaytimeProperties.WEEK,
                        Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.WEEK) + deltaMs));
            }
            case DAY -> {
                user.setProperty(PlaytimeProperties.TOTAL,
                        Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL) + deltaMs));
                user.setProperty(PlaytimeProperties.DAY,
                        Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.DAY) + deltaMs));
            }
        }
    }

    public void setPlaytime(SunUser user, PlaytimePeriod period, long valueMs) {
        long value = Math.max(0L, valueMs);
        this.ensureRollover(user);
        if (period == null) {
            user.setProperty(PlaytimeProperties.TOTAL, value);
            user.setProperty(PlaytimeProperties.YEAR, value);
            user.setProperty(PlaytimeProperties.MONTH, value);
            user.setProperty(PlaytimeProperties.WEEK, value);
            user.setProperty(PlaytimeProperties.DAY, value);
            return;
        }
        switch (period) {
            case ALLTIME -> user.setProperty(PlaytimeProperties.TOTAL, value);
            case YEAR -> user.setProperty(PlaytimeProperties.YEAR, value);
            case MONTH -> user.setProperty(PlaytimeProperties.MONTH, value);
            case WEEK -> user.setProperty(PlaytimeProperties.WEEK, value);
            case DAY -> user.setProperty(PlaytimeProperties.DAY, value);
        }
    }

    public void resetScope(SunUser user, String scope) {
        String normalized = Utils.lowercase(scope);
        if (normalized == null)
            return;
        PeriodKeys keys = this.currentKeys();
        switch (normalized) {
            case "all" -> {
                user.setProperty(PlaytimeProperties.TOTAL, 0L);
                user.setProperty(PlaytimeProperties.YEAR, 0L);
                user.setProperty(PlaytimeProperties.MONTH, 0L);
                user.setProperty(PlaytimeProperties.WEEK, 0L);
                user.setProperty(PlaytimeProperties.DAY, 0L);
                user.setProperty(PlaytimeProperties.SESSION, 0L);
                user.setProperty(PlaytimeProperties.DAILY_STREAK, 0);
                user.setProperty(PlaytimeProperties.WEEKLY_STREAK, 0);
                user.setProperty(PlaytimeProperties.GOAL_DAY, 0L);
                user.setProperty(PlaytimeProperties.GOAL_WEEK, 0L);
                user.setProperty(PlaytimeProperties.GOAL_MONTH, 0L);
                user.setProperty(PlaytimeProperties.REWARDED_DAY_KEY, 0L);
                user.setProperty(PlaytimeProperties.REWARDED_WEEK_KEY, 0L);
                user.setProperty(PlaytimeProperties.REWARDED_MONTH_KEY, 0L);
                user.setProperty(PlaytimeProperties.REMINDED_DAY_KEY, 0L);
                user.setProperty(PlaytimeProperties.REMINDED_WEEK_KEY, 0L);
                user.setProperty(PlaytimeProperties.REMINDED_MONTH_KEY, 0L);
                user.setProperty(PlaytimeProperties.MILESTONE_PROGRESS, new HashMap<String, Integer>());
            }
            case "alltime", "total" -> user.setProperty(PlaytimeProperties.TOTAL, 0L);
            case "year" -> {
                user.setProperty(PlaytimeProperties.YEAR, 0L);
                user.setProperty(PlaytimeProperties.YEAR_KEY, keys.year());
            }
            case "month" -> {
                user.setProperty(PlaytimeProperties.MONTH, 0L);
                user.setProperty(PlaytimeProperties.MONTH_KEY, keys.month());
                user.setProperty(PlaytimeProperties.REWARDED_MONTH_KEY, 0L);
                user.setProperty(PlaytimeProperties.REMINDED_MONTH_KEY, 0L);
            }
            case "week" -> {
                user.setProperty(PlaytimeProperties.WEEK, 0L);
                user.setProperty(PlaytimeProperties.WEEK_KEY, keys.week());
                user.setProperty(PlaytimeProperties.REWARDED_WEEK_KEY, 0L);
                user.setProperty(PlaytimeProperties.REMINDED_WEEK_KEY, 0L);
            }
            case "day" -> {
                user.setProperty(PlaytimeProperties.DAY, 0L);
                user.setProperty(PlaytimeProperties.DAY_KEY, keys.day());
                user.setProperty(PlaytimeProperties.REWARDED_DAY_KEY, 0L);
                user.setProperty(PlaytimeProperties.REMINDED_DAY_KEY, 0L);
            }
            case "session" -> {
                user.setProperty(PlaytimeProperties.SESSION, 0L);
                user.setProperty(PlaytimeProperties.SESSION_START, System.currentTimeMillis());
            }
            case "streaks", "streak" -> {
                user.setProperty(PlaytimeProperties.DAILY_STREAK, 0);
                user.setProperty(PlaytimeProperties.WEEKLY_STREAK, 0);
            }
            case "goals", "goal" -> {
                user.setProperty(PlaytimeProperties.GOAL_DAY, 0L);
                user.setProperty(PlaytimeProperties.GOAL_WEEK, 0L);
                user.setProperty(PlaytimeProperties.GOAL_MONTH, 0L);
            }
            case "milestones", "milestone" ->
                user.setProperty(PlaytimeProperties.MILESTONE_PROGRESS, new HashMap<String, Integer>());
            default -> {
            }
        }
    }

    // --- Display ---

    public void showStats(CommandSender viewer, SunUser user) {
        this.ensureRollover(user);

        this.sendPrefixed(PlaytimeLang.STATS_HEADER, viewer, b -> b
                .with(SLPlaceholders.PLAYER_NAME, user::getName));

        this.sendPrefixed(PlaytimeLang.STATS_TOTAL, viewer, b -> b
                .with(SLPlaceholders.GENERIC_TIME,
                        () -> this.format(this.getEffectivePlaytime(user, PlaytimePeriod.ALLTIME))));

        this.sendPeriodLine(viewer, "Year", this.getEffectivePlaytime(user, PlaytimePeriod.YEAR));
        this.sendPeriodLine(viewer, "Month", this.getEffectivePlaytime(user, PlaytimePeriod.MONTH));
        this.sendPeriodLine(viewer, "Week", this.getEffectivePlaytime(user, PlaytimePeriod.WEEK));
        this.sendPeriodLine(viewer, "Day", this.getEffectivePlaytime(user, PlaytimePeriod.DAY));

        this.sendPrefixed(PlaytimeLang.STATS_STREAK_DAILY, viewer, b -> b
                .with(SLPlaceholders.GENERIC_VALUE,
                        () -> String.valueOf(Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK)))));
        this.sendPrefixed(PlaytimeLang.STATS_STREAK_WEEKLY, viewer, b -> b
                .with(SLPlaceholders.GENERIC_VALUE, () -> String
                        .valueOf(Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK)))));
    }

    public void showSingleStat(CommandSender viewer, SunUser user, PlaytimePeriod period) {
        this.ensureRollover(user);

        this.sendPrefixed(PlaytimeLang.STATS_HEADER, viewer, b -> b
                .with(SLPlaceholders.PLAYER_NAME, user::getName));

        if (period == PlaytimePeriod.ALLTIME) {
            this.sendPrefixed(PlaytimeLang.STATS_TOTAL, viewer, b -> b
                    .with(SLPlaceholders.GENERIC_TIME,
                            () -> this.format(this.getEffectivePlaytime(user, PlaytimePeriod.ALLTIME))));
            return;
        }

        String label = period.name().substring(0, 1) + period.name().substring(1).toLowerCase();
        this.sendPeriodLine(viewer, label, this.getEffectivePlaytime(user, period));
    }

    private void sendPeriodLine(CommandSender viewer, String name, long ms) {
        this.sendPrefixed(PlaytimeLang.STATS_PERIOD, viewer, b -> b
                .with(SLPlaceholders.GENERIC_NAME, () -> name)
                .with(SLPlaceholders.GENERIC_TIME, () -> this.format(ms)));
    }

    public void showGoals(CommandSender viewer, SunUser user) {
        this.ensureRollover(user);
        this.sendPrefixed(PlaytimeLang.GOAL_HEADER, viewer);
        this.sendGoalLine(viewer, user, "Daily", this.getEffectivePlaytime(user, PlaytimePeriod.DAY),
                this.getEffectiveGoalMs(user, true, false, false));
        this.sendGoalLine(viewer, user, "Weekly", this.getEffectivePlaytime(user, PlaytimePeriod.WEEK),
                this.getEffectiveGoalMs(user, false, true, false));
        this.sendGoalLine(viewer, user, "Monthly", this.getEffectivePlaytime(user, PlaytimePeriod.MONTH),
                this.getEffectiveGoalMs(user, false, false, true));
    }

    private void sendGoalLine(CommandSender viewer, SunUser user, String name, long current, long goal) {
        if (goal <= 0L) {
            this.sendPrefixed(PlaytimeLang.GOAL_LINE_INACTIVE, viewer, b -> b
                    .with(SLPlaceholders.GENERIC_NAME, () -> name));
            return;
        }
        long percent = current <= 0L ? 0L : Math.min(100L, current * 100L / Math.max(1L, goal));
        this.sendPrefixed(PlaytimeLang.GOAL_LINE, viewer, b -> b
                .with(SLPlaceholders.GENERIC_NAME, () -> name)
                .with(SLPlaceholders.GENERIC_VALUE, () -> String.valueOf(percent))
                .with(SLPlaceholders.GENERIC_CURRENT, () -> this.format(Math.max(0L, current)))
                .with(SLPlaceholders.GENERIC_MAX, () -> this.format(goal)));
    }

    public boolean openStatsMenu(Player viewer, SunUser target) {
        this.ensureRollover(target);
        return new PlaytimeStatsMenu(this, target.getId()).show(this.plugin, viewer);
    }

    public boolean openTopMenu(Player viewer, PlaytimePeriod period) {
        return new PlaytimeTopMenu(this, period).show(this.plugin, viewer);
    }

    // --- Top ---

    public record TopEntry(UUID id, String name, long value) {
    }

    public List<TopEntry> getTop(PlaytimePeriod period) {
        this.topRequested = true;
        PlaytimePeriod key = period == null ? PlaytimePeriod.ALLTIME : period;
        List<TopEntry> cached = this.topCache.getOrDefault(key, List.of());
        long refreshMs = Math.max(1000L, this.settings.getTopRefreshInterval() * 50L);
        if (System.currentTimeMillis() - this.topCacheTimestamp > refreshMs) {
            this.refreshTopCacheAsync();
        }
        // Already an immutable snapshot; callers must not mutate. Avoids copying
        // a 100-row list on every placeholder resolution.
        return cached;
    }

    public void refreshTopCacheAsync() {
        try {
            this.plugin.runTaskAsync(task -> this.refreshTopCache());
        } catch (Exception exception) {
            this.refreshTopCache();
        }
    }

    /**
     * Rebuilds the leaderboard. Live users are always merged in (cheap); the
     * full database scan only runs when the board was actually requested and
     * the scan interval has passed, since playtime lives in the user table's
     * JSON properties blob and cannot be sorted in SQL. Runs off-thread, and
     * concurrent runs collapse into one via the in-flight guard.
     */
    public void refreshTopCache() {
        if (!this.topScanRunning.compareAndSet(false, true))
            return;
        try {
            this.refreshTopCacheUnsafe();
        } finally {
            this.topScanRunning.set(false);
        }
    }

    private void refreshTopCacheUnsafe() {
        PeriodKeys keys;
        try {
            keys = this.currentKeys();
        } catch (Exception exception) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean dbDue = now - this.lastDbScan > this.settings.getTopDbScanInterval() * 50L;
        boolean wantDb = this.topRequested || this.lastDbScan == 0L;

        Map<UUID, SunUser> merged = new LinkedHashMap<>();
        if (wantDb && dbDue) {
            long scanStart = System.nanoTime();
            int rows = 0;
            try {
                for (SunUser persisted : this.dataHandler.selectAny(this.dataHandler.getUsersTable(),
                        DataQueries.SELECT_USER)) {
                    if (persisted == null)
                        continue;
                    merged.put(persisted.getId(), persisted);
                    rows++;
                }
                this.lastDbScan = now;
                this.topRequested = false;
                this.debug("Top database scan: " + rows + " users in " + ((System.nanoTime() - scanStart) / 1_000_000L)
                        + "ms.");
            } catch (Exception exception) {
                this.debug("Top refresh: database scan failed, falling back to cached users.");
            }
        }
        try {
            Collection<SunUser> live = this.userManager.getAll();
            if (live != null) {
                for (SunUser user : live) {
                    if (user == null)
                        continue;
                    merged.put(user.getId(), user);
                }
            }
        } catch (Exception exception) {
            // Live data is best-effort; persisted rows still produce a board.
        }

        this.rebuildTopBoards(merged, keys);
        this.topCacheTimestamp = System.currentTimeMillis();
    }

    private void rebuildTopBoards(Map<UUID, SunUser> users, PeriodKeys keys) {
        if (users.isEmpty()) {
            for (PlaytimePeriod period : PlaytimePeriod.values()) {
                this.topCache.put(period, List.of());
            }
            return;
        }

        // Single pass over users instead of one pass per period: each user's five
        // bucket values are resolved once, then appended to the matching boards.
        List<TopEntry> alltime = new ArrayList<>();
        List<TopEntry> year = new ArrayList<>();
        List<TopEntry> month = new ArrayList<>();
        List<TopEntry> week = new ArrayList<>();
        List<TopEntry> day = new ArrayList<>();
        for (SunUser user : users.values()) {
            if (user == null) continue;
            String name;
            UUID id;
            long total;
            long yearValue;
            long monthValue;
            long weekValue;
            long dayValue;
            try {
                name = user.getName();
                if (name == null || name.isBlank()) continue;
                id = user.getId();
                if (id == null) continue;
                total = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
                yearValue = user.getPropertyOrDefault(PlaytimeProperties.YEAR_KEY) == keys.year()
                        ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.YEAR)) : 0L;
                monthValue = user.getPropertyOrDefault(PlaytimeProperties.MONTH_KEY) == keys.month()
                        ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.MONTH)) : 0L;
                weekValue = user.getPropertyOrDefault(PlaytimeProperties.WEEK_KEY) == keys.week()
                        ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.WEEK)) : 0L;
                dayValue = user.getPropertyOrDefault(PlaytimeProperties.DAY_KEY) == keys.day()
                        ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.DAY)) : 0L;
            } catch (Exception exception) {
                continue;
            }
            if (total > 0L) alltime.add(new TopEntry(id, name, total));
            if (yearValue > 0L) year.add(new TopEntry(id, name, yearValue));
            if (monthValue > 0L) month.add(new TopEntry(id, name, monthValue));
            if (weekValue > 0L) week.add(new TopEntry(id, name, weekValue));
            if (dayValue > 0L) day.add(new TopEntry(id, name, dayValue));
        }

        Comparator<TopEntry> order = Comparator.comparingLong(TopEntry::value).reversed();
        this.topCache.put(PlaytimePeriod.ALLTIME, topSnapshot(alltime, order));
        this.topCache.put(PlaytimePeriod.YEAR, topSnapshot(year, order));
        this.topCache.put(PlaytimePeriod.MONTH, topSnapshot(month, order));
        this.topCache.put(PlaytimePeriod.WEEK, topSnapshot(week, order));
        this.topCache.put(PlaytimePeriod.DAY, topSnapshot(day, order));
    }

    private static List<TopEntry> topSnapshot(List<TopEntry> entries, Comparator<TopEntry> order) {
        if (entries.isEmpty()) return List.of();
        entries.sort(order);
        if (entries.size() > TOP_CACHE_LIMIT) {
            entries = new ArrayList<>(entries.subList(0, TOP_CACHE_LIMIT));
        }
        return List.copyOf(entries);
    }

    public void showTop(CommandSender viewer, PlaytimePeriod period, int page) {
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
