package su.nightexpress.sunlight.moduleImpl.playtime.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeModule;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeProperties;
import su.nightexpress.sunlight.moduleImpl.playtime.config.PlaytimeLang;
import su.nightexpress.sunlight.moduleImpl.playtime.config.PlaytimePerms;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.Utils;

public class PlaytimeCommandProvider extends CommandProvider<PlaytimeModule> {

    public PlaytimeCommandProvider(final PlaytimeModule module) {
        super(module, "playtime");
    }

    @Override
    public void setup() {
        this.register("stats", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_STATS_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_STATS)
                .withArguments(CommandArgumentConstants.periodArgument())
                .withOptionalArguments(
                        CommandArgumentConstants.targetArgument().withPermission(PlaytimePerms.COMMAND_STATS_OTHERS))
                .executes(this::showStats))
                .under("playtime");

        this.register("top", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_TOP_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_TOP)
                .withOptionalArguments(
                        CommandArgumentConstants.periodArgument(),
                        new IntegerArgument("page"))
                .executes(this::showTop))
                .aliases("playtop")
                .under("playtime");

        this.register("goal", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_GOAL_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_GOAL)
                .withOptionalArguments(
                        CommandArgumentConstants.periodArgument(),
                        new IntegerArgument("minutes"))
                .executes(this::goal))
                .under("playtime");

        this.register("menu", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_MENU_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_MENU)
                .withOptionalArguments(
                        CommandArgumentConstants.targetArgument().withPermission(PlaytimePerms.COMMAND_MENU_OTHERS))
                .executes(this::showMenu))
                .aliases("gui")
                .under("playtime");

        this.register("add", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_ADMIN_ADD_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_ADMIN_ADD)
                .withArguments(
                        CommandArgumentConstants.periodArgument(),
                        new IntegerArgument("minutes"))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::addPlaytime))
                .under("playtime");

        this.register("set", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_ADMIN_SET_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_ADMIN_SET)
                .withArguments(
                        CommandArgumentConstants.periodArgument(),
                        new IntegerArgument("minutes"))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::setPlaytime))
                .under("playtime");

        this.register("reset", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_ADMIN_RESET_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_ADMIN_RESET)
                .withArguments(this.resetScopeArgument())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::resetPlaytime))
                .under("playtime");

        this.register("reload", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_ADMIN_RELOAD_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_ADMIN_RELOAD)
                .executes((sender, arguments) -> {
                    this.module.reloadModule();
                    this.module.sendPrefixed(PlaytimeLang.ADMIN_RELOADED, sender);
                    return 1;
                }))
                .under("playtime");

        this.registerRoot("playtime", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_PLAYTIME_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_PLAYTIME)
                .withOptionalArguments(
                        CommandArgumentConstants.targetArgument().withPermission(PlaytimePerms.COMMAND_STATS_OTHERS))
                .executes(this::showOwnStats));
    }

    private Argument<String> resetScopeArgument() {
        return CommandArgumentConstants.string(
                "scope",
                info -> List.of("all", "alltime", "year", "month", "week", "day", "session", "streaks", "goals",
                        "milestones"));
    }

    private int showOwnStats(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        // Bare /playtime opens the menu for players checking themselves.
        if (!target.hasTarget() && sender instanceof final Player player
                && sender.hasPermission(PlaytimePerms.COMMAND_MENU)) {
            final SunUser self = this.module.userManager().getOrFetch(player);
            return this.module.openStatsMenu(player, self) ? 1 : 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_STATS_OTHERS, (user, targetPlayer) -> {
            final SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.showStats(sender, resolved);
        });
    }

    private int showStats(final CommandSender sender, final CommandArguments arguments) {
        final PlaytimePeriod period = arguments.getOrDefaultUnchecked("period", PlaytimePeriod.ALLTIME);

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_STATS_OTHERS, (user, targetPlayer) -> {
            final SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.showSingleStat(sender, resolved, period);
        });
    }

    private int showTop(final CommandSender sender, final CommandArguments arguments) {
        final PlaytimePeriod period = arguments.getOrDefaultUnchecked("period", PlaytimePeriod.ALLTIME);
        final Object pageObj = arguments.get("page");
        int page = 1;
        if (pageObj instanceof final Integer value) {
            page = value;
        }

        if (sender instanceof final Player player) {
            return this.module.openTopMenu(player, period) ? 1 : 0;
        }
        this.module.showTop(sender, period, page);
        return 1;
    }

    private int showMenu(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_MENU_OTHERS, (user, targetPlayer) -> {
            final SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.openStatsMenu(player, resolved);
        });
    }

    private int goal(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final PlaytimePeriod period = arguments.getUnchecked("period");
        final Object minutesObj = arguments.get("minutes");

        final SunUser user = this.module.userManager().getOrFetch(player);

        if (period == null) {
            this.module.showGoals(sender, user);
            return 1;
        }

        if (!(minutesObj instanceof final Integer minutes)) {
            this.module.showGoals(sender, user);
            return 1;
        }

        if (!sender.hasPermission(PlaytimePerms.COMMAND_GOAL_SET)) {
            this.module.sendPrefixed(CoreLang.ERROR_NO_PERMISSION, sender);
            return 0;
        }

        if (minutes < 0) {
            this.module.sendPrefixed(PlaytimeLang.ERROR_GOAL_NEGATIVE, sender);
            return 0;
        }

        if (minutes == 0) {
            if (!this.isGoalPeriod(period)) {
                this.module.showGoals(sender, user);
                return 0;
            }
            this.setGoalProperty(user, period, 0L);
            this.module.sendPrefixed(PlaytimeLang.GOAL_RESET, sender, b -> b
                    .with(SLPlaceholders.GENERIC_NAME, () -> period.name()));
            return 1;
        }

        final int min = this.module.getSettings().getGoalMinMinutes();
        final int max = this.module.getSettings().getGoalMaxMinutes();
        final boolean belowMin = min > 0 && minutes < min;
        final boolean aboveMax = max > 0 && minutes > max;
        if (belowMin || aboveMax) {
            this.module.sendPrefixed(PlaytimeLang.GOAL_ERROR_RANGE, sender, b -> b
                    .with(SLPlaceholders.GENERIC_MIN, () -> String.valueOf(Math.max(0, min)))
                    .with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(Math.max(0, max))));
            return 0;
        }

        final long ms = (long) minutes * 60_000L;
        if (!this.isGoalPeriod(period) || !this.setGoalProperty(user, period, ms)) {
            this.module.showGoals(sender, user);
            return 0;
        }

        this.module.sendPrefixed(PlaytimeLang.GOAL_SET, sender, b -> b
                .with(SLPlaceholders.GENERIC_NAME, () -> period.name())
                .with(SLPlaceholders.GENERIC_TIME, () -> this.module.format(ms)));
        return 1;
    }

    private boolean isGoalPeriod(final PlaytimePeriod period) {
        return period == PlaytimePeriod.DAY || period == PlaytimePeriod.WEEK || period == PlaytimePeriod.MONTH;
    }

    private boolean setGoalProperty(final SunUser user, final PlaytimePeriod period, final long ms) {
        switch (period) {
            case DAY -> user.setProperty(PlaytimeProperties.GOAL_DAY, ms);
            case WEEK -> user.setProperty(PlaytimeProperties.GOAL_WEEK, ms);
            case MONTH -> user.setProperty(PlaytimeProperties.GOAL_MONTH, ms);
            default -> {
                return false;
            }
        }
        return true;
    }

    private int addPlaytime(final CommandSender sender, final CommandArguments arguments) {
        final PlaytimePeriod period = arguments.getOrDefaultUnchecked("period", PlaytimePeriod.ALLTIME);
        final Object minutesObj = arguments.get("minutes");

        if (!(minutesObj instanceof final Integer minutes)) {
            return 0;
        }
        if (minutes <= 0) {
            this.module.sendPrefixed(PlaytimeLang.ERROR_GOAL_NEGATIVE, sender);
            return 0;
        }
        final long deltaMs = (long) minutes * 60_000L;

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_ADMIN_ADD, (user, targetPlayer) -> {
            final SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.addPlaytime(resolved, period, deltaMs);
            this.module.sendPrefixed(PlaytimeLang.ADMIN_ADDED, sender, b -> b
                    .with(SLPlaceholders.GENERIC_TIME, () -> this.module.format(deltaMs))
                    .with(SLPlaceholders.PLAYER_NAME, resolved::getName)
                    .with(SLPlaceholders.GENERIC_NAME, () -> period.name()));
        });
    }

    private int setPlaytime(final CommandSender sender, final CommandArguments arguments) {
        final PlaytimePeriod period = arguments.getOrDefaultUnchecked("period", PlaytimePeriod.ALLTIME);
        final Object minutesObj = arguments.get("minutes");

        if (!(minutesObj instanceof final Integer minutes)) {
            return 0;
        }
        if (minutes < 0) {
            this.module.sendPrefixed(PlaytimeLang.ERROR_GOAL_NEGATIVE, sender);
            return 0;
        }

        final long valueMs = (long) minutes * 60_000L;

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_ADMIN_SET, (user, targetPlayer) -> {
            final SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.setPlaytime(resolved, period, valueMs);
            this.module.sendPrefixed(PlaytimeLang.ADMIN_SET, sender, b -> b
                    .with(SLPlaceholders.GENERIC_TIME, () -> this.module.format(valueMs))
                    .with(SLPlaceholders.PLAYER_NAME, resolved::getName)
                    .with(SLPlaceholders.GENERIC_NAME, () -> period.name()));
        });
    }

    private int resetPlaytime(final CommandSender sender, final CommandArguments arguments) {
        final Object scopeObj = arguments.get("scope");
        if (!(scopeObj instanceof final String scopeRaw) || scopeRaw.isBlank()) {
            return 0;
        }
        final String scope = Utils.lowercase(scopeRaw.trim());

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_ADMIN_RESET, (user, targetPlayer) -> {
            final SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.resetScope(resolved, scope);
            this.module.sendPrefixed(PlaytimeLang.ADMIN_RESET, sender, b -> b
                    .with(SLPlaceholders.GENERIC_NAME, () -> scope)
                    .with(SLPlaceholders.PLAYER_NAME, resolved::getName));
        });
    }
}
