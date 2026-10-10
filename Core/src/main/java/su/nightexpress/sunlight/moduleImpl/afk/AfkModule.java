package su.nightexpress.sunlight.moduleImpl.afk;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import su.nightexpress.nightcore.bridge.chat.UniversalChatEvent;
import su.nightexpress.nightcore.bridge.chat.UniversalChatEventHandler;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.geodata.pos.BlockPos;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.AfkProvider;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.afk.command.AfkCommandProvider;
import su.nightexpress.sunlight.moduleImpl.afk.core.AfkLang;
import su.nightexpress.sunlight.moduleImpl.afk.core.AfkSettings;
import su.nightexpress.sunlight.moduleImpl.afk.event.PlayerAfkEvent;
import su.nightexpress.sunlight.moduleImpl.afk.listener.AfkListener;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class AfkModule extends Module implements AfkProvider {

    public static final UserProperty<Long> AFK_TOTAL_TIME = UserProperty.create("afk_total_time", Long.class, 0L, true);
    public static final UserProperty<Integer> AFK_COUNT = UserProperty.create("afk_count", Integer.class, 0, true);

    private final Map<UUID, ActivityTracker> activityTrackerMap;
    private final Map<UUID, Long> kickedForIdling;
    private final AfkSettings settings;

    private UniversalChatEventHandler chatEventHandler;

    public AfkModule(final ModuleDefinition<AfkModule> definition, final SunLightPlugin plugin) {
        super(definition, plugin);
        this.activityTrackerMap = new ConcurrentHashMap<>();
        this.kickedForIdling = new ConcurrentHashMap<>();
        this.settings = new AfkSettings();
    }

    @Override
    protected void loadModule(final FileConfig config) {
        this.settings.load(config);
        this.plugin.injectLang(AfkLang.class);
        UserPropertyRegistry.register(AFK_TOTAL_TIME);
        UserPropertyRegistry.register(AFK_COUNT);

        this.addListener(new AfkListener(this.plugin, this));

        if (this.settings.getActivityPoints(ActivityType.CHAT) > 0) {
            this.chatEventHandler = this::handleChatEvent;
            this.plugin.addChatHandler(EventPriority.MONITOR, this.chatEventHandler);
        }

        this.addTask(this::tickTrackers, 20L);

        Utils.onlinePlayers().forEach(this::track);
        this.commandRegistry.addProvider(new AfkCommandProvider(this));
    }

    @Override
    protected void unloadModule() {
        if (this.chatEventHandler != null) {
            this.plugin.removeChatHandler(this.chatEventHandler);
            this.chatEventHandler = null;
        }

        Utils.onlinePlayers().forEach(player -> this.exitAfk(player, true));
        this.activityTrackerMap.clear();
        this.kickedForIdling.clear();
    }

    @Override
    public void registerPlaceholders(final PlaceholderRegistry registry) {
        registry.register("afk_state", (player, payload) -> {
            return CoreLang.STATE_YES_NO.get(this.isAfk(player));
        });

        registry.register("afk_mode", (player, payload) -> {
            return PlaceholderContext.builder()
                    .with(SLPlaceholders.GENERIC_TIME,
                            () -> TimeFormats.formatSince(this.getAfkEnterTimestamp(player), TimeFormatType.LITERAL))
                    .build()
                    .apply(AfkLang.PLACEHOLDER_MODE.get(this.isAfk(player)));
        });

        registry.register("afk_idle_time", (player, payload) -> {
            return String.valueOf(this.getIdleTime(player));
        });

        registry.register("afk_idle_time_formatted", (player, payload) -> {
            return TimeFormats.formatAmount(TimeUnit.MILLISECONDS.convert(this.getIdleTime(player), TimeUnit.SECONDS),
                    TimeFormatType.LITERAL);
        });

        registry.register("afk_total_time", (player, payload) -> {
            return String.valueOf(this.getTotalAfkTime(player));
        });

        registry.register("afk_total_time_formatted", (player, payload) -> {
            return TimeFormats.formatAmount(this.getTotalAfkTime(player), TimeFormatType.LITERAL);
        });

        registry.register("afk_count", (player, payload) -> {
            return String.valueOf(this.getAfkCount(player));
        });

        registry.register("afk_enter_timestamp", (player, payload) -> {
            return String.valueOf(this.getAfkEnterTimestamp(player));
        });

        registry.register("afk_enter_time", (player, payload) -> {
            final long entered = this.getAfkEnterTimestamp(player);
            return entered <= 0L ? "0s" : TimeFormats.formatSince(entered, TimeFormatType.LITERAL);
        });
    }

    public AfkSettings getSettings() {
        return this.settings;
    }

    public void kickForIdling(final Player player) {
        final String reason = PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_TIME,
                        () -> TimeFormats.formatAmount(
                                TimeUnit.MILLISECONDS.convert(this.getIdleTime(player), TimeUnit.SECONDS),
                                TimeFormatType.LITERAL))
                .andThen(SLPlaceholders.forPlayerWithPAPI(player))
                .build()
                .apply(String.join("\n", this.settings.kickText.get()));

        if (this.settings.rejoinKickAfter.get() != 0) {
            this.kickedForIdling.put(player.getUniqueId(), System.currentTimeMillis());
        }

        Players.kick(player, reason);
    }

    public boolean isWithinRejoinKickWindow(final Player player) {
        final int window = this.settings.rejoinKickAfter.get();
        if (window == 0)
            return false;

        final Long kickedAt = this.kickedForIdling.remove(player.getUniqueId());
        if (kickedAt == null)
            return false;

        if (window < 0)
            return true;

        return System.currentTimeMillis() - kickedAt <= window * 1000L;
    }

    @Override
    public boolean isAfk(final Player player) {
        return this.activityTracker(player).map(ActivityTracker::isAfk).orElse(false);
    }

    public void track(final Player player) {
        final ActivityTracker tracker = new ActivityTracker(this.settings);
        this.activityTrackerMap.put(player.getUniqueId(), tracker);
    }

    public void untrack(final Player player, final boolean silent) {
        this.exitAfk(player, silent);
        this.activityTrackerMap.remove(player.getUniqueId());
    }

    public void exitAfk(final Player player, final boolean silent) {
        this.activityTracker(player).filter(ActivityTracker::isAfk).ifPresent(tracker -> {
            this.handleAfkExit(player, tracker, silent);
            tracker.resetCounters();
        });
    }

    public void enterAfk(final Player player, final boolean silent) {
        this.activityTracker(player).filter(Predicate.not(ActivityTracker::isAfk)).ifPresent(tracker -> {
            this.handleAfkEnter(player, tracker, silent);
        });
    }

    public void trackActivity(final Player player, final ActivityType type) {
        this.activityTracker(player).ifPresent(tracker -> tracker.countActivity(type));
    }

    public void trackActivity(final Player player, final int amount) {
        this.activityTracker(player).ifPresent(tracker -> tracker.countActivity(amount));
    }

    public boolean isExempt(final Player player) {
        if (this.isExemptWorld(player.getWorld().getName()))
            return true;

        if (!HookId.hasWorldGuard() || !Utils.isInstalled(HookId.WORLD_GUARD))
            return false;
        if (this.settings.exemptRegions.get().isEmpty())
            return false;

        return this.isInExemptRegion(player);
    }

    private boolean isExemptWorld(final String worldName) {
        for (final String world : this.settings.exemptWorlds.get()) {
            if (world.equalsIgnoreCase(worldName))
                return true;
        }
        return false;
    }

    private boolean isInExemptRegion(final Player player) {
        try {
            final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            final ApplicableRegionSet set = container.createQuery()
                    .getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
            for (final ProtectedRegion region : set) {
                for (final String exempt : this.settings.exemptRegions.get()) {
                    if (region.getId().equalsIgnoreCase(exempt))
                        return true;
                }
            }
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
        return false;
    }

    private void tickTrackers() {
        if (this.activityTrackerMap.isEmpty()) return;
        this.pruneKickRecords();

        Map.copyOf(this.activityTrackerMap).forEach((playerId, tracker) -> {
            final Player player = Utils.getPlayer(playerId);
            if (player == null || !player.isOnline())
                return;

            if (this.isExempt(player)) {
                if (tracker.isAfk())
                    this.exitAfk(player, true);
                tracker.resetCounters();
                return;
            }

            final Location location = player.getLocation();
            tracker.updatePosition(BlockPos.from(location), location.getYaw(), location.getPitch());
            tracker.tick();

            if (tracker.isAfk()) {
                if (tracker.isEnoughActivity()) {
                    this.handleAfkExit(player, tracker, false);
                    return;
                }

                final int idleTime = tracker.getIdleTime();
                final int timeToKick = this.getTimeToKick(player);
                if (timeToKick > 0 && idleTime >= timeToKick) {
                    this.kickForIdling(player);
                    return;
                }

                final int afkKickTime = this.getAfkKickTime(player);
                if (afkKickTime > 0 && this.getAfkDuration(player) >= afkKickTime) {
                    this.kickForIdling(player);
                    return;
                }

                this.sendKickWarning(player, tracker, timeToKick, afkKickTime);
                this.sendStatusBar(player, tracker);
            } else {
                final int idleTime = tracker.getIdleTime();
                final int timeToKick = this.getTimeToKick(player);
                if (timeToKick > 0 && idleTime >= timeToKick) {
                    this.kickForIdling(player);
                    return;
                }

                final int timeToAfk = this.getTimeToAfk(player);
                if (timeToAfk > 0 && idleTime >= timeToAfk) {
                    this.handleAfkEnter(player, tracker, false);
                }
            }
        });
    }

    private void pruneKickRecords() {
        final int window = this.settings.rejoinKickAfter.get();

        if (window == 0) {
            this.kickedForIdling.clear();
            return;
        }
        if (window < 0)
            return;

        this.kickedForIdling.values().removeIf(kickedAt -> System.currentTimeMillis() - kickedAt > window * 1000L);
    }

    private void handleChatEvent(final UniversalChatEvent event) {
        if (event.isCancelled())
            return;

        final Player player = event.getPlayer();
        if (this.settings.blockChat.get() && this.isAfk(player)) {
            event.setCancelled(true);
            this.sendPrefixed(AfkLang.ERROR_ACTION_BLOCKED, player);
            return;
        }

        this.trackActivity(player, this.settings.getActivityPoints(ActivityType.CHAT));
    }

    private void handleAfkEnter(final Player player, final ActivityTracker tracker, final boolean silent) {
        tracker.resetCounters();
        tracker.setAfkTimestamp();

        final PlaceholderContext context = PlaceholderContext.builder()
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .andThen(CommonPlaceholders.forPlaceholderAPI(player))
                .build();

        final List<String> commands = context.apply(this.settings.afkCommands.get());
        Players.dispatchCommands(player, commands);

        final PlayerAfkEvent event = new PlayerAfkEvent(player, true);
        this.plugin.getPluginManager().callEvent(event);
        this.plugin.getPluginManager().callEvent(new su.nightexpress.sunlight.api.event.PlayerAfkChangeEvent(player, true));

        this.settings.soundEnter.get().play(player);

        if (!silent)
            this.broadcastPrefixed(AfkLang.AFK_ENTER_BROADCAST, context);
    }

    private void handleAfkExit(final Player player, final ActivityTracker tracker, final boolean silent) {
        final long afkDuration = System.currentTimeMillis() - tracker.getAfkEnterTimestamp();
        if (afkDuration > 0L) {
            this.recordAfkSession(player, afkDuration);
        }

        final PlaceholderContext context = PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_TIME,
                        () -> TimeFormats.formatSince(tracker.getAfkEnterTimestamp(), TimeFormatType.LITERAL))
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .andThen(CommonPlaceholders.forPlaceholderAPI(player))
                .build();

        final List<String> commands = context.apply(this.settings.wakeUpCommands.get());
        Players.dispatchCommands(player, commands);

        final PlayerAfkEvent event = new PlayerAfkEvent(player, false);
        this.plugin.getPluginManager().callEvent(event);
        this.plugin.getPluginManager().callEvent(new su.nightexpress.sunlight.api.event.PlayerAfkChangeEvent(player, false));

        this.settings.soundExit.get().play(player);

        if (!silent)
            this.broadcastPrefixed(AfkLang.AFK_EXIT_BROADCAST, context);

        tracker.resetCounters();
    }

    private void recordAfkSession(final Player player, final long durationMs) {
        final SunUser user = this.userManager.getOrFetch(player);
        user.setProperty(AFK_TOTAL_TIME, user.getPropertyOrDefault(AFK_TOTAL_TIME) + durationMs);
        user.setProperty(AFK_COUNT, user.getPropertyOrDefault(AFK_COUNT) + 1);
    }

    private void sendKickWarning(final Player player, final ActivityTracker tracker, final int timeToKick,
            final int afkKickTime) {
        final int ahead = this.settings.kickWarningAhead.get();
        if (ahead <= 0)
            return;

        final int idleTime = tracker.getIdleTime();
        final long afkDuration = this.getAfkDuration(player);

        int remaining = -1;
        if (timeToKick > 0 && idleTime < timeToKick) {
            remaining = timeToKick - idleTime;
        }
        if (afkKickTime > 0 && afkDuration < afkKickTime) {
            final int afkRemaining = (int) (afkKickTime - afkDuration);
            remaining = remaining < 0 ? afkRemaining : Math.min(remaining, afkRemaining);
        }
        if (remaining <= 0 || remaining > ahead)
            return;

        final long now = System.currentTimeMillis();
        final int interval = this.settings.kickWarningInterval.get();
        if (interval > 0 && now - tracker.getLastKickWarningSent() < interval * 1000L)
            return;

        tracker.setLastKickWarningSent(now);

        final int timeLeft = remaining;
        this.sendPrefixed(AfkLang.KICK_WARNING, player, builder -> builder
                .with(SLPlaceholders.GENERIC_TIME, () -> TimeFormats.formatAmount(
                        TimeUnit.MILLISECONDS.convert(timeLeft, TimeUnit.SECONDS), TimeFormatType.LITERAL)));
    }

    private void sendStatusBar(final Player player, final ActivityTracker tracker) {
        if (tracker.isWakingUp() && this.settings.afkWakeUpBarEnabled.get()) {
            Players.sendActionBar(player, PlaceholderContext.builder()
                    .with(SLPlaceholders.GENERIC_REMAIN, () -> String.valueOf(tracker.getWakeUpProgress()))
                    .with(SLPlaceholders.GENERIC_TIME_LEFT,
                            () -> TimeFormats.formatAmount(
                                    TimeUnit.MILLISECONDS.convert(tracker.getWakeUpTimeLeft(), TimeUnit.SECONDS),
                                    TimeFormatType.LITERAL))
                    .andThen(SLPlaceholders.forPlayerWithPAPI(player))
                    .build()
                    .apply(this.settings.afkWakeUpBarText.get()));
            return;
        }

        if (this.settings.afkStatusBarEnabled.get()) {
            Players.sendActionBar(player, PlaceholderContext.builder()
                    .with(SLPlaceholders.GENERIC_TIME,
                            () -> TimeFormats.formatSince(tracker.getAfkEnterTimestamp(), TimeFormatType.LITERAL))
                    .andThen(SLPlaceholders.forPlayerWithPAPI(player))
                    .build()
                    .apply(this.settings.afkStatusBarText.get()));
        }
    }

    public Set<ActivityTracker> getActivityTrackers() {
        return new HashSet<>(this.activityTrackerMap.values());
    }

    public List<Player> getAfkPlayers() {
        return this.activityTrackerMap.entrySet().stream()
                .filter(entry -> entry.getValue().isAfk())
                .map(entry -> Utils.getPlayer(entry.getKey()))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public ActivityTracker getActivityTracker(final Player player) {
        return this.getActivityTracker(player.getUniqueId());
    }

    public ActivityTracker getActivityTracker(final UUID playerId) {
        return this.activityTrackerMap.get(playerId);
    }

    public Optional<ActivityTracker> activityTracker(final Player player) {
        return Optional.ofNullable(this.getActivityTracker(player));
    }

    public int getTimeToAfk(final Player player) {
        return this.settings.idleAfkTimes.get().getGreatest(player).intValue();
    }

    public int getTimeToKick(final Player player) {
        return this.settings.idleKickTimes.get().getGreatest(player).intValue();
    }

    public int getAfkKickTime(final Player player) {
        return this.settings.afkKickTimes.get().getGreatest(player).intValue();
    }

    public int getIdleTime(final Player player) {
        return this.activityTracker(player).map(ActivityTracker::getIdleTime).orElse(0);
    }

    public long getAfkEnterTimestamp(final Player player) {
        return this.activityTracker(player).map(ActivityTracker::getAfkEnterTimestamp).orElse(0L);
    }

    public long getAfkDuration(final Player player) {
        return this.activityTracker(player)
                .filter(ActivityTracker::isAfk)
                .map(tracker -> TimeUnit.SECONDS.convert(System.currentTimeMillis() - tracker.getAfkEnterTimestamp(),
                        TimeUnit.MILLISECONDS))
                .orElse(0L);
    }

    public long getTotalAfkTime(final Player player) {
        return this.userManager.getOrFetch(player).getPropertyOrDefault(AFK_TOTAL_TIME);
    }

    public int getAfkCount(final Player player) {
        return this.userManager.getOrFetch(player).getPropertyOrDefault(AFK_COUNT);
    }
}