package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.arguments.WorldArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.BukkitThing;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.teleport.TeleportContext;
import su.nightexpress.sunlight.teleport.TeleportType;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_WORLD;
import static su.nightexpress.sunlight.SLPlaceholders.PLAYER_DISPLAY_NAME;

public class DimensionCommandProvider extends CommandProvider<EssentialModule> {

    private static final Permission PERMISSION = EssentialPerms.COMMAND.permission("dimension");
    private static final Permission PERMISSION_OTHERS = EssentialPerms.COMMAND.permission("dimension.others");

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Dimension.Desc")
            .text("Teleport to a world.");

    private static final MessageLocale MESSAGE_FEEDBACK = LangEntry.builder("Command.Dimension.Target").chatMessage(
            GRAY.wrap("You've teleported player " + SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + " to the "
                    + SOFT_YELLOW.wrap(GENERIC_WORLD) + "."));

    private static final MessageLocale MESSAGE_NOTIFY = LangEntry.builder("Command.Dimension.Notify").chatMessage(
            Sound.ENTITY_ENDERMAN_TELEPORT,
            GRAY.wrap("You have teleported to the " + ORANGE.wrap(GENERIC_WORLD) + "."));

    public DimensionCommandProvider(final EssentialModule module) {
        super(module, "dimension");
    }

    @Override
    public void setup() {
        this.register("dimension", List.of(), command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION.getName())
                .withArguments(new WorldArgument(CommandArgumentConstants.WORLD))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.moveToWorld(sender, arguments);
                }));
    }

    private int moveToWorld(final CommandSender sender, final CommandArguments arguments) {
        final Object worldArg = arguments.get(CommandArgumentConstants.WORLD);
        if (!(worldArg instanceof final World world)) {
            return 0;
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERMISSION_OTHERS.getName(), (user, player) -> {
            final Location location = world.getSpawnLocation();

            final TeleportContext teleportContext = TeleportContext
                    .builder(this.module, player, location)
                    .sender(sender)
                    .callback(() -> {
                        if (sender != player) {
                            this.module.sendPrefixed(MESSAGE_FEEDBACK, sender,
                                    builder -> builder
                                            .with(CommonPlaceholders.PLAYER.resolver(player))
                                            .with(SLPlaceholders.GENERIC_WORLD,
                                                    () -> BukkitThing.getValue(world)));
                        }

                        if (!target.silent()) {
                            this.module.sendPrefixed(MESSAGE_NOTIFY, player,
                                    builder -> builder
                                            .with(SLPlaceholders.GENERIC_WORLD,
                                                    () -> BukkitThing.getValue(world)));
                        }
                    })
                    .build();

            this.module.teleportManager().teleport(teleportContext, TeleportType.OTHER);
        });
    }
}
