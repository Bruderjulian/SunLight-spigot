package su.nightexpress.sunlight.moduleImpl.playtime;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.event.PlaytimeAdjustedEvent;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.playtime.command.PlaytimeCommandProvider;
import su.nightexpress.sunlight.moduleImpl.playtime.config.PlaytimeLang;
import su.nightexpress.sunlight.moduleImpl.playtime.listener.PlaytimeListener;
import su.nightexpress.sunlight.moduleImpl.playtime.menu.PlaytimeStatsMenu;
import su.nightexpress.sunlight.moduleImpl.playtime.menu.PlaytimeTopMenu;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.Utils;

public class PlaytimeModule extends Module {

    public static final int TOP_PAGE_SIZE = 10;
    public static final int TOP_CACHE_LIMIT = 100;

    private final PlaytimeSettings settings;
    public final PlaytimePlaceholders placeholders;
    public final PlaytimeTopHandler topHandler;
    public final PlaytimeMilestoneHandler milestoneHandler;

    public volatile ZoneId zoneId;
    public volatile PeriodKeys keysCache;
    public volatile long keysCacheTime;

    public PlaytimeModule(final ModuleDefinition<PlaytimeModule> definition, final SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new PlaytimeSettings();
        this.placeholders = new PlaytimePlaceholders(this);
        this.topHandler = new PlaytimeTopHandler(this);
        this.milestoneHandler = new PlaytimeMilestoneHandler(this);
    }

    @Override
    protected void loadModule(final FileConfig config) {
        this.settings.load(config);
        this.zoneId = this.settings.getZoneId();
        milestoneHandler.loadMilestones(config);
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

        this.commandRegistry.addProvider(new PlaytimeCommandProvider(this));

        Utils.onlinePlayers().forEach(player -> {
            try {
                final SunUser user = this.userManager.getOrFetch(player);
                if (user == null)
                    return;
                this.ensureRollover(user);
                if (user.getPropertyOrDefault(PlaytimeProperties.SESSION_START) <= 0L) {
                    user.setProperty(PlaytimeProperties.SESSION_START, System.currentTimeMillis());
                }
            } catch (final Exception exception) {
                this.debug("Playtime init failed for " + player.getName() + ": " + exception.getMessage());
            }
        });
        topHandler.refreshTopCacheAsync();
    }

    @Override
    protected void unloadModule() {
        topHandler.unload();
        milestoneHandler.unload();
    }

    public void reloadModule() {
        final FileConfig config = this.getConfig();
        this.settings.load(config);
        this.zoneId = this.settings.getZoneId();
        milestoneHandler.loadMilestones(config);
        config.saveChanges();
        topHandler.refreshTopCacheAsync();
    }

    @Override
    public void registerPlaceholders(final PlaceholderRegistry registry) {
        placeholders.registerPlaceholders(registry);
    }

    public PlaytimeSettings getSettings() {
        return this.settings;
    }

    // --- Tick ---

    private void tick() {
        final long deltaMs = this.settings.getTickInterval() * 50L;
        if (deltaMs <= 0L)
            return;

        final boolean excludeAfk = this.settings.isExcludeAfkEnabled();
        final var afkProvider = this.plugin.afkProvider();
        final PeriodKeys keys = this.currentKeys();
        final long now = System.currentTimeMillis();

        for (final Player player : Bukkit.getOnlinePlayers()) {
            try {
                final SunUser user = this.userManager.getOrFetch(player);
                if (user == null)
                    continue;
                this.ensureRollover(user, keys);
                // LAST_SEEN is write-only telemetry; throttle it so the hot tick
                // path does one less map write per player per second.
                final long lastSeen = user.getPropertyOrDefault(PlaytimeProperties.LAST_SEEN);
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

                this.milestoneHandler.checkGoals(player, user);
                milestoneHandler.checkMilestones(player, user);
            } catch (final Exception exception) {
                // One corrupt/edge-case record must never kill the tick for everyone else.
                this.debug("Playtime tick failed for " + player.getName() + ": " + exception.getMessage());
            }
        }
    }

    // --- Rollover & streaks ---

    public record PeriodKeys(long day, long week, long month, long year) {
    }

    public PeriodKeys currentKeys() {
        final long now = System.currentTimeMillis();

        final PeriodKeys cached = this.keysCache;
        if (cached != null && now - this.keysCacheTime < 1000L) {
            return cached;
        }
        ZoneId zone = this.zoneId;
        if (zone == null) {
            zone = ZoneId.systemDefault();
            this.zoneId = zone;
        }
        final ZonedDateTime dateTime = ZonedDateTime.now(zone);
        final PeriodKeys keys = new PeriodKeys(
                dateTime.toLocalDate().toEpochDay(),
                dateTime.toLocalDate().with(DayOfWeek.MONDAY).toEpochDay(),
                (long) dateTime.getYear() * 12L + (dateTime.getMonthValue() - 1),
                dateTime.getYear());
        this.keysCache = keys;
        this.keysCacheTime = now;
        return keys;
    }

    public void ensureRollover(final SunUser user) {
        this.ensureRollover(user, this.currentKeys());
    }

    public void ensureRollover(final SunUser user, final PeriodKeys keys) {
        final long storedDay = user.getPropertyOrDefault(PlaytimeProperties.DAY_KEY);
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

        final long storedWeek = user.getPropertyOrDefault(PlaytimeProperties.WEEK_KEY);
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

    public void startSession(final SunUser user) {
        this.ensureRollover(user);
        user.setProperty(PlaytimeProperties.SESSION, 0L);
        user.setProperty(PlaytimeProperties.SESSION_START, System.currentTimeMillis());
        user.setProperty(PlaytimeProperties.LAST_SEEN, System.currentTimeMillis());
    }

    // --- Reads ---

    public long getEffectivePlaytime(final SunUser user, final PlaytimePeriod period) {
        if (period == PlaytimePeriod.ALLTIME) {
            return Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
        }
        if (period == PlaytimePeriod.SESSION) {
            return Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.SESSION));
        }
        final PeriodKeys keys = this.currentKeys();
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

    public long getEffectivePlaytime(final Player player, final PlaytimePeriod period) {
        if (player == null)
            return 0L;
        final SunUser user = userManager.getOrFetch(player);
        if (user == null)
            return 0L;
        return getEffectivePlaytime(user, period);
    }

    public void dispatchRewardCommands(final Player player, final List<String> commands) {
        if (commands.isEmpty())
            return;

        final PlaceholderContext context = PlaceholderContext.builder()
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .andThen(SLPlaceholders.forPlayerWithPAPI(player))
                .build();

        final List<String> resolved = new ArrayList<>(commands);
        resolved.replaceAll(command -> context.apply(command
                .replace("%player_name%", player.getName())
                .replace("%player_display_name%", player.getDisplayName())));

        final CommandSender console = plugin.getServer().getConsoleSender();
        plugin
                .runTask(() -> resolved
                        .forEach(command -> plugin.getServer().dispatchCommand(console, command)));
    }
    // --- Admin operations ---

    private long readCounter(final SunUser user, final PlaytimePeriod period) {
        return switch (period == null ? PlaytimePeriod.ALLTIME : period) {
            case ALLTIME -> user.getPropertyOrDefault(PlaytimeProperties.TOTAL);
            case YEAR -> user.getPropertyOrDefault(PlaytimeProperties.YEAR);
            case MONTH -> user.getPropertyOrDefault(PlaytimeProperties.MONTH);
            case WEEK -> user.getPropertyOrDefault(PlaytimeProperties.WEEK);
            case DAY -> user.getPropertyOrDefault(PlaytimeProperties.DAY);
            case SESSION -> user.getPropertyOrDefault(PlaytimeProperties.SESSION);
        };
    }

    public void addPlaytime(final SunUser user, final PlaytimePeriod period, final long deltaMs) {
        if (deltaMs == 0L || user == null)
            return;
        this.ensureRollover(user);

        final long previous = this.readCounter(user, period);
        final PlaytimeAdjustedEvent event = new PlaytimeAdjustedEvent(user, PlaytimeAdjustedEvent.Operation.ADD, period,
                null, previous, previous + deltaMs);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

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

    private void addBucket(final SunUser user, final PlaytimePeriod period, final long deltaMs) {
        switch (period) {
            case ALLTIME -> {
                user.setProperty(PlaytimeProperties.TOTAL,
                        Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL) + deltaMs));
            }
            case SESSION -> {
                user.setProperty(PlaytimeProperties.SESSION,
                        Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.SESSION) + deltaMs));
            }
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

    public void setPlaytime(final SunUser user, final PlaytimePeriod period, final long valueMs) {
        if (user == null)
            return;
        final long value = Math.max(0L, valueMs);
        this.ensureRollover(user);

        final PlaytimeAdjustedEvent event = new PlaytimeAdjustedEvent(user, PlaytimeAdjustedEvent.Operation.SET, period,
                null, this.readCounter(user, period), value);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        if (period == null) {
            user.setProperty(PlaytimeProperties.TOTAL, value);
            user.setProperty(PlaytimeProperties.YEAR, value);
            user.setProperty(PlaytimeProperties.MONTH, value);
            user.setProperty(PlaytimeProperties.WEEK, value);
            user.setProperty(PlaytimeProperties.DAY, value);
            user.setProperty(PlaytimeProperties.SESSION, value);
            return;
        }
        switch (period) {
            case ALLTIME -> user.setProperty(PlaytimeProperties.TOTAL, value);
            case YEAR -> user.setProperty(PlaytimeProperties.YEAR, value);
            case MONTH -> user.setProperty(PlaytimeProperties.MONTH, value);
            case WEEK -> user.setProperty(PlaytimeProperties.WEEK, value);
            case DAY -> user.setProperty(PlaytimeProperties.DAY, value);
            case SESSION -> user.setProperty(PlaytimeProperties.SESSION, value);
        }
    }

    public void resetScope(final SunUser user, final String scope) {
        final String normalized = Utils.lowercase(scope);
        if (normalized == null)
            return;

        // Only scopes that actually zero a playtime counter can be reported as a period
        // change;
        // "session", "streaks", "goals" and "milestones" clear unrelated bookkeeping
        // instead.
        final PlaytimePeriod period = switch (normalized) {
            case "all", "alltime", "total" -> PlaytimePeriod.ALLTIME;
            case "year" -> PlaytimePeriod.YEAR;
            case "month" -> PlaytimePeriod.MONTH;
            case "week" -> PlaytimePeriod.WEEK;
            case "day" -> PlaytimePeriod.DAY;
            default -> null;
        };
        final PlaytimeAdjustedEvent event = new PlaytimeAdjustedEvent(user, PlaytimeAdjustedEvent.Operation.RESET,
                period, normalized, this.readCounter(user, period), 0L);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        final PeriodKeys keys = this.currentKeys();
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

    public boolean openStatsMenu(final Player viewer, final SunUser target) {
        this.ensureRollover(target);
        return new PlaytimeStatsMenu(this, target.getId()).show(this.plugin, viewer);
    }

    public boolean openTopMenu(final Player viewer, final PlaytimePeriod period) {
        return new PlaytimeTopMenu(this, period).show(this.plugin, viewer);
    }
}
