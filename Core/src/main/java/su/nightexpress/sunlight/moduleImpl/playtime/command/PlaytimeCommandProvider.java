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

    private static final String ARG_PERIOD = "period";
    private static final String ARG_PAGE = "page";
    private static final String ARG_MINUTES = "minutes";

    public PlaytimeCommandProvider(PlaytimeModule module) {
        super(module, "playtime");
    }

    @Override
    public void setup() {
        this.register("stats", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_STATS_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_STATS)
                .withArguments(this.periodArgument())
                .withOptionalArguments(
                        CommandArgumentConstants.targetArgument().withPermission(PlaytimePerms.COMMAND_STATS_OTHERS))
                .executes(this::showStats))
                .under("playtime");

        this.register("top", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_TOP_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_TOP)
                .withOptionalArguments(this.periodArgument(), new IntegerArgument(ARG_PAGE))
                .executes(this::showTop))
                .aliases("playtop")
                .under("playtime");

        this.register("goal", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_GOAL_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_GOAL)
                .withOptionalArguments(this.goalPeriodArgument(), new IntegerArgument(ARG_MINUTES))
                .executes(this::goal))
                .under("playtime");

        this.registerRoot("playtime", command -> command
                .withFullDescription(PlaytimeLang.COMMAND_PLAYTIME_DESC.text())
                .withPermission(PlaytimePerms.COMMAND_PLAYTIME)
                .withOptionalArguments(CommandArgumentConstants.targetArgument()
                        .withPermission(PlaytimePerms.COMMAND_STATS_OTHERS))
                .executes(this::showOwnStats));
    }

    private Argument<String> periodArgument() {
        return CommandArgumentConstants.string(ARG_PERIOD, info -> Utils.getEnumNames(PlaytimePeriod.class));
    }

    private Argument<String> goalPeriodArgument() {
        return CommandArgumentConstants.string(ARG_PERIOD, info -> List.of("day", "week", "month"));
    }

    private int showOwnStats(CommandSender sender, CommandArguments arguments) {
        CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_STATS_OTHERS, (user, targetPlayer) -> {
            SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.showStats(sender, resolved);
        });
    }

    private int showStats(CommandSender sender, CommandArguments arguments) {
        Object periodObj = arguments.get(ARG_PERIOD);
        String raw = periodObj instanceof String value ? value : "alltime";
        PlaytimePeriod period = PlaytimePeriod.fromString(raw);

        CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_STATS_OTHERS, (user, targetPlayer) -> {
            SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.showSingleStat(sender, resolved, period);
        });
    }

    private int showTop(CommandSender sender, CommandArguments arguments) {
        Object periodObj = arguments.get(ARG_PERIOD);
        Object pageObj = arguments.get(ARG_PAGE);

        PlaytimePeriod period = PlaytimePeriod.ALLTIME;
        int page = 1;

        if (periodObj instanceof String raw && !raw.isBlank()) {
            try {
                page = Math.max(1, Integer.parseInt(raw.trim()));
            } catch (NumberFormatException exception) {
                period = PlaytimePeriod.fromString(raw);
            }
        }
        if (pageObj instanceof Integer value) {
            page = value;
        }

        this.module.showTop(sender, period, page);
        return 1;
    }

    private int goal(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        Object periodObj = arguments.get(ARG_PERIOD);
        Object minutesObj = arguments.get(ARG_MINUTES);

        SunUser user = this.module.userManager().getOrFetch(player);

        if (!(periodObj instanceof String periodRaw) || periodRaw.isBlank()) {
            this.module.showGoals(sender, user);
            return 1;
        }

        String period = Utils.lowercase(periodRaw.trim());
        if (!(minutesObj instanceof Integer minutes)) {
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
            String label = this.goalLabel(period);
            if (label == null) {
                this.module.showGoals(sender, user);
                return 0;
            }
            this.setGoalProperty(user, period, 0L);
            this.module.sendPrefixed(PlaytimeLang.GOAL_RESET, sender, b -> b
                    .with(SLPlaceholders.GENERIC_NAME, () -> label));
            return 1;
        }

        int min = this.module.getSettings().getGoalMinMinutes();
        int max = this.module.getSettings().getGoalMaxMinutes();
        boolean belowMin = min > 0 && minutes < min;
        boolean aboveMax = max > 0 && minutes > max;
        if (belowMin || aboveMax) {
            this.module.sendPrefixed(PlaytimeLang.GOAL_ERROR_RANGE, sender, b -> b
                    .with(SLPlaceholders.GENERIC_MIN, () -> String.valueOf(Math.max(0, min)))
                    .with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(Math.max(0, max))));
            return 0;
        }

        long ms = (long) minutes * 60_000L;
        String label = this.goalLabel(period);
        if (label == null || !this.setGoalProperty(user, period, ms)) {
            this.module.showGoals(sender, user);
            return 0;
        }

        String finalLabel = label;
        this.module.sendPrefixed(PlaytimeLang.GOAL_SET, sender, b -> b
                .with(SLPlaceholders.GENERIC_NAME, () -> finalLabel)
                .with(SLPlaceholders.GENERIC_TIME, () -> this.module.format(ms)));
        return 1;
    }

    private String goalLabel(String period) {
        return switch (period) {
            case "day", "daily" -> "Daily";
            case "week", "weekly" -> "Weekly";
            case "month", "monthly" -> "Monthly";
            default -> null;
        };
    }

    private boolean setGoalProperty(SunUser user, String period, long ms) {
        switch (period) {
            case "day", "daily" -> user.setProperty(PlaytimeProperties.GOAL_DAY, ms);
            case "week", "weekly" -> user.setProperty(PlaytimeProperties.GOAL_WEEK, ms);
            case "month", "monthly" -> user.setProperty(PlaytimeProperties.GOAL_MONTH, ms);
            default -> {
                return false;
            }
        }
        return true;
    }
}
