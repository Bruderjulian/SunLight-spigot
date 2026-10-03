package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.List;
import java.util.stream.IntStream;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_AMOUNT;
import static su.nightexpress.sunlight.SLPlaceholders.PLAYER_DISPLAY_NAME;

public class FlySpeedCommandProvider extends CommandProvider<EssentialModule> {

    private static final float DEF_SPEED = 0.1F;
    private static final float MAX_SPEED = 1.0F;
    private static final int SPEEDS_AMOUNT = 10;

    // TODO Per speed permission

    private static final Permission PERMISSION = EssentialPerms.COMMAND.permission("flyspeed");
    private static final Permission PERMISSION_OTHERS = EssentialPerms.COMMAND.permission("flyspeed.others");

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.FlySpeed.Desc")
            .text("Change fly speed.");

    private static final MessageLocale MESSAGE_SET_NOTIFY = LangEntry.builder("Command.FlySpeed.Done.Notify")
            .chatMessage(
                    GRAY.wrap("Your fly speed has been set to " + SOFT_YELLOW.wrap(GENERIC_AMOUNT)
                            + "."));

    private static final MessageLocale MESSAGE_SET_FEEDBACK = LangEntry
            .builder("Command.FlySpeed.Done.Target")
            .chatMessage(
                    GRAY.wrap("You have set " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s fly speed to "
                            + SOFT_YELLOW.wrap(GENERIC_AMOUNT) + "."));

    public FlySpeedCommandProvider(final EssentialModule module) {
        super(module, "flyspeed");
    }

    @Override
    public void setup() {
        this.register("flyspeed", List.of(), command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION.getName())
                .withArguments(new IntegerArgument(CommandArgumentConstants.VALUE)
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> IntStream
                                .range(1, SPEEDS_AMOUNT + 1).boxed()
                                .map(String::valueOf).toList())))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.setFlySpeed(sender, arguments);
                }));
    }

    private int setFlySpeed(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final Object valueArg = arguments.get(CommandArgumentConstants.VALUE);
        if (!(valueArg instanceof final Integer value)) {
            return 0;
        }

        final int speed = Math.clamp(value, 1, SPEEDS_AMOUNT);

        return target.runAs(this.module, sender, PERMISSION_OTHERS.getName(), (user, player) -> {
            final float realSpeed = DEF_SPEED
                    + (MAX_SPEED - DEF_SPEED) * (speed - 1) / (SPEEDS_AMOUNT - 1);

            player.setFlySpeed(realSpeed);

            if (sender != player) {
                this.module.sendPrefixed(MESSAGE_SET_FEEDBACK, sender,
                        builder -> builder
                                .with(CommonPlaceholders.PLAYER.resolver(player))
                                .with(SLPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(speed)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(MESSAGE_SET_NOTIFY, player, builder -> builder
                        .with(SLPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(speed)));
            }
        });
    }
}
