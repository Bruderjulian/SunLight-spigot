package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.List;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.WHITE;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_STATE;
import static su.nightexpress.sunlight.SLPlaceholders.PLAYER_DISPLAY_NAME;

public class FlyCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_TOGGLE = "toggle";
    private static final String COMMAND_OFF = "off";
    private static final String COMMAND_ON = "on";

    // TODO world restrictions, tempfly

    private static final Permission PERMISSION = EssentialPerms.COMMAND.permission("fly");
    private static final Permission PERMISSION_OTHERS = EssentialPerms.COMMAND.permission("fly.others");
    private static final Permission PERMISSION_ROOT = EssentialPerms.COMMAND.permission("fly.root");

    private static final TextLocale DESCRIPTION_ROOT = LangEntry.builder("Command.Fly.Root.Desc").text(
            "Fly commands.");
    private static final TextLocale DESCRIPTION_TOGGLE = LangEntry.builder("Command.Fly.Toggle.Desc").text(
            "Toggle fly.");
    private static final TextLocale DESCRIPTION_ON = LangEntry.builder("Command.Fly.On.Desc").text("Enable fly.");
    private static final TextLocale DESCRIPTION_OFF = LangEntry.builder("Command.Fly.Off.Desc")
            .text("Disable fly.");

    private static final MessageLocale MESSAGE_TOGGLE_FEEDBACK = LangEntry.builder("Command.Fly.Target")
            .chatMessage(
                    Sound.ITEM_FIRECHARGE_USE,
                    GRAY.wrap("You have set " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s fly on "
                            + WHITE.wrap(GENERIC_STATE)
                            + "."));

    private static final MessageLocale MESSAGE_TOGGLE_NOTIFY = LangEntry.builder("Command.Fly.Notify").chatMessage(
            Sound.ITEM_FIRECHARGE_USE,
            GRAY.wrap("Your fly has been set on " + WHITE.wrap(GENERIC_STATE) + "."));

    public FlyCommandProvider(final EssentialModule module) {
        super(module, "fly");
    }

    @Override
    public void setup() {
        this.register(COMMAND_TOGGLE, List.of(), command -> this.buildCommand(command,
                DESCRIPTION_TOGGLE, ToggleMode.TOGGLE));
        this.register(COMMAND_ON, List.of(), command -> this.buildCommand(command,
                DESCRIPTION_ON, ToggleMode.ON));
        this.register(COMMAND_OFF, List.of(), command -> this.buildCommand(command,
                DESCRIPTION_OFF, ToggleMode.OFF));

        this.registerRoot("flymode", command -> command
                .withFullDescription(DESCRIPTION_ROOT.text())
                .withPermission(PERMISSION_ROOT.getName()));
    }

    private void buildCommand(final CommandAPICommand command, final TextLocale description, final ToggleMode mode) {
        command
                .withFullDescription(description.text())
                .withPermission(PERMISSION.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleFly(sender, arguments, mode);
                });
    }

    private int toggleFly(final CommandSender sender, final CommandArguments arguments, final ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERMISSION_OTHERS.getName(), (user, player) -> {
            player.setAllowFlight(mode.apply(player.getAllowFlight()));

            if (sender != player) {
                this.module.sendPrefixed(MESSAGE_TOGGLE_FEEDBACK, sender,
                        builder -> builder
                                .with(CommonPlaceholders.PLAYER.resolver(player))
                                .with(SLPlaceholders.GENERIC_STATE,
                                        () -> CoreLang.STATE_ENABLED_DISALBED.get(player.getAllowFlight())));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(MESSAGE_TOGGLE_NOTIFY, player,
                        builder -> builder
                                .with(SLPlaceholders.GENERIC_STATE,
                                        () -> CoreLang.STATE_ENABLED_DISALBED.get(player.getAllowFlight())));
            }
        });
    }
}
