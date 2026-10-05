package su.nightexpress.sunlight.moduleImpl.playerwarps.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.playerwarps.PlayerWarp;
import su.nightexpress.sunlight.moduleImpl.playerwarps.PlayerWarpsModule;
import su.nightexpress.sunlight.moduleImpl.playerwarps.core.PlayerWarpsLang;
import su.nightexpress.sunlight.moduleImpl.playerwarps.core.PlayerWarpsPerms;
import su.nightexpress.sunlight.utils.Utils;

public class PlayerWarpsCommands extends CommandProvider<PlayerWarpsModule> {

    private static final String ARGUMENT_WARP = "warp";

    private static final String COMMAND_CREATE = "create";
    private static final String COMMAND_UPDATE = "update";
    private static final String COMMAND_DELETE = "delete";
    private static final String COMMAND_MENU = "menu";
    private static final String COMMAND_JUMP = "teleport";

    public PlayerWarpsCommands(final PlayerWarpsModule module) {
        super(module, "playerwarps");
    }

    @Override
    public void setup() {
        this.register(COMMAND_CREATE, command -> command
                .withFullDescription(PlayerWarpsLang.COMMAND_WARPS_CREATE_DESC.text())
                .withPermission(PlayerWarpsPerms.COMMAND_WARPS_CREATE)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(new StringArgument(CommandArgumentConstants.NAME))
                .executes(this::createWarp))
            .under("playerwarps");

        this.register(COMMAND_UPDATE, command -> command
                .withFullDescription(PlayerWarpsLang.COMMAND_WARPS_UPDATE_DESC.text())
                .withPermission(PlayerWarpsPerms.COMMAND_WARPS_UPDATE)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(ARGUMENT_WARP,
                        info -> this.editableWarpSuggestions(info.sender())))
                .executes(this::updateWarp))
            .under("playerwarps");

        this.register(COMMAND_DELETE, command -> command
                .withFullDescription(PlayerWarpsLang.COMMAND_WARPS_DELETE_DESC.text())
                .withPermission(PlayerWarpsPerms.COMMAND_WARPS_DELETE)
                .withArguments(CommandArgumentConstants.string(ARGUMENT_WARP,
                        info -> this.editableWarpSuggestions(info.sender())))
                .executes(this::deleteWarp))
            .under("playerwarps");

        this.register(COMMAND_JUMP, command -> command
                .withFullDescription(PlayerWarpsLang.COMMAND_WARPS_JUMP_DESC.text())
                .withPermission(PlayerWarpsPerms.COMMAND_WARPS_JUMP)
                .withArguments(CommandArgumentConstants.string(ARGUMENT_WARP,
                        info -> this.usableWarpSuggestions(info.sender())))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::moveToWarp))
            .under("playerwarps");

        this.register(COMMAND_MENU, command -> command
                .withFullDescription(PlayerWarpsLang.COMMAND_WARPS_LIST_DESC.text())
                .withPermission(PlayerWarpsPerms.COMMAND_WARPS_LIST)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::openWarps))
            .under("playerwarps");

        this.registerRoot("playerwarps", command -> command
                .withFullDescription(PlayerWarpsLang.COMMAND_WARPS_ROOT_DESC.text())
                .withPermission(PlayerWarpsPerms.COMMAND_WARPS_ROOT)).aliases("pwarp", "pw");
    }

    private List<String> editableWarpSuggestions(final CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return this.module.getRepository().stream().map(PlayerWarp::getId).toList();
        }
        return this.module.getRepository().stream()
                .filter(warp -> warp.canEdit(player))
                .map(PlayerWarp::getId)
                .toList();
    }

    private List<String> usableWarpSuggestions(final CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return this.module.getRepository().stream()
                    .filter(PlayerWarp::isActive)
                    .map(PlayerWarp::getId)
                    .toList();
        }
        return this.module.getRepository().stream()
                .filter(warp -> warp.isActive() && warp.canUse(player))
                .map(PlayerWarp::getId)
                .toList();
    }

    private PlayerWarp getWarp(final CommandArguments arguments, final CommandSender sender) {
        final Object warpObj = arguments.get(ARGUMENT_WARP);
        final PlayerWarp warp = warpObj instanceof final String warpId
                ? this.module.getRepository().getById(warpId)
                : null;
        if (warp == null) {
            this.module.sendPrefixed(PlayerWarpsLang.COMMAND_SYNTAX_INVALID_WARP, sender);
            return null;
        }
        return warp;
    }

    private int createWarp(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Object nameObj = arguments.get(CommandArgumentConstants.NAME);
        if (!(nameObj instanceof final String name)) {
            return 0;
        }

        return this.module.create(player, name, false) ? 1 : 0;
    }

    private int updateWarp(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final PlayerWarp warp = this.getWarp(arguments, sender);
        if (warp == null) {
            return 0;
        }

        return this.module.updateWarp(player, warp, false) ? 1 : 0;
    }

    private int deleteWarp(final CommandSender sender, final CommandArguments arguments) {
        final PlayerWarp warp = this.getWarp(arguments, sender);
        if (warp == null) {
            return 0;
        }

        return this.module.removeWarp(sender, warp, false) ? 1 : 0;
    }

    private int openWarps(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PlayerWarpsPerms.COMMAND_WARPS_LIST_OTHERS,
                (user, targetPlayer) -> {
                    if (!targetPlayer.isOnline()) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return;
                    }
                    this.module.openWarpsMenu(targetPlayer);

                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(PlayerWarpsLang.WARP_LIST_OPEN_FEEDBACK, sender,
                                replacer -> replacer.with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                    }
                });
    }

    private int moveToWarp(final CommandSender sender, final CommandArguments arguments) {
        final PlayerWarp warp = this.getWarp(arguments, sender);
        if (warp == null || !warp.isActive()) {
            if (warp != null) {
                this.module.sendPrefixed(PlayerWarpsLang.COMMAND_SYNTAX_INVALID_WARP, sender);
            }
            return 0;
        }

        final TargetWithForce parsed = TargetWithForce.parse(arguments);
        if (parsed == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final boolean force = parsed.force();
        return parsed.target().runAs(this.module, sender, PlayerWarpsPerms.COMMAND_WARPS_JUMP_OTHERS,
                (user, targetPlayer) -> {
                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(PlayerWarpsLang.WARP_JUMP_FEEDBACK, sender, replacer -> replacer
                                .with(CommonPlaceholders.PLAYER.resolver(targetPlayer))
                                .with(warp.placeholders()));
                    }

                    this.module.teleportToWarp(warp, targetPlayer, force);
                });
    }

    private record TargetWithForce(CommandArgumentConstants.Target target, boolean force) {

        static TargetWithForce parse(final CommandArguments arguments) {
            final Object rawObj = arguments.get(CommandArgumentConstants.TARGET);
            if (!(rawObj instanceof final String raw) || raw.isBlank()) {
                return new TargetWithForce(new CommandArgumentConstants.Target(null, false), false);
            }

            String playerName = null;
            boolean silent = false;
            boolean force = false;
            for (final String token : raw.trim().split("\\s+")) {
                if (token.isBlank()) {
                    continue;
                }
                final String lower = Utils.lowercase(token);
                if (lower.equals(CommandArgumentConstants.FLAG_SILENT)
                        || lower.equals(CommandArgumentConstants.FLAG_SILENT_LONG)) {
                    silent = true;
                    continue;
                }
                if (lower.equals(CommandArgumentConstants.FLAG_FORCE)) {
                    force = true;
                    continue;
                }
                if (playerName != null) {
                    return null;
                }
                playerName = token;
            }
            return new TargetWithForce(new CommandArgumentConstants.Target(playerName, silent), force);
        }
    }
}
