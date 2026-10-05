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

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.LangUtil;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
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

public class EnderchestCommandsProvider extends CommandProvider<InventoriesModule> {

        private static final String COMMAND_CLEAR = "clear";
        private static final String COMMAND_COPY = "copy";
        private static final String COMMAND_FILL = "fill";
        private static final String COMMAND_OPEN = "open";
        private static final String COMMAND_REPAIR = "repair";

        private static final String PERMISSION_ROOT = InventoriesPerms.COMMAND + ".enderchest.root";
        private static final String PERMISSION_CLEAR = InventoriesPerms.COMMAND + ".enderchest.clear";
        private static final String PERMISSION_CLEAR_OTHERS = InventoriesPerms.COMMAND + ".enderchest.clear.others";
        private static final String PERMISSION_COPY = InventoriesPerms.COMMAND + ".enderchest.copy";
        private static final String PERMISSION_FILL = InventoriesPerms.COMMAND + ".enderchest.fill";
        private static final String PERMISSION_OPEN = InventoriesPerms.COMMAND + ".enderchest.open";
        private static final String PERMISSION_OPEN_OTHERS = InventoriesPerms.COMMAND + ".enderchest.open.others";
        private static final String PERMISSION_REPAIR = InventoriesPerms.COMMAND + ".enderchest.repair";
        private static final String PERMISSION_REPAIR_OTHERS = InventoriesPerms.COMMAND + ".enderchest.repair.others";

        private static final TextLocale DESCRIPTION_ROOT = LangEntry.builder("Command.Enderchest.Hub.Desc").text(
                        "Ender Chest commands.");
        private static final TextLocale DESCRIPTION_CLEAR = LangEntry.builder("Command.Enderchest.Clear.Desc").text(
                        "Clear Ender Chest.");
        private static final TextLocale DESCRIPTION_COPY = LangEntry.builder("Command.Enderchest.Copy.Desc").text(
                        "Copy player's Ender Chest.");
        private static final TextLocale DESCRIPTION_FILL = LangEntry.builder("Command.Enderchest.Fill.Desc").text(
                        "Fill Ender Chest with specified item.");
        private static final TextLocale DESCRIPTION_OPEN = LangEntry.builder("Command.Enderchest.Open.Desc").text(
                        "Open Ender Chest.");
        private static final TextLocale DESCRIPTION_REPAIR = LangEntry.builder("Command.Enderchest.Repair.Desc").text(
                        "Repair all Ender Chest items.");

        private static final MessageLocale MESSAGE_CLEAR_TARGETTED = LangEntry.builder(
                        "Command.Enderchest.Clear.Done.Target").chatMessage(
                                        Sound.BLOCK_FIRE_EXTINGUISH,
                                        GRAY.wrap("You have cleared " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s "
                                                        + SOFT_PURPLE.wrap("Ender Chest.")));

        private static final MessageLocale MESSAGE_CLEAR_NOTIFY = LangEntry
                        .builder("Command.Enderchest.Clear.Done.Notify")
                        .chatMessage(
                                        Sound.BLOCK_FIRE_EXTINGUISH,
                                        GRAY.wrap("Your " + SOFT_PURPLE.wrap("Ender Chest") + " has been cleared."));

        private static final MessageLocale MESSAGE_COPY_NOTIFY = LangEntry
                        .builder("Command.Enderchest.Copy.Done.Executor")
                        .chatMessage(
                                        Sound.ITEM_ARMOR_EQUIP_LEATHER,
                                        GRAY.wrap("You have copied " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s "
                                                        + SOFT_PURPLE.wrap("Ender Chest") +
                                                        " content to yours."));

        private static final MessageLocale MESSAGE_COPY_YOURSELF = LangEntry.builder(
                        "Inventories.Command.Enderchest.Copy.NotYourself").chatMessage(
                                        Sound.ENTITY_VILLAGER_NO,
                                        GRAY.wrap("You can not copy your own " + SOFT_PURPLE.wrap("Ender Chest")
                                                        + "."));

        private static final MessageLocale MESSAGE_FILL_TARGETTED = LangEntry.builder(
                        "Command.Enderchest.Fill.Done.Executor").chatMessage(
                                        Sound.ENTITY_ITEM_PICKUP,
                                        GRAY.wrap("You have filled " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s "
                                                        + SOFT_PURPLE.wrap("Ender Chest") +
                                                        " with " + WHITE.wrap(GENERIC_ITEM + ".")));

        private static final MessageLocale MESSAGE_OPEN_NOTIFY = LangEntry.builder("Command.Enderchest.Open.Notify")
                        .chatMessage(
                                        Sound.BLOCK_ENDER_CHEST_OPEN,
                                        GRAY.wrap("You have opened your " + SOFT_PURPLE.wrap("Ender Chest.")));

        private static final MessageLocale MESSAGE_OPEN_TARGETTED = LangEntry
                        .builder("Command.Enderchest.Open.Targetted")
                        .chatMessage(
                                        Sound.BLOCK_ENDER_CHEST_OPEN,
                                        GRAY.wrap("You have opened " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s "
                                                        + SOFT_PURPLE.wrap("Ender Chest.")));

        private static final MessageLocale MESSAGE_REPAIR_NOTIFY = LangEntry.builder("Command.Enderchest.Repair.Notify")
                        .chatMessage(
                                        Sound.BLOCK_ANVIL_USE,
                                        GRAY.wrap("All items in your " + SOFT_PURPLE.wrap("Ender Chest")
                                                        + " has been repaired."));

        private static final MessageLocale MESSAGE_REPAIR_TARGETTED = LangEntry
                        .builder("Command.Enderchest.Repair.Target")
                        .chatMessage(
                                        Sound.BLOCK_ANVIL_USE,
                                        GRAY.wrap("You have repaired all items in " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                                                        + "'s "
                                                        + SOFT_PURPLE.wrap(
                                                                        "Ender Chest.")));

        public EnderchestCommandsProvider(final InventoriesModule module) {
                super(module, "enderchest");
        }

        @Override
        public void setup() {
                this.register(COMMAND_CLEAR, builder -> builder
                                .withFullDescription(DESCRIPTION_CLEAR.text())
                                .withPermission(PERMISSION_CLEAR)
                                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                                .executes((sender, arguments) -> {
                                    return this.executeClear(sender, arguments);
                                }))
                    .under("enderchest");

                this.register(COMMAND_COPY, builder -> builder
                                .withFullDescription(DESCRIPTION_COPY.text())
                                .withPermission(PERMISSION_COPY)
                                .withRequirement(sender -> sender instanceof Player)
                                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                        info -> CommandArgumentConstants.onlinePlayerNames()))
                                .executes((sender, arguments) -> {
                                    return this.executeCopy(sender, arguments);
                                }))
                    .under("enderchest");

                this.register(COMMAND_FILL, builder -> builder
                                .withFullDescription(DESCRIPTION_FILL.text())
                                .withPermission(PERMISSION_FILL)
                                .withArguments(
                                                CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                                        info -> CommandArgumentConstants.onlinePlayerNames()),
                                                CommandArgumentConstants.string(CommandArgumentConstants.ITEM,
                                                        info -> materialSuggestions()))
                                .executes((sender, arguments) -> {
                                    return this.executeFill(sender, arguments);
                                }))
                    .under("enderchest");

                this.register(COMMAND_OPEN, builder -> builder
                                .withFullDescription(DESCRIPTION_OPEN.text())
                                .withPermission(PERMISSION_OPEN)
                                .withRequirement(sender -> sender instanceof Player)
                                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                                .executes((sender, arguments) -> {
                                    return this.executeOpen(sender, arguments);
                                }))
                    .under("enderchest");

                this.register(COMMAND_REPAIR, builder -> builder
                                .withFullDescription(DESCRIPTION_REPAIR.text())
                                .withPermission(PERMISSION_REPAIR)
                                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                                .executes((sender, arguments) -> {
                                    return this.executeRepair(sender, arguments);
                                }))
                    .under("enderchest");

                this.registerRoot("enderchest", builder -> builder
                                .withFullDescription(DESCRIPTION_ROOT.text())
                                .withPermission(PERMISSION_ROOT)).aliases("ec");
        }

        private static List<String> materialSuggestions() {
                return Arrays.stream(Material.values())
                                .filter(Material::isItem)
                                .map(material -> material.getKey().getKey())
                                .sorted()
                                .toList();
        }

        private Optional<Inventory> getEnderChest(CommandSender sender, Player target) {
                final SunNMS internals = this.module.getInternals();
                if (internals != null) {
                        return Optional.of(internals.getPlayerEnderChest(target));
                }

                if (!target.isOnline()) {
                        this.module.sendPrefixed(Lang.ERROR_NO_INTERNALS_HANDLER, sender);
                        return Optional.empty();
                }

                return Optional.of(target.getEnderChest());
        }

        public int executeClear(CommandSender sender, CommandArguments arguments) {
                final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
                if (target == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return target.runAs(this.module, sender, PERMISSION_CLEAR_OTHERS,
                        (user, targetPlayer) -> {
                                final boolean self = sender == targetPlayer;
                                final boolean confirm = this.module.isClearConfirmationRequired()
                                                && (self || !this.module.isClearConfirmSelfOnly());

                                if (confirm && sender instanceof final Player viewer) {
                                        this.module.plugin().showDialog(viewer, InventoryDialogKeys.CLEAR,
                                                        new ClearRequest(targetPlayer, ClearType.ENDER_CHEST),
                                                        () -> this.doClearEnderChest(sender, target, targetPlayer));
                                        return;
                                }
                                this.doClearEnderChest(sender, target, targetPlayer);
                        });
        }

        private void doClearEnderChest(CommandSender sender, CommandArgumentConstants.Target target, Player targetPlayer) {
                this.getEnderChest(sender, targetPlayer).ifPresent(inventory -> {
                        inventory.clear();

                        if (sender != targetPlayer) {
                                this.module.sendPrefixed(MESSAGE_CLEAR_TARGETTED, sender,
                                                builder -> builder
                                                                .with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                        }

                        if (!target.silent()) {
                                this.module.sendPrefixed(MESSAGE_CLEAR_NOTIFY, targetPlayer);
                        }
                });
        }

        public int executeCopy(CommandSender sender, CommandArguments arguments) {
                if (!(sender instanceof final Player player)) {
                        return 0;
                }

                final Object nameObj = arguments.get(CommandArgumentConstants.PLAYER);
                if (!(nameObj instanceof final String playerName)) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return this.loadPlayerAndRunInMainThread(sender, playerName, target -> {
                        if (target == player) {
                                this.module.sendPrefixed(MESSAGE_COPY_YOURSELF, sender);
                                return;
                        }

                        Inventory invFrom = this.getEnderChest(sender, target).orElse(null);
                        Inventory invTo = this.getEnderChest(sender, player).orElse(null);
                        if (invFrom == null || invTo == null) {
                                return;
                        }

                        for (int slot = 0; slot < invTo.getSize(); slot++) {
                                invTo.setItem(slot, invFrom.getItem(slot));
                        }

                        this.module.sendPrefixed(MESSAGE_COPY_NOTIFY, sender, builder -> builder
                                        .with(CommonPlaceholders.PLAYER.resolver(target)));
                }) ? 1 : 0;
        }

        public int executeFill(CommandSender sender, CommandArguments arguments) {
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

                return this.loadPlayerAndRunInMainThread(sender, playerName, targetPlayer -> {
                        this.getEnderChest(sender, targetPlayer).ifPresent(inventory -> {
                                for (int slot = 0; slot < inventory.getSize(); slot++) {
                                        ItemStack has = inventory.getItem(slot);
                                        if (has == null || has.getType().isAir()) {
                                                inventory.setItem(slot, new ItemStack(material));
                                        }
                                }

                                this.module.sendPrefixed(MESSAGE_FILL_TARGETTED, sender, builder -> builder
                                                .with(CommonPlaceholders.PLAYER.resolver(targetPlayer))
                                                .with(SLPlaceholders.GENERIC_ITEM,
                                                        () -> LangUtil.getSerializedName(material)));
                        });
                }) ? 1 : 0;
        }

        public int executeOpen(CommandSender sender, CommandArguments arguments) {
                if (!(sender instanceof final Player player)) {
                        return 0;
                }

                final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
                if (target == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return target.runAs(this.module, sender, PERMISSION_OPEN_OTHERS,
                        (user, targetPlayer) -> {
                                this.getEnderChest(sender, targetPlayer).ifPresent(inventory -> {
                                        player.openInventory(inventory);

                                        if (player != targetPlayer) {
                                                this.module.sendPrefixed(MESSAGE_OPEN_TARGETTED, player,
                                                                builder -> builder.with(
                                                                                CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                                        } else {
                                                this.module.sendPrefixed(MESSAGE_OPEN_NOTIFY, player);
                                        }
                                });
                        });
        }

        public int executeRepair(CommandSender sender, CommandArguments arguments) {
                final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
                if (target == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return target.runAs(this.module, sender, PERMISSION_REPAIR_OTHERS,
                        (user, targetPlayer) -> {
                                this.getEnderChest(sender, targetPlayer).ifPresent(inventory -> {
                                        inventory.forEach(ItemStackUtils::repairItem);

                                        if (sender != targetPlayer) {
                                                this.module.sendPrefixed(MESSAGE_REPAIR_TARGETTED, sender,
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
