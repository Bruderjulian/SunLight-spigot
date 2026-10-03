package su.nightexpress.sunlight.moduleImpl.warps.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.warps.Warp;
import su.nightexpress.sunlight.moduleImpl.warps.WarpsModule;
import su.nightexpress.sunlight.moduleImpl.warps.core.WarpsLang;
import su.nightexpress.sunlight.moduleImpl.warps.core.WarpsPerms;
import su.nightexpress.sunlight.utils.Utils;

public class WarpsCommandProvider extends CommandProvider<WarpsModule> {

    private static final String ARGUMENT_WARP = "warp";

    private static final String COMMAND_CREATE = "create";
    private static final String COMMAND_UPDATE = "update";
    private static final String COMMAND_DELETE = "delete";
    private static final String COMMAND_MENU = "menu";
    private static final String COMMAND_JUMP = "jump";
    private static final String COMMAND_EDIT = "edit";

    public WarpsCommandProvider(final WarpsModule module) {
        super(module, "warps");
    }

    @Override
    public void setup() {
        this.register(COMMAND_CREATE, List.of(), command -> command
                .withFullDescription(WarpsLang.COMMAND_WARPS_CREATE_DESC.text())
                .withPermission(WarpsPerms.COMMAND_WARPS_CREATE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(new StringArgument(CommandArgumentConstants.NAME))
                .executes(this::createWarp));

        this.register(COMMAND_UPDATE, List.of(), command -> command
                .withFullDescription(WarpsLang.COMMAND_WARPS_UPDATE_DESC.text())
                .withPermission(WarpsPerms.COMMAND_WARPS_UPDATE.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(ARGUMENT_WARP,
                        info -> this.module.getWarps().stream().map(Warp::getId).toList()))
                .executes(this::updateWarp));

        this.register(COMMAND_DELETE, List.of(), command -> command
                .withFullDescription(WarpsLang.COMMAND_WARPS_DELETE_DESC.text())
                .withPermission(WarpsPerms.COMMAND_WARPS_DELETE.getName())
                .withArguments(CommandArgumentConstants.string(ARGUMENT_WARP,
                        info -> this.module.getWarps().stream().map(Warp::getId).toList()))
                .executes(this::deleteWarp));

        this.register(COMMAND_EDIT, List.of(), command -> command
                .withFullDescription(WarpsLang.COMMAND_WARPS_EDIT_DESC.text())
                .withPermission(WarpsPerms.COMMAND_WARPS_EDIT.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(ARGUMENT_WARP,
                        info -> this.module.getWarps().stream().map(Warp::getId).toList()))
                .executes(this::editWarp));

        this.register(COMMAND_MENU, List.of(), command -> command
                .withFullDescription(WarpsLang.COMMAND_WARPS_LIST_DESC.text())
                .withPermission(WarpsPerms.COMMAND_WARPS_LIST.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::openWarps));

        this.register(COMMAND_JUMP, List.of(), command -> command
                .withFullDescription(WarpsLang.COMMAND_WARPS_JUMP_DESC.text())
                .withPermission(WarpsPerms.COMMAND_WARPS_JUMP.getName())
                .withArguments(CommandArgumentConstants.string(ARGUMENT_WARP,
                        info -> this.jumpSuggestions(info.sender())))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::jumpToWarp));

        this.registerRoot("warps", command -> command
                .withFullDescription(WarpsLang.COMMAND_WARPS_ROOT_DESC.text())
                .withPermission(WarpsPerms.COMMAND_WARPS_ROOT.getName()));
    }

    private List<String> jumpSuggestions(final CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return this.module.getWarps().stream().map(Warp::getId).toList();
        }
        return this.module.getAvailableWarps(player).stream().map(Warp::getId).toList();
    }

    private Warp getWarp(final CommandArguments arguments, final CommandSender sender) {
        final Object warpObj = arguments.get(ARGUMENT_WARP);
        final Warp warp = warpObj instanceof final String warpId ? this.module.getWarpById(warpId) : null;
        if (warp == null) {
            this.module.sendPrefixed(WarpsLang.COMMAND_SYNTAX_INVALID_WARP, sender);
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
        final Warp warp = this.getWarp(arguments, sender);
        if (warp == null) {
            return 0;
        }

        return this.module.updateWarp(player, warp) ? 1 : 0;
    }

    private int deleteWarp(final CommandSender sender, final CommandArguments arguments) {
        final Warp warp = this.getWarp(arguments, sender);
        if (warp == null) {
            return 0;
        }

        return this.module.removeWarp(sender, warp, false) ? 1 : 0;
    }

    private int editWarp(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        final Warp warp = this.getWarp(arguments, sender);
        if (warp == null) {
            return 0;
        }

        return this.module.openWarpSettings(player, warp) ? 1 : 0;
    }

    private int openWarps(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, WarpsPerms.COMMAND_WARPS_LIST_OTHERS.getName(),
                (user, targetPlayer) -> {
                    if (!targetPlayer.isOnline()) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return;
                    }
                    if (!this.module.openWarpsMenu(targetPlayer)) {
                        return;
                    }
                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(WarpsLang.BROWSER_FEEDBACK, sender, replacer -> replacer.with(
                                CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                    }
                });
    }

    private int jumpToWarp(final CommandSender sender, final CommandArguments arguments) {
        final Warp warp = this.getWarp(arguments, sender);
        if (warp == null || !warp.isActive()) {
            if (warp != null) {
                this.module.sendPrefixed(WarpsLang.COMMAND_SYNTAX_INVALID_WARP, sender);
            }
            return 0;
        }

        final TargetWithForce parsed = TargetWithForce.parse(arguments);
        if (parsed == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final boolean force = parsed.force();
        return parsed.target().runAs(this.module, sender, WarpsPerms.COMMAND_WARPS_JUMP_OTHERS.getName(),
                (user, targetPlayer) -> {
                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(WarpsLang.WARP_TELEPORT_FEEDBACK, sender, replacer -> replacer
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
