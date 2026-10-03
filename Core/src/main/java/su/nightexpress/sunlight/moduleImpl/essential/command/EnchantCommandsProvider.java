package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EnchantmentArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.EntityUtil;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.LangUtil;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.utils.Utils;

import static su.nightexpress.nightcore.util.Placeholders.PLAYER_DISPLAY_NAME;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class EnchantCommandsProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_ENCHANT = "enchant";
    private static final String COMMAND_DISENCHANT = "disenchant";

    private static final String ARG_SLOT = "slot";
    private static final String ARG_ENCHANT = "enchant";
    private static final String ARG_LEVEL = "level";

    private static final Permission PERMISSION_ENCHANT = EssentialPerms.COMMAND.permission("enchant");
    private static final Permission PERMISSION_ENCHANT_OTHERS = EssentialPerms.COMMAND.permission("enchant.others");
    private static final Permission PERMISSION_DISENCHANT = EssentialPerms.COMMAND.permission("disenchant");
    private static final Permission PERMISSION_DISENCHANT_OTHERS = EssentialPerms.COMMAND.permission(
            "disenchant.others");

    private static final TextLocale DESCRIPTION_ENCHANT = LangEntry.builder("Command.Enchant.Desc").text(
            "Enchant item in a slot.");
    private static final TextLocale DESCRIPTION_DISENCHANT = LangEntry.builder("Command.Disenchant.Desc").text(
            "Disenchant item in a slot.");

    private static final MessageLocale MESSAGE_ERROR_NO_ITEM = LangEntry.builder("Command.Enchant.EmptySlot")
            .chatMessage(
                    GRAY.wrap("There is no item in the " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s "
                            + SOFT_RED.wrap(
                            GENERIC_SLOT)
                            + " slot."));

    private static final MessageLocale MESSAGE_ERROR_NO_ENCHANT = LangEntry
            .builder("Command.Disenchant.NotEnchanted")
            .chatMessage(
                    GRAY.wrap("The " + SOFT_RED.wrap(GENERIC_ITEM) + " item in the "
                            + WHITE.wrap(PLAYER_DISPLAY_NAME)
                            + "'s " +
                            SOFT_RED.wrap(GENERIC_SLOT) + " slot does not contain "
                            + SOFT_RED.wrap(GENERIC_ENCHANTMENT)
                            +
                            " enchantment to remove."));

    private static final MessageLocale MESSAGE_ENCHANTED_FEEDBACK = LangEntry.builder(
            "Command.Enchant.Enchanted.Target").chatMessage(
            Sound.BLOCK_ENCHANTMENT_TABLE_USE,
            GRAY.wrap("You have enchanted " + WHITE.wrap(GENERIC_ITEM) + " with " +
                    ORANGE.wrap(GENERIC_NAME + " " + GENERIC_AMOUNT) + " in " +
                    WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s "
                    + WHITE.wrap(GENERIC_TYPE) + " slot."));

    private static final MessageLocale MESSAGE_ENCHANTED_NOTIFY = LangEntry
            .builder("Command.Enchant.Enchanted.Notify")
            .chatMessage(
                    Sound.BLOCK_ENCHANTMENT_TABLE_USE,
                    GRAY.wrap("Your " + WHITE.wrap(GENERIC_ITEM) + " has been enchanted with "
                            + ORANGE.wrap(GENERIC_NAME +
                            " " + GENERIC_AMOUNT)
                            + "."));

    private static final MessageLocale MESSAGE_DISENCHANTED_SPECIFIC_FEEDBACK = LangEntry.builder(
            "Command.Disenchant.Specific.Target").chatMessage(
            Sound.BLOCK_GRINDSTONE_USE,
            GRAY.wrap("You have removed " + ORANGE.wrap(GENERIC_ENCHANTMENT) + " from " +
                    WHITE.wrap(GENERIC_ITEM) + " in " +
                    WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s "
                    + WHITE.wrap(GENERIC_TYPE) + " slot."));

    private static final MessageLocale MESSAGE_DISENCHANTED_SPECIFIC_NOTIFY = LangEntry.builder(
            "Command.Disenchant.Specific.Notify").chatMessage(
            Sound.BLOCK_GRINDSTONE_USE,
            GRAY.wrap("Your " + WHITE.wrap(GENERIC_ITEM) + " has been disenchanted from "
                    + ORANGE.wrap(
                    GENERIC_ENCHANTMENT)
                    + "."));

    private static final MessageLocale MESSAGE_DISENCHANTED_ALL_FEEDBACK = LangEntry.builder(
            "Command.Disenchant.All.Target").chatMessage(
            Sound.BLOCK_GRINDSTONE_USE,
            GRAY.wrap("You have disenchanted " + WHITE.wrap(GENERIC_ITEM) + " in "
                    + WHITE.wrap(PLAYER_DISPLAY_NAME) +
                    "'s " + WHITE.wrap(GENERIC_TYPE) + " slot."));

    private static final MessageLocale MESSAGE_DISENCHANTED_ALL_NOTIFY = LangEntry.builder(
            "Command.Disenchant.All.Notify").chatMessage(
            Sound.BLOCK_GRINDSTONE_USE,
            GRAY.wrap("Your " + WHITE.wrap(GENERIC_ITEM) + " has been disenchanted."));

    public EnchantCommandsProvider(final EssentialModule module) {
        super(module, "enchant");
    }

    @Override
    public void setup() {
        this.register(COMMAND_ENCHANT, List.of(), command -> command
                .withFullDescription(DESCRIPTION_ENCHANT.text())
                .withPermission(PERMISSION_ENCHANT.getName())
                .withArguments(CommandArgumentConstants.string(ARG_SLOT, info -> this.slotSuggestions()),
                        new EnchantmentArgument(ARG_ENCHANT))
                .withOptionalArguments(new IntegerArgument(ARG_LEVEL, 1)
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(
                                info -> List.of("0", "1", "5", "10", "127"))))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.enchantSlot(sender, arguments);
                }));

        this.register(COMMAND_DISENCHANT, List.of(), command -> command
                .withFullDescription(DESCRIPTION_DISENCHANT.text())
                .withPermission(PERMISSION_DISENCHANT.getName())
                .withArguments(CommandArgumentConstants.string(ARG_SLOT, info -> this.slotSuggestions()))
                .withOptionalArguments(new EnchantmentArgument(ARG_ENCHANT))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.disenchantSlot(sender, arguments);
                }));
    }

    private List<String> slotSuggestions() {
        return Arrays.stream(EntityUtil.EQUIPMENT_SLOTS)
                .map(Enum::name)
                .map(String::toLowerCase)
                .toList();
    }

    /**
     * @return {@code null} when the given slot name is not a valid equipment slot.
     */
    private EquipmentSlot getSlot(final Object slotArg) {
        if (!(slotArg instanceof final String slotName)) {
            return null;
        }
        return Utils.enumOptionalValueOf(slotName, EquipmentSlot.class)
                .filter(slot -> slot != EquipmentSlot.BODY)
                .orElse(null);
    }

    private int enchantSlot(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final EquipmentSlot slot = this.getSlot(arguments.get(ARG_SLOT));
        if (slot == null) {
            return 0;
        }

        final Object enchantArg = arguments.get(ARG_ENCHANT);
        if (!(enchantArg instanceof final Enchantment enchant)) {
            return 0;
        }

        final Object levelArg = arguments.get(ARG_LEVEL);
        final int level = levelArg instanceof final Integer value ? value : 1;

        return target.runAs(this.module, sender, PERMISSION_ENCHANT_OTHERS.getName(), (user, player) -> {
            final ItemStack item = player.getInventory().getItem(slot);
            if (item.getType().isAir() || item.getItemMeta() == null) {
                this.module.sendPrefixed(MESSAGE_ERROR_NO_ITEM, sender,
                        replacer -> replacer
                                .with(CommonPlaceholders.PLAYER.resolver(player))
                                .with(GENERIC_SLOT, () -> Lang.EQUIPMENT_SLOT.getLocalized(slot)));
                return;
            }

            final ItemMeta meta = item.getItemMeta();
            if (meta instanceof EnchantmentStorageMeta storageMeta) {
                storageMeta.addStoredEnchant(enchant, level, true);
                item.setItemMeta(storageMeta);
            } else {
                meta.addEnchant(enchant, level, true);
                item.setItemMeta(meta);
            }

            if (player != sender) {
                this.module.sendPrefixed(MESSAGE_ENCHANTED_FEEDBACK,
                        sender, replacer -> replacer
                                .with(CommonPlaceholders.PLAYER.resolver(player))
                                .with(SLPlaceholders.GENERIC_TYPE, () -> Lang.EQUIPMENT_SLOT.getLocalized(slot))
                                .with(SLPlaceholders.GENERIC_ITEM, () -> ItemUtil.getNameSerialized(item))
                                .with(SLPlaceholders.GENERIC_NAME, () -> LangUtil.getSerializedName(enchant))
                                .with(SLPlaceholders.GENERIC_AMOUNT, () -> LangUtil.getEnchantmentLevelLang(level)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(MESSAGE_ENCHANTED_NOTIFY, player,
                        replacer -> replacer
                                .with(SLPlaceholders.GENERIC_TYPE, () -> Lang.EQUIPMENT_SLOT.getLocalized(slot))
                                .with(SLPlaceholders.GENERIC_ITEM, () -> ItemUtil.getNameSerialized(item))
                                .with(SLPlaceholders.GENERIC_NAME, () -> LangUtil.getSerializedName(enchant))
                                .with(SLPlaceholders.GENERIC_AMOUNT, () -> LangUtil.getEnchantmentLevelLang(level)));
            }
        });
    }

    private int disenchantSlot(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final EquipmentSlot slot = this.getSlot(arguments.get(ARG_SLOT));
        if (slot == null) {
            return 0;
        }

        final Object enchantArg = arguments.get(ARG_ENCHANT);
        final Enchantment enchant = enchantArg instanceof final Enchantment parsed ? parsed : null;
        final boolean hasEnchant = enchant != null;

        return target.runAs(this.module, sender, PERMISSION_DISENCHANT_OTHERS.getName(), (user, player) -> {
            final ItemStack item = player.getInventory().getItem(slot);
            if (item.getType().isAir() || item.getItemMeta() == null) {
                this.module.sendPrefixed(MESSAGE_ERROR_NO_ITEM, sender,
                        replacer -> replacer
                                .with(CommonPlaceholders.PLAYER.resolver(player))
                                .with(GENERIC_SLOT, () -> Lang.EQUIPMENT_SLOT.getLocalized(slot)));
                return;
            }

            final ItemMeta meta = item.getItemMeta();

            if (hasEnchant) {
                final boolean isEnchanted = (meta instanceof EnchantmentStorageMeta storageMeta
                        && storageMeta.hasStoredEnchant(enchant)
                        || meta.hasEnchant(enchant));
                if (!isEnchanted) {
                    this.module.sendPrefixed(MESSAGE_ERROR_NO_ENCHANT, sender, replacer -> replacer
                            .with(CommonPlaceholders.PLAYER.resolver(player))
                            .with(GENERIC_ITEM, () -> ItemUtil.getNameSerialized(item))
                            .with(GENERIC_SLOT, () -> Lang.EQUIPMENT_SLOT.getLocalized(slot))
                            .with(GENERIC_ENCHANTMENT, () -> LangUtil.getSerializedName(enchant)));
                    return;
                }
            }

            if (meta instanceof EnchantmentStorageMeta storageMeta) {
                if (hasEnchant) {
                    storageMeta.removeStoredEnchant(enchant);
                } else {
                    storageMeta.getStoredEnchants().keySet().forEach(storageMeta::removeStoredEnchant);
                }
            } else {
                if (hasEnchant) {
                    meta.removeEnchant(enchant);
                } else {
                    meta.removeEnchantments();
                }
            }

            item.setItemMeta(meta);

            if (player != sender) {
                this.module.sendPrefixed(
                        (hasEnchant ? MESSAGE_DISENCHANTED_SPECIFIC_FEEDBACK : MESSAGE_DISENCHANTED_ALL_FEEDBACK),
                        sender,
                        replacer -> replacer
                                .with(CommonPlaceholders.PLAYER.resolver(player))
                                .with(GENERIC_SLOT, () -> Lang.EQUIPMENT_SLOT.getLocalized(slot))
                                .with(GENERIC_ITEM, () -> ItemUtil.getNameSerialized(item))
                                .with(GENERIC_ENCHANTMENT, () -> hasEnchant
                                        ? LangUtil.getSerializedName(enchant) : ""));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(
                        hasEnchant ? MESSAGE_DISENCHANTED_SPECIFIC_NOTIFY : MESSAGE_DISENCHANTED_ALL_NOTIFY,
                        player,
                        replacer -> replacer
                                .with(GENERIC_SLOT, () -> Lang.EQUIPMENT_SLOT.getLocalized(slot))
                                .with(GENERIC_ITEM, () -> ItemUtil.getNameSerialized(item))
                                .with(GENERIC_ENCHANTMENT, () -> hasEnchant
                                        ? LangUtil.getSerializedName(enchant) : ""));
            }
        });
    }
}
