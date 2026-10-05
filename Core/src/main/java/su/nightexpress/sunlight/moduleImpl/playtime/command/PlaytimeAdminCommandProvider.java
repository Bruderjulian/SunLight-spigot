package su.nightexpress.sunlight.moduleImpl.playtime.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeModule;
import su.nightexpress.sunlight.moduleImpl.playtime.config.PlaytimeLang;
import su.nightexpress.sunlight.moduleImpl.playtime.config.PlaytimePerms;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.Utils;

public class PlaytimeAdminCommandProvider extends CommandProvider<PlaytimeModule> {

    private static final String ARG_PERIOD = "period";
    private static final String ARG_MINUTES = "minutes";
    private static final String ARG_SCOPE = "scope";

    private static final String COMMAND_ROOT = "playtime";
    private static final String COMMAND_ADD = "add";
    private static final String COMMAND_SET = "set";
    private static final String COMMAND_RESET = "reset";
    private static final String COMMAND_RELOAD = "reload";

    public PlaytimeAdminCommandProvider(PlaytimeModule module) {
        super(module, "playtime-admin");
    }

    @Override
    public void setup() {
        this.register(COMMAND_ADD, command -> command
            .withFullDescription(PlaytimeLang.COMMAND_ADMIN_ADD_DESC.text())
            .withPermission(PlaytimePerms.COMMAND_ADMIN_ADD)
            .withArguments(this.periodOrAllArgument(), new IntegerArgument(ARG_MINUTES))
            .withOptionalArguments(CommandArgumentConstants.targetArgument())
            .executes(this::addPlaytime))
            .under(COMMAND_ROOT);

        this.register(COMMAND_SET, command -> command
            .withFullDescription(PlaytimeLang.COMMAND_ADMIN_SET_DESC.text())
            .withPermission(PlaytimePerms.COMMAND_ADMIN_SET)
            .withArguments(this.periodOrAllArgument(), new IntegerArgument(ARG_MINUTES))
            .withOptionalArguments(CommandArgumentConstants.targetArgument())
            .executes(this::setPlaytime))
            .under(COMMAND_ROOT);

        this.register(COMMAND_RESET, command -> command
            .withFullDescription(PlaytimeLang.COMMAND_ADMIN_RESET_DESC.text())
            .withPermission(PlaytimePerms.COMMAND_ADMIN_RESET)
            .withArguments(this.resetScopeArgument())
            .withOptionalArguments(CommandArgumentConstants.targetArgument())
            .executes(this::resetPlaytime))
            .under(COMMAND_ROOT);

        this.register(COMMAND_RELOAD, command -> command
            .withFullDescription(PlaytimeLang.COMMAND_ADMIN_RELOAD_DESC.text())
            .withPermission(PlaytimePerms.COMMAND_ADMIN_RELOAD)
            .executes((sender, arguments) -> {
                this.module.reloadModule();
                this.module.sendPrefixed(PlaytimeLang.ADMIN_RELOADED, sender);
                return 1;
            }))
            .under(COMMAND_ROOT);
    }

    private Argument<String> periodOrAllArgument() {
        return CommandArgumentConstants.string(ARG_PERIOD,
            info -> List.of("all", "alltime", "year", "month", "week", "day"));
    }

    private Argument<String> resetScopeArgument() {
        return CommandArgumentConstants.string(ARG_SCOPE,
            info -> List.of("all", "alltime", "year", "month", "week", "day", "session", "streaks", "goals", "milestones"));
    }

    private PlaytimePeriod parsePeriodOrAll(String raw) {
        String normalized = Utils.lowercase(raw == null ? "" : raw.trim());
        if (normalized == null || normalized.equals("all")) return null;
        if (normalized.equals("total")) return PlaytimePeriod.ALLTIME;
        return PlaytimePeriod.fromString(normalized);
    }

    private String periodLabel(PlaytimePeriod period) {
        if (period == null) return "All";
        return period == PlaytimePeriod.ALLTIME ? "Total"
            : period.name().substring(0, 1) + period.name().substring(1).toLowerCase();
    }

    private int addPlaytime(CommandSender sender, CommandArguments arguments) {
        Object periodObj = arguments.get(ARG_PERIOD);
        Object minutesObj = arguments.get(ARG_MINUTES);

        if (!(periodObj instanceof String periodRaw) || !(minutesObj instanceof Integer minutes)) {
            return 0;
        }
        if (minutes <= 0) {
            this.module.sendPrefixed(PlaytimeLang.ERROR_GOAL_NEGATIVE, sender);
            return 0;
        }

        PlaytimePeriod period = this.parsePeriodOrAll(periodRaw);
        String label = this.periodLabel(period);
        long deltaMs = (long) minutes * 60_000L;

        CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_ADMIN_ADD, (user, targetPlayer) -> {
            SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.addPlaytime(resolved, period, deltaMs);
            this.module.sendPrefixed(PlaytimeLang.ADMIN_ADDED, sender, b -> b
                .with(SLPlaceholders.GENERIC_TIME, () -> this.module.format(deltaMs))
                .with(SLPlaceholders.PLAYER_NAME, resolved::getName)
                .with(SLPlaceholders.GENERIC_NAME, () -> label));
        });
    }

    private int setPlaytime(CommandSender sender, CommandArguments arguments) {
        Object periodObj = arguments.get(ARG_PERIOD);
        Object minutesObj = arguments.get(ARG_MINUTES);

        if (!(periodObj instanceof String periodRaw) || !(minutesObj instanceof Integer minutes)) {
            return 0;
        }
        if (minutes < 0) {
            this.module.sendPrefixed(PlaytimeLang.ERROR_GOAL_NEGATIVE, sender);
            return 0;
        }

        PlaytimePeriod period = this.parsePeriodOrAll(periodRaw);
        String label = this.periodLabel(period);
        long valueMs = (long) minutes * 60_000L;

        CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_ADMIN_SET, (user, targetPlayer) -> {
            SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.setPlaytime(resolved, period, valueMs);
            this.module.sendPrefixed(PlaytimeLang.ADMIN_SET, sender, b -> b
                .with(SLPlaceholders.GENERIC_TIME, () -> this.module.format(valueMs))
                .with(SLPlaceholders.PLAYER_NAME, resolved::getName)
                .with(SLPlaceholders.GENERIC_NAME, () -> label));
        });
    }

    private int resetPlaytime(CommandSender sender, CommandArguments arguments) {
        Object scopeObj = arguments.get(ARG_SCOPE);
        if (!(scopeObj instanceof String scopeRaw) || scopeRaw.isBlank()) {
            return 0;
        }
        String scope = Utils.lowercase(scopeRaw.trim());

        CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return target.runAs(this.module, sender, PlaytimePerms.COMMAND_ADMIN_RESET, (user, targetPlayer) -> {
            SunUser resolved = this.module.userManager().getOrFetch(targetPlayer);
            this.module.resetScope(resolved, scope);
            this.module.sendPrefixed(PlaytimeLang.ADMIN_RESET, sender, b -> b
                .with(SLPlaceholders.GENERIC_NAME, () -> scope)
                .with(SLPlaceholders.PLAYER_NAME, resolved::getName));
        });
    }
}
