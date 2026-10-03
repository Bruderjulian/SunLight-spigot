package su.nightexpress.sunlight.moduleImpl.inventories.command;

import java.util.List;
import java.util.stream.Stream;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.EnumLocale;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.sound.VanillaSound;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.api.PortableContainer;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.inventories.InventoriesModule;
import su.nightexpress.sunlight.moduleImpl.inventories.InventoriesPerms;
import su.nightexpress.sunlight.nms.SunNMS;

import static su.nightexpress.nightcore.util.Placeholders.PLAYER_DISPLAY_NAME;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_TYPE;

public class ContainerCommandProvider extends CommandProvider<InventoriesModule> {

        private static final Permission PERMISSION_ROOT = InventoriesPerms.COMMAND.permission("container.root");
        private static final Permission PERMISSION_OTHERS = InventoriesPerms.COMMAND.permission("container.others");
        private static final Permission PERMISSION_ANVIL = InventoriesPerms.COMMAND.permission("container.anvil");
        private static final Permission PERMISSION_LOOM = InventoriesPerms.COMMAND.permission("container.loom");
        private static final Permission PERMISSION_WORKBENCH = InventoriesPerms.COMMAND
                        .permission("container.workbench");
        private static final Permission PERMISSION_SMITHING = InventoriesPerms.COMMAND.permission("container.smithing");
        private static final Permission PERMISSION_GRINDSTONE = InventoriesPerms.COMMAND.permission(
                        "container.grindstone");
        private static final Permission PERMISSION_CARTOGRAPHY = InventoriesPerms.COMMAND.permission(
                        "container.cartography");
        private static final Permission PERMISSION_ENCHANTING = InventoriesPerms.COMMAND.permission(
                        "container.enchanting");
        private static final Permission PERMISSION_STONECUTTER = InventoriesPerms.COMMAND.permission(
                        "container.stonecutter");

        private static final TextLocale DESCRIPTION_ROOT = LangEntry.builder("Command.Container.Root.Desc").text(
                        "Portable Container commands.");
        private static final TextLocale DESCRIPTION_TYPE = LangEntry.builder("Command.Container.Type.Desc").text(
                        "Opens Portable " + GENERIC_TYPE + ".");

        private static final MessageLocale MESSAGE_NOTIFY = LangEntry.builder("Command.Container.Notify").chatMessage(
                        GRAY.wrap("You have opened " + SOFT_AQUA.wrap("Portable " + GENERIC_TYPE + ".")));

        private static final MessageLocale MESSAGE_TARGETTED = LangEntry.builder("Command.Container.Target")
                        .chatMessage(
                                        GRAY.wrap("You have opened " + SOFT_YELLOW.wrap("Portable " + GENERIC_TYPE)
                                                        + " for " + SOFT_YELLOW.wrap(
                                                        PLAYER_DISPLAY_NAME + ".")));

        private static final EnumLocale<PortableContainer> CONTAINER_LOCALE = LangEntry.builder("MenuType").enumeration(
                        PortableContainer.class);

        public ContainerCommandProvider(final InventoriesModule module) {
                super(module, "container");
        }

        private Permission getPermission(PortableContainer container) {
                return switch (container) {
                        case ANVIL -> PERMISSION_ANVIL;
                        case LOOM -> PERMISSION_LOOM;
                        case WORKBENCH -> PERMISSION_WORKBENCH;
                        case SMITHING_TABLE -> PERMISSION_SMITHING;
                        case GRINDSTONE -> PERMISSION_GRINDSTONE;
                        case CARTOGRAPHY_TABLE -> PERMISSION_CARTOGRAPHY;
                        case ENCHANTING_TABLE -> PERMISSION_ENCHANTING;
                        case STONECUTTER -> PERMISSION_STONECUTTER;
                };
        }

        private Sound getSound(PortableContainer container) {
                return switch (container) {
                        case ANVIL -> Sound.BLOCK_ANVIL_PLACE;
                        case LOOM -> Sound.UI_LOOM_SELECT_PATTERN;
                        case WORKBENCH -> Sound.BLOCK_WOOD_PLACE;
                        case SMITHING_TABLE -> Sound.BLOCK_SMITHING_TABLE_USE;
                        case GRINDSTONE -> Sound.BLOCK_GRINDSTONE_USE;
                        case CARTOGRAPHY_TABLE -> Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT;
                        case ENCHANTING_TABLE -> Sound.BLOCK_ENCHANTMENT_TABLE_USE;
                        case STONECUTTER -> Sound.UI_STONECUTTER_TAKE_RESULT;
                };
        }

        @Override
        public void setup() {
                Stream.of(PortableContainer.values()).forEach(container -> {
                        Permission permission = this.getPermission(container);
                        String label = container.label();

                        this.register(label, List.of(), command -> command
                                        .withFullDescription(DESCRIPTION_TYPE.text().replace(SLPlaceholders.GENERIC_TYPE,
                                                        CONTAINER_LOCALE.getLocalized(container)))
                                        .withPermission(permission.getName())
                                        .withOptionalArguments(CommandArgumentConstants.targetArgument())
                                        .executes((sender, arguments) -> {
                                            return this.open(sender, arguments, container);
                                        }));
                });

                this.registerRoot("container", command -> command
                                .withFullDescription(DESCRIPTION_ROOT.text())
                                .withPermission(PERMISSION_ROOT.getName()));
        }

        private int open(CommandSender sender, CommandArguments arguments, PortableContainer container) {
                final SunNMS nms = this.module.getInternals();
                if (nms == null) {
                        return 0;
                }

                final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
                if (target == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return target.runAs(this.module, sender, PERMISSION_OTHERS.getName(), (user, targetPlayer) -> {
                        nms.openContainer(targetPlayer, container);
                        VanillaSound.of(this.getSound(container)).play(targetPlayer);

                        if (sender != targetPlayer) {
                                this.module.sendPrefixed(MESSAGE_TARGETTED, sender, builder -> builder
                                                .with(CommonPlaceholders.PLAYER.resolver(targetPlayer))
                                                .with(SLPlaceholders.GENERIC_TYPE,
                                                                () -> CONTAINER_LOCALE.getLocalized(container)));
                        }

                        if (!target.silent()) {
                                this.module.sendPrefixed(MESSAGE_NOTIFY, targetPlayer, builder -> builder
                                                .with(SLPlaceholders.GENERIC_TYPE,
                                                        () -> CONTAINER_LOCALE.getLocalized(container)));
                        }
                });
        }
}
