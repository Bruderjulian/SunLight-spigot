package su.nightexpress.sunlight.moduleImpl.essential.command;


import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.EntityUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;

public class HatCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_HAT = "hat";

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Hat.Desc").text("Put item in head.");

    private static final String PERMISSION = EssentialPerms.COMMAND + ".hat";

    private static final MessageLocale MESSAGE_HAT_FEEDBACK = LangEntry.builder("Command.Hat.Done").chatMessage(
            Sound.ITEM_ARMOR_EQUIP_LEATHER,
            GRAY.wrap("Enjoy your new hat!"));

    private static final MessageLocale MESSAGE_EMPTY_HAND = LangEntry.builder("Command.Hat.EmptyHand").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You must hold an item in your hand to equip it!"));

    public HatCommandProvider(final EssentialModule module) {
        super(module, "hat");
    }

    @Override
    public void setup() {
        this.register(COMMAND_HAT, command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION)
                .withRequirement(sender -> sender instanceof Player)
                .executes((sender, arguments) -> {
                    return this.equipHat(sender, arguments);
                }));
    }

    private int equipHat(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            this.module.sendPrefixed(MESSAGE_EMPTY_HAND, player);
            return 0;
        }

        final EquipmentSlot slot = EquipmentSlot.HEAD;
        final ItemStack oldItem = EntityUtil.getItemInSlot(player, EquipmentSlot.HEAD);
        player.getInventory().setItemInMainHand(null);
        player.getInventory().setItem(slot, item);

        if (oldItem != null && !oldItem.getType().isAir()) {
            Players.addItem(player, oldItem);
        }

        this.module.sendPrefixed(MESSAGE_HAT_FEEDBACK, player);
        return 1;
    }
}