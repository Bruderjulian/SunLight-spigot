package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.text.NightMessage;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class DisposalCommandProvider extends CommandProvider<EssentialModule> {

    private static final Permission PERMISSION = EssentialPerms.COMMAND.permission("disposal");
    private static final Permission PERMISSION_OTHERS = EssentialPerms.COMMAND.permission("disposal.others");

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Disposal.Desc").text(
            "Open Virtual Disposal.");

    private static final MessageLocale MESSAGE_FEEDBACK = LangEntry.builder("Command.Disposal.Target").chatMessage(
            GRAY.wrap("You have opened " + ORANGE.wrap("Virtual Disposal") + " for " + WHITE.wrap(
                    CommonPlaceholders.PLAYER_DISPLAY_NAME) + "."));

    private static final MessageLocale MESSAGE_NOTIFY = LangEntry.builder("Command.Disposal.Notify").chatMessage(
            GRAY.wrap("You have opened " + ORANGE.wrap("Virtual Disposal.")));

    public DisposalCommandProvider(final EssentialModule module) {
        super(module, "disposal");
    }

    @Override
    public void setup() {
        this.register("disposal", List.of(), command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.openDisposal(sender, arguments);
                }));
    }

    private int openDisposal(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERMISSION_OTHERS.getName(), (user, player) -> {
            final Inventory inventory = this.module.plugin().getServer().createInventory(null,
                    this.module.settings().disposalSize.get(),
                    NightMessage.asLegacy(this.module.settings().disposalTitle.get()));
            player.openInventory(inventory);

            if (!target.silent()) {
                this.module.sendPrefixed(MESSAGE_NOTIFY, player);
            }
            if (player != sender) {
                this.module.sendPrefixed(MESSAGE_FEEDBACK, sender, builder -> builder.with(
                        CommonPlaceholders.PLAYER.resolver(player)));
            }
        });
    }
}
