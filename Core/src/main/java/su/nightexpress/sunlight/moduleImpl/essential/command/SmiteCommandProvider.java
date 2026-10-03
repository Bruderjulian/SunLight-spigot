package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;
import static su.nightexpress.sunlight.SLPlaceholders.PLAYER_NAME;

public class SmiteCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_SMITE = "smite";

    private static final Permission PERMISSION = EssentialPerms.COMMAND.permission("smite");

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Smite.Desc").text(
            "Smite player with lightning.");

    private static final MessageLocale COMMAND_SMITE_TARGET = LangEntry.builder("Command.Smite.Target").chatMessage(
            GRAY.wrap("You have smited " + SOFT_YELLOW.wrap(PLAYER_NAME) + "!"));

    private static final MessageLocale COMMAND_SMITE_NOTIFY = LangEntry.builder("Command.Smite.Notify").chatMessage(
            GRAY.wrap("You have been smited!"));

    public SmiteCommandProvider(final EssentialModule module) {
        super(module, "smite");
    }

    @Override
    public void setup() {
        this.register(COMMAND_SMITE, List.of(), command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.execute(sender, arguments);
                }));
    }

    private int execute(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERMISSION.getName(), (user, player) -> {
            player.getWorld().strikeLightning(player.getLocation());

            if (sender != player) {
                this.module.sendPrefixed(COMMAND_SMITE_TARGET, sender, replacer -> replacer
                        .with(CommonPlaceholders.PLAYER.resolver(player)));
            }
            if (!target.silent()) {
                this.module.sendPrefixed(COMMAND_SMITE_NOTIFY, player);
            }
        });
    }
}