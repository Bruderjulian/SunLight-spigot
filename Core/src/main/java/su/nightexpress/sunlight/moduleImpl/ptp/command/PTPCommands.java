package su.nightexpress.sunlight.moduleImpl.ptp.command;

import java.util.List;
import java.util.Objects;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.ptp.PTPModule;
import su.nightexpress.sunlight.moduleImpl.ptp.PTPProperties;
import su.nightexpress.sunlight.moduleImpl.ptp.config.PTPLang;
import su.nightexpress.sunlight.moduleImpl.ptp.config.PTPPerms;
import su.nightexpress.sunlight.moduleImpl.ptp.request.TeleportMode;
import su.nightexpress.sunlight.moduleImpl.ptp.request.TeleportRequest;
import su.nightexpress.sunlight.utils.Utils;

public class PTPCommands extends CommandProvider<PTPModule> {

    private static final String COMMAND_ROOT = "ptp";
    private static final String COMMAND_OFF = "off";
    private static final String COMMAND_ON = "on";
    private static final String COMMAND_TOGGLE = "toggle";

    private static final String COMMAND_ACCEPT = "accept";
    private static final String COMMAND_DECLINE = "decline";
    private static final String COMMAND_REQUEST = "request";
    private static final String COMMAND_INVITE = "invite";

    // Standalone names advertised in the request messages, see ACCEPT_NAME / DECLINE_NAME.
    public static final String ACCEPT_NAME = "tpyes";
    public static final String DECLINE_NAME = "tpno";

    public PTPCommands(final PTPModule module) {
        super(module, "ptp");
    }

    @Override
    public void setup() {
        this.register(COMMAND_ACCEPT, command -> {
            command.withFullDescription(PTPLang.COMMAND_ACCEPT_DESC.text())
                    .withPermission(PTPPerms.COMMAND_ACCEPT)
                    .withRequirement(sender -> sender instanceof Player)
                    .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                            info -> this.requestSuggestions(info.sender())))
                    .executes((sender, arguments) -> {
                        return this.acceptOrDecline(sender, arguments, true);
                    });
        })
            .aliases(ACCEPT_NAME)
            .under(COMMAND_ROOT);

        this.register(COMMAND_DECLINE, command -> {
            command.withFullDescription(PTPLang.COMMAND_DECLINE_DESC.text())
                    .withPermission(PTPPerms.COMMAND_DECLINE)
                    .withRequirement(sender -> sender instanceof Player)
                    .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                            info -> this.requestSuggestions(info.sender())))
                    .executes((sender, arguments) -> {
                        return this.acceptOrDecline(sender, arguments, false);
                    });
        })
            .aliases(DECLINE_NAME)
            .under(COMMAND_ROOT);

        this.register(COMMAND_REQUEST, command -> {
            command.withFullDescription(PTPLang.COMMAND_REQUEST_DESC.text())
                    .withPermission(PTPPerms.COMMAND_REQUEST)
                    .withRequirement(sender -> sender instanceof Player)
                    .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                            info -> CommandArgumentConstants.onlinePlayerNames()))
                    .executes((sender, arguments) -> {
                        return this.sendRequest(sender, arguments, TeleportMode.REQUEST);
                    });
        })
            .under(COMMAND_ROOT);

        this.register(COMMAND_INVITE, command -> {
            command.withFullDescription(PTPLang.COMMAND_INVITE_DESC.text())
                    .withPermission(PTPPerms.COMMAND_INVITE)
                    .withRequirement(sender -> sender instanceof Player)
                    .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                            info -> CommandArgumentConstants.onlinePlayerNames()))
                    .executes((sender, arguments) -> {
                        return this.sendRequest(sender, arguments, TeleportMode.INVITE);
                    });
        })
            .under(COMMAND_ROOT);

        this.register(COMMAND_TOGGLE, command -> this.buildRequests(command, ToggleMode.TOGGLE))
            .under(COMMAND_ROOT);
        this.register(COMMAND_ON, command -> this.buildRequests(command, ToggleMode.ON))
            .under(COMMAND_ROOT);
        this.register(COMMAND_OFF, command -> this.buildRequests(command, ToggleMode.OFF))
            .under(COMMAND_ROOT);

        this.registerRoot(COMMAND_ROOT, command -> command
                .withFullDescription(PTPLang.COMMAND_PTP_DESC.text())
                .withPermission(PTPPerms.COMMAND_ROOT));
    }

    private List<String> requestSuggestions(final CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return List.of();
        }
        return this.module.getRequests(player).stream().map(TeleportRequest::getSender).filter(
                Objects::nonNull).map(Player::getName).toList();
    }

    private void buildRequests(final CommandAPICommand command, final ToggleMode mode) {
        final TextLocale description = switch (mode) {
            case TOGGLE -> PTPLang.COMMAND_REQUESTS_TOGGLE_DESC;
            case ON -> PTPLang.COMMAND_REQUESTS_ON_DESC;
            case OFF -> PTPLang.COMMAND_REQUESTS_OFF_DESC;
        };

        command.withFullDescription(description.text())
                .withPermission(PTPPerms.COMMAND_REQUESTS)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                        return this.toggleRequests(sender, arguments, mode);
                    });
    }

    private int acceptOrDecline(final CommandSender sender, final CommandArguments arguments,
            final boolean accept) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object fromObj = arguments.get(CommandArgumentConstants.PLAYER);
        final String from = fromObj instanceof final String value ? value : null;

        return (accept ? this.module.accept(player, from) : this.module.decline(player, from)) ? 1 : 0;
    }

    private int sendRequest(final CommandSender sender, final CommandArguments arguments,
            final TeleportMode mode) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final Object targetObj = arguments.get(CommandArgumentConstants.PLAYER);
        if (!(targetObj instanceof final String targetName)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        final Player target = Utils.getPlayer(targetName);
        if (target == null || !this.canSee(sender, target)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        if (player == target) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_NOT_YOURSELF, sender);
            return 0;
        }

        return this.module.sendRequest(player, target, mode) ? 1 : 0;
    }

    private int toggleRequests(final CommandSender sender, final CommandArguments arguments,
            final ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PTPPerms.COMMAND_REQUESTS_OTHERS, (user, targetPlayer) -> {
            final boolean state = mode.apply(user.getPropertyOrDefault(PTPProperties.TELEPORT_REQUESTS));
            user.setProperty(PTPProperties.TELEPORT_REQUESTS, state);
            user.markDirty();

            if (sender != targetPlayer) {
                this.module.sendPrefixed(PTPLang.REQUESTS_TOGGLE_FEEDBACK, sender, replacer -> replacer
                        .with(CommonPlaceholders.PLAYER.resolver(targetPlayer))
                        .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(PTPLang.REQUESTS_TOGGLE_NOTIFY, targetPlayer, replacer -> replacer
                        .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
            }
        });
    }
}
