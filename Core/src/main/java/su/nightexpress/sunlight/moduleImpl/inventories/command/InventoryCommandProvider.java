package su.nightexpress.sunlight.moduleImpl.inventories.command;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.LangUtil;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.moduleImpl.inventories.InventoriesModule;
import su.nightexpress.sunlight.moduleImpl.inventories.InventoriesPerms;
import su.nightexpress.sunlight.moduleImpl.inventories.dialog.InventoryDialogKeys;
import su.nightexpress.sunlight.moduleImpl.inventories.dialog.impl.InventoryClearDialog.ClearRequest;
import su.nightexpress.sunlight.moduleImpl.inventories.dialog.impl.InventoryClearDialog.ClearType;
import su.nightexpress.sunlight.nms.SunNMS;
import su.nightexpress.sunlight.utils.ItemStackUtils;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_ITEM;
import static su.nightexpress.sunlight.SLPlaceholders.PLAYER_DISPLAY_NAME;

public class InventoryCommandProvider extends CommandProvider<InventoriesModule> {

        private static final String COMMAND_CLEAR = "clear";
        private static final String COMMAND_COPY = "copy";
        private static final String COMMAND_FILL = "fill";
        private static final String COMMAND_OPEN = "open";
        private static final String COMMAND_REPAIR = "repair";

        private static final Permission PERMISSION_ROOT = InventoriesPerms.COMMAND.permission("inventory.root");
        private static final Permission PERMISSION_CLEAR = InventoriesPerms.COMMAND.permission("inventory.clear");
        private static final Permission PERMISSION_CLEAR_OTHERS = InventoriesPerms.COMMAND
                        .permission("inventory.clear.others");
        private static final Permission PERMISSION_COPY = InventoriesPerms.COMMAND.permission("inventory.copy");
        private static final Permission PERMISSION_COPY_OTHERS = InventoriesPerms.COMMAND
                        .permission("inventory.copy.others");
        private static final Permission PERMISSION_FILL = InventoriesPerms.COMMAND.permission("inventory.fill");
        private static final Permission PERMISSION_OPEN = InventoriesPerms.COMMAND.permission("inventory.open");
        private static final Permission PERMISSION_REPAIR = InventoriesPerms.COMMAND.permission("inventory.repair");
        private static final Permission PERMISSION_REPAIR_OTHERS = InventoriesPerms.COMMAND
                        .permission("inventory.repair.others");

        private static final TextLocale DESCRIPTION_ROOT = LangEntry.builder("Command.Inventory.Root.Desc")
                        .text("Inventory management commands.");
        private static final TextLocale DESCRIPTION_CLEAR = LangEntry.builder("Command.Inventory.Clear.Desc")
                        .text("Clear inventory.");
        private static final TextLocale DESCRIPTION_COPY = LangEntry.builder("Command.Inventory.Copy.Desc")
                        .text("Copy player's inventory.");
        private static final TextLocale DESCRIPTION_FILL = LangEntry.builder("Command.Inventory.Fill.Desc")
                        .text("Fill player's inventory with certain item.");
        private static final TextLocale DESCRIPTION_OPEN = LangEntry.builder("Command.Inventory.Open.Desc")
                        .text("Open player's inventory.");
        private static final TextLocale DESCRIPTION_REPAIR = LangEntry.builder("Command.Inventory.Repair.Desc")
                        .text("Repair all items in the inventory.");

        private static final MessageLocale MESSAGE_CLEAR_FEEDBACK = LangEntry
                        .builder("Command.Inventory.Clear.Done.Target")
                        .chatMessage(
                                        Sound.BLOCK_FIRE_EXTINGUISH,
                                        GRAY.wrap("You have cleared " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                                                        + "'s inventory."));

        private static final MessageLocale MESSAGE_CLEAR_NOTIFY = LangEntry
                        .builder("Command.Inventory.Clear.Done.Notify")
                        .chatMessage(
                                        Sound.BLOCK_FIRE_EXTINGUISH,
                                        GRAY.wrap("Your inventory has been cleared."));

        private static final MessageLocale MESSAGE_COPY_NOTIFY = LangEntry.builder("Command.Inventory.Copy.Done.Notify")
                        .chatMessage(
                                        Sound.ITEM_ARMOR_EQUIP_LEATHER,
                                        GRAY.wrap("You have copied " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                                                        + "'s inventory contents."));

        private static final MessageLocale MESSAGE_COPY_YOURSELF = LangEntry
                        .builder("Inventories.Command.Inventory.Copy.NotYourself").chatMessage(
                                        Sound.ENTITY_VILLAGER_NO,
                                        GRAY.wrap("You can not copy your own " + ORANGE.wrap("Inventory") + "."));

        private static final MessageLocale MESSAGE_FILL_FEEDBACK = LangEntry.builder("Command.Inventory.Fill.Done")
                        .chatMessage(
                                        Sound.ENTITY_ITEM_PICKUP,
                                        GRAY.wrap("You have filled up all empty slots in the "
                                                        + WHITE.wrap(PLAYER_DISPLAY_NAME)
                                                        + "'s inventory with " + SOFT_YELLOW.wrap(GENERIC_ITEM) + "."));

        private static final MessageLocale MESSAGE_OPEN_FEEDBACK = LangEntry.builder("Command.Inventory.Open.Done")
                        .chatMessage(
                                        Sound.BLOCK_CHEST_OPEN,
                                        GRAY.wrap("You have opened " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                                                        + "'s inventory."));

        private static final MessageLocale MESSAGE_OPEN_YOURSELF = LangEntry
                        .builder("Inventories.Command.Inventory.Open.NotYourself").chatMessage(
                                        Sound.ENTITY_VILLAGER_NO,
                                        GRAY.wrap("You can not open your own " + ORANGE.wrap("Inventory")
                                                        + " using this command."));

        private static final MessageLocale MESSAGE_REPAIR_NOTIFY = LangEntry.builder("Command.Inventory.Repair.Notify")
                        .chatMessage(
                                        Sound.BLOCK_ANVIL_USE,
                                        GRAY.wrap("All items in your inventory have been repaired."));

        private static final MessageLocale MESSAGE_REPAIR_FEEDBACK = LangEntry
                        .builder("Command.Inventory.Repair.Target")
                        .chatMessage(
                                        Sound.BLOCK_ANVIL_USE,
                                        GRAY.wrap(
                                                        "You have repaired all items in the "
                                                                        + WHITE.wrap(PLAYER_DISPLAY_NAME)
                                                                        + "'s inventory."));

        public InventoryCommandProvider(final InventoriesModule module) {
                super(module, "inventory");
        }

        @Override
        public void setup() {
                this.register(COMMAND_CLEAR, List.of(), builder -> builder
                                .withFullDescription(DESCRIPTION_CLEAR.text())
                                .withPermission(PERMISSION_CLEAR.getName())
                                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                                .executes((sender, arguments) -> {
                                    return this.clearInventory(sender, arguments);
                                }));

                this.register(COMMAND_COPY, List.of(), builder -> builder
                                .withFullDescription(DESCRIPTION_COPY.text())
                                .withPermission(PERMISSION_COPY.getName())
                                .withRequirement(sender -> sender instanceof Player)
                                .withArguments(
                                                CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                                        info -> CommandArgumentConstants.onlinePlayerNames()))
                                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                                .executes((sender, arguments) -> {
                                    return this.copyInventory(sender, arguments);
                                }));

                this.register(COMMAND_FILL, List.of(), builder -> builder
                                .withFullDescription(DESCRIPTION_FILL.text())
                                .withPermission(PERMISSION_FILL.getName())
                                .withArguments(
                                                CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                                        info -> CommandArgumentConstants.onlinePlayerNames()),
                                                CommandArgumentConstants.string(CommandArgumentConstants.ITEM,
                                                        info -> materialSuggestions()))
                                .executes((sender, arguments) -> {
                                    return this.fillInventory(sender, arguments);
                                }));

                this.register(COMMAND_OPEN, List.of(), builder -> builder
                                .withFullDescription(DESCRIPTION_OPEN.text())
                                .withPermission(PERMISSION_OPEN.getName())
                                .withRequirement(sender -> sender instanceof Player)
                                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                        info -> CommandArgumentConstants.onlinePlayerNames()))
                                .executes((sender, arguments) -> {
                                    return this.openInventory(sender, arguments);
                                }));

                this.register(COMMAND_REPAIR, List.of(), builder -> builder
                                .withFullDescription(DESCRIPTION_REPAIR.text())
                                .withPermission(PERMISSION_REPAIR.getName())
                                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                                .executes((sender, arguments) -> {
                                    return this.repairInventoryItems(sender, arguments);
                                }));

                this.registerRoot("inventory", builder -> builder
                                .withFullDescription(DESCRIPTION_ROOT.text())
                                .withPermission(PERMISSION_ROOT.getName()));
        }

        private static List<String> materialSuggestions() {
                return Arrays.stream(Material.values())
                                .filter(Material::isItem)
                                .map(material -> material.getKey().getKey())
                                .sorted()
                                .toList();
        }

        private Optional<Inventory> getInventory(CommandSender sender, Player target) {
                final SunNMS internals = this.module.getInternals();
                if (internals != null) {
                        return Optional.of(internals.getPlayerInventory(target));
                }

                if (!target.isOnline()) {
                        this.module.sendPrefixed(Lang.ERROR_NO_INTERNALS_HANDLER, sender);
                        return Optional.empty();
                }

                return Optional.of(target.getInventory());
        }

        private int clearInventory(CommandSender sender, CommandArguments arguments) {
                final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
                if (target == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return target.runAs(this.module, sender, PERMISSION_CLEAR_OTHERS.getName(),
                        (user, targetPlayer) -> {
                                final boolean self = sender == targetPlayer;
                                final boolean confirm = this.module.isClearConfirmationRequired()
                                                && (self || !this.module.isClearConfirmSelfOnly());

                                if (confirm && sender instanceof final Player viewer) {
                                        this.module.plugin().showDialog(viewer, InventoryDialogKeys.CLEAR,
                                                        new ClearRequest(targetPlayer, ClearType.INVENTORY),
                                                        () -> this.doClearInventory(sender, target, targetPlayer));
                                        return;
                                }
                                this.doClearInventory(sender, target, targetPlayer);
                        });
        }

        private void doClearInventory(CommandSender sender, CommandArgumentConstants.Target target, Player targetPlayer) {
                this.getInventory(sender, targetPlayer).ifPresent(inventory -> {
                        inventory.clear();

                        if (sender != targetPlayer) {
                                this.module.sendPrefixed(MESSAGE_CLEAR_FEEDBACK, sender,
                                                builder -> builder.with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                        }
                        if (!target.silent()) {
                                this.module.sendPrefixed(MESSAGE_CLEAR_NOTIFY, targetPlayer);
                        }
                });
        }

        private int copyInventory(CommandSender sender, CommandArguments arguments) {
                if (!(sender instanceof final Player player)) {
                        return 0;
                }

                final Object nameObj = arguments.get(CommandArgumentConstants.PLAYER);
                if (!(nameObj instanceof final String playerName)) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
                if (target == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }
                // Mirrors the legacy per-argument permission: naming another executor needs the
                // 'others' node, omitting it always means the sender.
                if (target.hasTarget() && !sender.hasPermission(PERMISSION_COPY_OTHERS.getName())) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return this.loadPlayerAndRunInMainThread(sender, playerName, source -> {
                        if (source == player) {
                                this.module.sendPrefixed(MESSAGE_COPY_YOURSELF, sender);
                                return;
                        }

                        Inventory sourceInventory = this.getInventory(sender, source).orElse(null);
                        Inventory targetInventory = this.getInventory(sender, player).orElse(null);
                        if (sourceInventory == null || targetInventory == null) {
                                return;
                        }

                        for (int slot = 0; slot < targetInventory.getSize(); slot++) {
                                targetInventory.setItem(slot, sourceInventory.getItem(slot));
                        }

                        this.module.sendPrefixed(MESSAGE_COPY_NOTIFY, sender,
                                        builder -> builder.with(CommonPlaceholders.PLAYER.resolver(source)));
                }) ? 1 : 0;
        }

        private int fillInventory(CommandSender sender, CommandArguments arguments) {
                final Object nameObj = arguments.get(CommandArgumentConstants.PLAYER);
                if (!(nameObj instanceof final String playerName)) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                final Object itemObj = arguments.get(CommandArgumentConstants.ITEM);
                final Material material = itemObj instanceof final String name ? Material.matchMaterial(name) : null;
                if (material == null || material.isAir()) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return this.loadPlayerAndRunInMainThread(sender, playerName, target -> {
                        this.getInventory(sender, target).ifPresent(inventory -> {
                                for (int slot = 0; slot < 36; slot++) {
                                        ItemStack has = inventory.getItem(slot);
                                        if (has == null || has.getType().isAir()) {
                                                inventory.setItem(slot, new ItemStack(material));
                                        }
                                }

                                this.module.sendPrefixed(MESSAGE_FILL_FEEDBACK, sender, builder -> builder
                                                .with(CommonPlaceholders.PLAYER.resolver(target))
                                                .with(GENERIC_ITEM, () -> LangUtil.getSerializedName(material)));
                        });
                }) ? 1 : 0;
        }

        private int openInventory(CommandSender sender, CommandArguments arguments) {
                if (!(sender instanceof final Player player)) {
                        return 0;
                }

                final SunNMS internals = this.module.getInternals();
                if (internals == null) {
                        this.module.sendPrefixed(Lang.ERROR_NO_INTERNALS_HANDLER, sender);
                        return 0;
                }

                final Object nameObj = arguments.get(CommandArgumentConstants.PLAYER);
                if (!(nameObj instanceof final String playerName)) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return this.loadPlayerAndRunInMainThread(sender, playerName, target -> {
                        if (target == sender) {
                                this.module.sendPrefixed(MESSAGE_OPEN_YOURSELF, sender);
                                return;
                        }

                        internals.openPlayerInventory(player, target);
                        this.module.sendPrefixed(MESSAGE_OPEN_FEEDBACK, sender,
                                        builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
                }) ? 1 : 0;
        }

        private int repairInventoryItems(CommandSender sender, CommandArguments arguments) {
                final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
                if (target == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return target.runAs(this.module, sender, PERMISSION_REPAIR_OTHERS.getName(),
                        (user, targetPlayer) -> {
                                this.getInventory(sender, targetPlayer).ifPresent(inventory -> {
                                        inventory.forEach(ItemStackUtils::repairItem);

                                        if (sender != targetPlayer) {
                                                this.module.sendPrefixed(MESSAGE_REPAIR_FEEDBACK, sender,
                                                                builder -> builder.with(
                                                                                CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                                        }
                                        if (!target.silent()) {
                                                this.module.sendPrefixed(MESSAGE_REPAIR_NOTIFY, targetPlayer);
                                        }
                                });
                        });
        }
}
