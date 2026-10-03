package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.WorldArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.teleport.TeleportContext;
import su.nightexpress.sunlight.teleport.TeleportFlag;
import su.nightexpress.sunlight.teleport.TeleportType;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class TeleportCommandsProvider extends CommandProvider<EssentialModule> {

    // TODO Split

    private static final String COMMAND_LOCATION = "tppos";
    private static final String COMMAND_MOVE = "tpplayer";
    private static final String COMMAND_BRING = "tphere";
    private static final String COMMAND_GO_TO = "goto";
    private static final String COMMAND_TOP = "tptop";

    private static final Permission PERMISSION_LOCATION = EssentialPerms.COMMAND.permission("coords");
    private static final Permission PERMISSION_LOCATION_OTHERS = EssentialPerms.COMMAND.permission("coords.others");
    private static final Permission PERMISSION_BRING = EssentialPerms.COMMAND.permission("bring");
    private static final Permission PERMISSION_GOTO = EssentialPerms.COMMAND.permission("goto");
    private static final Permission PERMISSION_MOVE = EssentialPerms.COMMAND.permission("move");
    private static final Permission PERMISSION_SURFACE = EssentialPerms.COMMAND.permission("surface");
    private static final Permission PERMISSION_SURFACE_OTHERS = EssentialPerms.COMMAND.permission("surface.others");

    private static final TextLocale DESCRIPTION_LOCATION = LangEntry.builder("Command.Teleport.Location.Desc").text(
            "Teleport to specific position.");
    private static final TextLocale DESCRIPTION_SUMMON = LangEntry.builder("Command.Teleport.Summon.Desc").text(
            "Teleport player to your location.");
    private static final TextLocale DESCRIPTION_TO = LangEntry.builder("Command.Teleport.To.Desc").text(
            "Teleport to a player's location.");
    private static final TextLocale DESCRIPTION_SEND = LangEntry.builder("Command.Teleport.Send.Desc").text(
            "Teleport one player to another.");
    private static final TextLocale DESCRIPTION_TOP = LangEntry.builder("Command.Teleport.Top.Desc").text(
            "Teleport to the highest block above you.");

    private static final MessageLocale MESSAGE_COORDS_FEEDBACK = LangEntry.builder(
            "Command.Teleport.Location.Done.Target").chatMessage(
            Sound.ENTITY_ENDERMAN_TELEPORT,
            GRAY.wrap("You have teleported " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                    + " teleported to " + RED.wrap(
                    LOCATION_X)
                    + " " + GREEN.wrap(LOCATION_Y) + " " + BLUE.wrap(LOCATION_Z)
                    + " @ "
                    + WHITE.wrap(
                    LOCATION_WORLD)
                    + "."));

    private static final MessageLocale MESSAGE_COORDS_NOTIFY = LangEntry.builder(
            "Command.Teleport.Location.Done.Notify").chatMessage(
            Sound.ENTITY_ENDERMAN_TELEPORT,
            GRAY.wrap("You have been teleported to " + RED.wrap(LOCATION_X) + " "
                    + GREEN.wrap(LOCATION_Y) + " "
                    + BLUE
                    .wrap(LOCATION_Z)
                    + " @ " + WHITE.wrap(LOCATION_WORLD) + "."));

    private static final MessageLocale MESSAGE_SUMMON_YOURSELF = LangEntry
            .builder("Command.Teleport.Summon.Yourself")
            .chatMessage(
                    Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("You can not summon " + RED.wrap("yourself") + "."));

    private static final MessageLocale MESSAGE_SUMMON_FEEDBACK = LangEntry.builder("Command.Teleport.Summon.Target")
            .chatMessage(
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    GRAY.wrap("You have summonned " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "."));

    private static final MessageLocale MESSAGE_SUMMON_NOTIFY = LangEntry.builder("Command.Teleport.Summon.Notify")
            .chatMessage(
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    GRAY.wrap("You have been summoned by " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                            + "."));

    private static final MessageLocale MESSAGE_TO_YOURSELF = LangEntry.builder("Command.Teleport.To.Yourself")
            .chatMessage(
                    Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("You can not teleport to " + RED.wrap("yourself") + "."));

    private static final MessageLocale MESSAGE_TO_DONE = LangEntry.builder("Command.Teleport.To.Done").chatMessage(
            Sound.ENTITY_ENDERMAN_TELEPORT,
            GRAY.wrap("You have been teleported to " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "."));

    private static final MessageLocale MESSAGE_MOVE_FEEDBACK = LangEntry.builder("Command.Teleport.Send.Target")
            .chatMessage(
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    GRAY.wrap("You have teleported " + RED.wrap(GENERIC_SOURCE) + " to "
                            + BLUE.wrap(GENERIC_TARGET)
                            + "."));

    private static final MessageLocale MESSAGE_MOVE_NOTIFY = LangEntry.builder("Command.Teleport.Send.Notify")
            .chatMessage(
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    GRAY.wrap("You have been teleported to " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                            + "."));

    private static final MessageLocale MESSAGE_SURFACE_FEEDBACK = LangEntry.builder("Command.Teleport.Top.Target")
            .chatMessage(
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    GRAY.wrap("You have teleported " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                            + " to the surface."));

    private static final MessageLocale MESSAGE_SURFACE_NOTIFY = LangEntry.builder("Command.Teleport.Top.Notify")
            .chatMessage(
                    Sound.ENTITY_ENDERMAN_TELEPORT,
                    GRAY.wrap("You have been teleported to the surface."));

    public TeleportCommandsProvider(final EssentialModule module) {
        super(module, "teleport");
    }

    @Override
    public void setup() {
        this.register(COMMAND_LOCATION, List.of(), command -> command
                .withFullDescription(DESCRIPTION_LOCATION.text())
                .withPermission(PERMISSION_LOCATION.getName())
                .withArguments(
                        new IntegerArgument(CommandArgumentConstants.X)
                                .replaceSuggestions(this.blockCoordinate(Block::getX)),
                        new IntegerArgument(CommandArgumentConstants.Y)
                                .replaceSuggestions(this.blockCoordinate(Block::getY)),
                        new IntegerArgument(CommandArgumentConstants.Z)
                                .replaceSuggestions(this.blockCoordinate(Block::getZ)))
                .withOptionalArguments(new WorldArgument(CommandArgumentConstants.WORLD))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.teleportToCoords(sender, arguments);
                }));

        this.register(COMMAND_MOVE, List.of(), command -> command
                .withFullDescription(DESCRIPTION_SEND.text())
                .withPermission(PERMISSION_MOVE.getName())
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> CommandArgumentConstants.onlinePlayerNames()),
                        CommandArgumentConstants.string(CommandArgumentConstants.TARGET,
                                info -> CommandArgumentConstants.onlinePlayerNames()))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.movePlayerToOther(sender, arguments);
                }));

        this.register(COMMAND_BRING, List.of(), command -> command
                .withRequirement(sender -> sender instanceof Player)
                .withFullDescription(DESCRIPTION_SUMMON.text())
                .withPermission(PERMISSION_BRING.getName())
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> CommandArgumentConstants.onlinePlayerNames()))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.summonPlayer(sender, arguments);
                }));

        this.register(COMMAND_GO_TO, List.of(), command -> command
                .withRequirement(sender -> sender instanceof Player)
                .withFullDescription(DESCRIPTION_TO.text())
                .withPermission(PERMISSION_GOTO.getName())
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> CommandArgumentConstants.onlinePlayerNames()))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.teleportToPlayer(sender, arguments);
                }));

        this.register(COMMAND_TOP, List.of(), command -> command
                .withFullDescription(DESCRIPTION_TOP.text())
                .withPermission(PERMISSION_SURFACE.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.teleportToTop(sender, arguments);
                }));
    }

    private ArgumentSuggestions<CommandSender> blockCoordinate(final Function<Block, Integer> function) {
        return ArgumentSuggestions.stringCollection(info -> {
            if (!(info.sender() instanceof final Player player)) {
                return Collections.emptyList();
            }
            final Block block = player.getTargetBlock(null, 100);
            return Lists.newList(NumberUtil.format(function.apply(block)));
        });
    }

    private int teleportToCoords(final CommandSender sender, final CommandArguments arguments) {
        final Object xArg = arguments.get(CommandArgumentConstants.X);
        final Object yArg = arguments.get(CommandArgumentConstants.Y);
        final Object zArg = arguments.get(CommandArgumentConstants.Z);
        if (!(xArg instanceof final Integer x) || !(yArg instanceof final Integer y)
                || !(zArg instanceof final Integer z)) {
            return 0;
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERMISSION_LOCATION_OTHERS.getName(), (user, player) -> {
            final Object worldArg = arguments.get(CommandArgumentConstants.WORLD);
            final World world = worldArg instanceof final World parsed ? parsed : player.getWorld();

            final Block block = world.getBlockAt(x, y, z);
            final Location location = block.getRelative(BlockFace.UP).getLocation();

            final TeleportContext teleportContext = TeleportContext
                    .builder(this.module, player, location)
                    .withFlag(TeleportFlag.KEEP_DIRECTION)
                    .withFlag(TeleportFlag.CENTERED)
                    .sender(sender)
                    .callback(() -> {
                        if (sender != player) {
                            this.module.sendPrefixed(MESSAGE_COORDS_FEEDBACK, sender,
                                    replacer -> replacer
                                            .with(CommonPlaceholders.PLAYER.resolver(player))
                                            .with(CommonPlaceholders.LOCATION.resolver(location)));
                        }

                        if (!target.silent()) {
                            this.module.sendPrefixed(MESSAGE_COORDS_NOTIFY, player,
                                    replacer -> replacer.with(CommonPlaceholders.LOCATION.resolver(location)));
                        }
                    })
                    .build();

            this.module.teleportManager().teleport(teleportContext, TeleportType.OTHER);
        });
    }

    private int movePlayerToOther(final CommandSender sender, final CommandArguments arguments) {
        final Object targetArg = arguments.get(CommandArgumentConstants.TARGET);
        if (!(targetArg instanceof final String targetName)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        final Object toArg = arguments.get(CommandArgumentConstants.PLAYER);
        if (!(toArg instanceof final String toName)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final CommandArgumentConstants.Target flags = CommandArgumentConstants.target(arguments);
        if (flags == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        this.loadPlayerAndRunInMainThread(sender, targetName, target -> {

            this.loadPlayerAndRunInMainThread(sender, toName, toPlayer -> {
                final Location location = target.getLocation();

                final TeleportContext teleportContext = TeleportContext
                        .builder(this.module, target, location)
                        .withFlag(TeleportFlag.KEEP_DIRECTION)
                        .withFlag(TeleportFlag.LOOK_FOR_SURFACE)
                        .sender(sender)
                        .callback(() -> {
                            if (toPlayer != sender) {
                                this.module.sendPrefixed(MESSAGE_MOVE_FEEDBACK, sender,
                                        replacer -> replacer
                                                .with(GENERIC_SOURCE, toPlayer::getName)
                                                .with(GENERIC_TARGET, target::getName));
                            }

                            if (!flags.silent()) {
                                this.module.sendPrefixed(MESSAGE_MOVE_NOTIFY, toPlayer,
                                        replacer -> replacer
                                                .with(CommonPlaceholders.PLAYER.resolver(target)));
                            }
                        })
                        .build();

                this.module.teleportManager().teleport(teleportContext, TeleportType.OTHER);
            });
        });

        return 1;
    }

    private int summonPlayer(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player executor)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final Object playerArg = arguments.get(CommandArgumentConstants.PLAYER);
        if (!(playerArg instanceof final String playerName)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final CommandArgumentConstants.Target flags = CommandArgumentConstants.target(arguments);
        if (flags == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        this.loadPlayerAndRunInMainThread(sender, playerName, target -> {
            if (target == executor) {
                this.module.sendPrefixed(MESSAGE_SUMMON_YOURSELF, sender);
                return;
            }

            final Location location = executor.getLocation();

            final TeleportContext teleportContext = TeleportContext.builder(this.module, target, location)
                    .withFlag(TeleportFlag.KEEP_DIRECTION)
                    .withFlag(TeleportFlag.LOOK_FOR_SURFACE)
                    .callback(() -> {
                        this.module.sendPrefixed(MESSAGE_SUMMON_FEEDBACK, sender,
                                replacer -> replacer.with(CommonPlaceholders.PLAYER.resolver(target)));

                        if (!flags.silent()) {
                            this.module.sendPrefixed(MESSAGE_SUMMON_NOTIFY, target,
                                    replacer -> replacer.with(CommonPlaceholders.PLAYER.resolver(executor)));
                        }
                    })
                    .build();

            this.module.teleportManager().teleport(teleportContext, TeleportType.OTHER);
        });

        return 1;
    }

    private int teleportToPlayer(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final Object playerArg = arguments.get(CommandArgumentConstants.PLAYER);
        if (!(playerArg instanceof final String playerName)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        this.loadPlayerAndRunInMainThread(sender, playerName, target -> {
            if (target == player) {
                this.module.sendPrefixed(MESSAGE_TO_YOURSELF, sender);
                return;
            }

            final Location location = target.getLocation();
            final TeleportContext teleportContext = TeleportContext.builder(this.module, player, location)
                    .withFlag(TeleportFlag.LOOK_FOR_SURFACE)
                    .callback(() -> this.module.sendPrefixed(MESSAGE_TO_DONE, player,
                            replacer -> replacer.with(CommonPlaceholders.PLAYER.resolver(target))))
                    .build();

            this.module.teleportManager().teleport(teleportContext, TeleportType.OTHER);
        });

        return 1;
    }

    private int teleportToTop(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERMISSION_SURFACE_OTHERS.getName(), (user, player) -> {
            final Block block = player.getWorld().getHighestBlockAt(player.getLocation())
                    .getRelative(BlockFace.UP);
            final Location location = block.getLocation();

            final TeleportContext teleportContext = TeleportContext
                    .builder(this.module, player, location)
                    .withFlag(TeleportFlag.KEEP_DIRECTION)
                    .withFlag(TeleportFlag.LOOK_FOR_SURFACE)
                    .withFlag(TeleportFlag.CENTERED)
                    .sender(sender)
                    .callback(() -> {
                        if (sender != player) {
                            this.module.sendPrefixed(MESSAGE_SURFACE_FEEDBACK, sender,
                                    replacer -> replacer
                                            .with(CommonPlaceholders.PLAYER.resolver(player)));
                        }

                        if (!target.silent()) {
                            this.module.sendPrefixed(MESSAGE_SURFACE_NOTIFY, player);
                        }
                    })
                    .build();

            this.module.teleportManager().teleport(teleportContext, TeleportType.OTHER);
        });
    }
}