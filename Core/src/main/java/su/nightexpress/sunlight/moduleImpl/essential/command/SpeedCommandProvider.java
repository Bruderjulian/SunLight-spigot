package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.stream.IntStream;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
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

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_AMOUNT;
import static su.nightexpress.sunlight.SLPlaceholders.PLAYER_DISPLAY_NAME;

public class SpeedCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_SPEED = "speed";

    private static final String PERMISSION = EssentialPerms.COMMAND + ".speed";
    private static final String PERMISSION_OTHERS = EssentialPerms.COMMAND + ".speed.others";

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Speed.Desc")
            .text("Change walk speed.");

    private static final MessageLocale MESSAGE_SET_SPEED_NOTIFY = LangEntry.builder("Command.Speed.Done.Notify")
            .chatMessage(
                    Sound.ITEM_FIRECHARGE_USE,
                    GRAY.wrap("Your walk speed has been changed to "
                            + SOFT_YELLOW.wrap(GENERIC_AMOUNT) + "."));

    private static final MessageLocale MESSAGE_SET_SPEED_FEEDBACK = LangEntry.builder("Command.Speed.Done.Target")
            .chatMessage(
                    Sound.ITEM_FIRECHARGE_USE,
                    GRAY.wrap("You have set " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                            + "'s walk speed to "
                            + SOFT_YELLOW.wrap(GENERIC_AMOUNT) + "."));

    private static final float DEF_SPEED = 0.2F;
    private static final float MAX_SPEED = 1.0F;
    private static final int SPEEDS_AMOUNT = 10;

    public SpeedCommandProvider(final EssentialModule module) {
        super(module, "speed");
    }

    @Override
    public void setup() {
        this.register(COMMAND_SPEED, command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION)
                .withArguments(new IntegerArgument(CommandArgumentConstants.VALUE, 1)
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(
                                info -> IntStream.range(1, SPEEDS_AMOUNT + 1).boxed().map(String::valueOf)
                                        .toList())))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.changeSpeed(sender, arguments);
                }));
    }

    private int changeSpeed(final CommandSender sender, final CommandArguments arguments) {
        final Object valueArg = arguments.get(CommandArgumentConstants.VALUE);
        if (!(valueArg instanceof final Integer value)) {
            return 0;
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERMISSION_OTHERS, (user, player) -> {
            final int speed = Math.clamp(value, 1, SPEEDS_AMOUNT);
            final float realSpeed = DEF_SPEED
                    + (MAX_SPEED - DEF_SPEED) * (speed - 1) / (SPEEDS_AMOUNT - 1);

            player.setWalkSpeed(realSpeed);

            if (sender != player) {
                this.module.sendPrefixed(MESSAGE_SET_SPEED_FEEDBACK, sender, replacer -> replacer
                        .with(CommonPlaceholders.PLAYER.resolver(player))
                        .with(GENERIC_AMOUNT, () -> String.valueOf(speed)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(MESSAGE_SET_SPEED_NOTIFY, player, replacer -> replacer
                        .with(GENERIC_AMOUNT, () -> String.valueOf(speed)));
            }
        });
    }
}