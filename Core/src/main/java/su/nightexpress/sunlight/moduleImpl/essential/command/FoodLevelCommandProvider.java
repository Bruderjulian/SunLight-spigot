package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.stream.IntStream;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ModifyMode;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class FoodLevelCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_RESTORE = "restore";
    private static final String COMMAND_ADD = "add";
    private static final String COMMAND_SET = "set";
    private static final String COMMAND_REMOVE = "remove";

    private static final String PERM_ADD = EssentialPerms.COMMAND + ".foodlevel.add";
    private static final String PERM_SET = EssentialPerms.COMMAND + ".foodlevel.set";
    private static final String PERM_REMOVE = EssentialPerms.COMMAND + ".foodlevel.remove";
    private static final String PERM_RESTORE = EssentialPerms.COMMAND + ".foodlevel.restore";
    private static final String PERM_ROOT = EssentialPerms.COMMAND + ".foodlevel.root";
    private static final String PERM_OTHERS = EssentialPerms.COMMAND + ".foodlevel.others";

    private static final TextLocale DESCRIPTION_ROOT = LangEntry.builder("Command.Food.Root.Desc")
            .text("Food level commands.");
    private static final TextLocale DESCRIPTION_ADD = LangEntry.builder("Command.Food.Add.Desc")
            .text("Add food points.");
    private static final TextLocale DESCRIPTION_SET = LangEntry.builder("Command.Food.Set.Desc")
            .text("Set food level.");
    private static final TextLocale DESCRIPTION_REMOVE = LangEntry.builder("Command.Food.Remove.Desc")
            .text("Remove food points.");
    private static final TextLocale DESCRIPTION_RESTORE = LangEntry.builder("Command.Food.Restore.Desc")
            .text("Restore food level.");

    private static final MessageLocale MESSAGE_ADD_FEEDBACK = LangEntry.builder("Command.Food.Give.Target")
            .chatMessage(
                    GRAY.wrap("You have " + GREEN.wrap("added " + GENERIC_AMOUNT)
                            + " food points to "
                            + WHITE.wrap(PLAYER_DISPLAY_NAME) + " ("
                            + WHITE.wrap(GENERIC_OLD_VALUE) + DARK_GRAY.wrap(" → ")
                            + GREEN.wrap(GENERIC_NEW_VALUE) + ")"));

    private static final MessageLocale MESSAGE_REMOVE_FEEDBACK = LangEntry.builder("Command.Food.Take.Target")
            .chatMessage(
                    GRAY.wrap("You have " + RED.wrap("removed " + GENERIC_AMOUNT)
                            + " food points from "
                            + WHITE.wrap(PLAYER_DISPLAY_NAME) + " ("
                            + WHITE.wrap(GENERIC_OLD_VALUE)
                            + DARK_GRAY.wrap(" → ") + RED.wrap(GENERIC_NEW_VALUE) + ")"));

    private static final MessageLocale MESSAGE_SET_FEEDBACK = LangEntry.builder("Command.Food.Set.Target")
            .chatMessage(
                    GRAY.wrap("You have set " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                            + "'s food level to "
                            + YELLOW.wrap(GENERIC_AMOUNT) + " ("
                            + WHITE.wrap(GENERIC_OLD_VALUE) + DARK_GRAY.wrap(" → ")
                            + YELLOW.wrap(GENERIC_NEW_VALUE) + ")"));

    private static final MessageLocale MESSAGE_ADD_NOTIFY = LangEntry.builder("Command.Food.Give.Notify")
            .chatMessage(
                    GRAY.wrap("Your food level has been increased by "
                            + SOFT_YELLOW.wrap(GENERIC_AMOUNT) + "."));

    private static final MessageLocale MESSAGE_REMOVE_NOTIFY = LangEntry.builder("Command.Food.Take.Notify")
            .chatMessage(
                    GRAY.wrap("Your food level has been decreased by "
                            + SOFT_YELLOW.wrap(GENERIC_AMOUNT) + "."));

    private static final MessageLocale MESSAGE_SET_NOTIFY = LangEntry.builder("Command.Food.Set.Notify")
            .chatMessage(
                    GRAY.wrap("Your food level has been set to " + SOFT_YELLOW.wrap(GENERIC_AMOUNT)
                            + "."));

    private static final MessageLocale MESSAGE_RESTORE_NOTIFY = LangEntry.builder("Command.Food.Restore.Notify")
            .chatMessage(
                    Sound.ENTITY_GENERIC_EAT,
                    GRAY.wrap("You have been fed!"));

    private static final MessageLocale MESSAGE_RESTORE_FEEDBACK = LangEntry.builder("Command.Food.Restore.Info")
            .chatMessage(
                    Sound.ENTITY_GENERIC_EAT,
                    GRAY.wrap("You have fed " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "."));

    private static final int MAX_VALUE = 20;

    public FoodLevelCommandProvider(final EssentialModule module) {
        super(module, "foodlevel");
    }

    @Override
    public void setup() {
        this.register(COMMAND_ADD, command -> this.buildModeCommand(command, ModifyMode.ADD))
            .under("foodlevel");
        this.register(COMMAND_SET, command -> this.buildModeCommand(command, ModifyMode.SET))
            .under("foodlevel");
        this.register(COMMAND_REMOVE, command -> this.buildModeCommand(command, ModifyMode.REMOVE))
            .under("foodlevel");

        this.register(COMMAND_RESTORE, command -> command
                .withFullDescription(DESCRIPTION_RESTORE.text())
                .withPermission(PERM_RESTORE)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.restoreFood(sender, arguments);
                }))
            .under("foodlevel");

        this.registerRoot("foodlevel", command -> command
                .withFullDescription(DESCRIPTION_ROOT.text())
                .withPermission(PERM_ROOT));
    }

    private void buildModeCommand(final CommandAPICommand command, final ModifyMode mode) {
        final TextLocale description = switch (mode) {
            case ADD -> DESCRIPTION_ADD;
            case SET -> DESCRIPTION_SET;
            case REMOVE -> DESCRIPTION_REMOVE;
        };

        final String permission = switch (mode) {
            case ADD -> PERM_ADD;
            case SET -> PERM_SET;
            case REMOVE -> PERM_REMOVE;
        };

        command
                .withFullDescription(description.text())
                .withPermission(permission)
                .withArguments(new IntegerArgument(CommandArgumentConstants.AMOUNT, 0)
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(info -> IntStream.range(0, 21)
                                .boxed()
                                .map(String::valueOf).toList())))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.modifyFood(sender, arguments, mode);
                });
    }

    private int modifyFood(final CommandSender sender, final CommandArguments arguments, final ModifyMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final Object amountArg = arguments.get(CommandArgumentConstants.AMOUNT);
        if (!(amountArg instanceof final Integer amount)) {
            return 0;
        }

        return target.runAs(this.module, sender, PERM_OTHERS, (user, player) -> {
            final int oldValue = player.getFoodLevel();
            final int newValue = (int) Math.clamp(mode.modify(oldValue, amount), 0, MAX_VALUE);

            player.setFoodLevel(newValue);

            final MessageLocale infoMessage = switch (mode) {
                case SET -> MESSAGE_SET_FEEDBACK;
                case REMOVE -> MESSAGE_REMOVE_FEEDBACK;
                case ADD -> MESSAGE_ADD_FEEDBACK;
            };

            final MessageLocale notifyMessage = switch (mode) {
                case ADD -> MESSAGE_ADD_NOTIFY;
                case SET -> MESSAGE_SET_NOTIFY;
                case REMOVE -> MESSAGE_REMOVE_NOTIFY;
            };

            if (player != sender) {
                this.module.sendPrefixed(infoMessage, sender,
                        builder -> builder
                                .with(GENERIC_NEW_VALUE, () -> NumberUtil.format(player.getFoodLevel()))
                                .with(GENERIC_OLD_VALUE, () -> NumberUtil.format(oldValue))
                                .with(GENERIC_MAX, () -> NumberUtil.format(MAX_VALUE))
                                .with(GENERIC_AMOUNT, () -> NumberUtil.format(amount))
                                .with(CommonPlaceholders.PLAYER.resolver(player)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(notifyMessage, player, builder -> builder
                        .with(GENERIC_NEW_VALUE, () -> NumberUtil.format(player.getFoodLevel()))
                        .with(GENERIC_OLD_VALUE, () -> NumberUtil.format(oldValue))
                        .with(GENERIC_MAX, () -> NumberUtil.format(MAX_VALUE))
                        .with(GENERIC_AMOUNT, () -> NumberUtil.format(amount)));
            }
        });
    }

    private int restoreFood(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERM_OTHERS, (user, player) -> {
            player.setFoodLevel(MAX_VALUE);

            if (this.module.settings().foodSaturationEnabled.get()) {
                player.setSaturation(Math.min(MAX_VALUE,
                        player.getSaturation()
                                + this.module.settings().foodSaturationAmount.get().floatValue()));
            }

            if (sender != player) {
                this.module.sendPrefixed(MESSAGE_RESTORE_FEEDBACK, sender,
                        replacer -> replacer.with(CommonPlaceholders.PLAYER.resolver(player)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(MESSAGE_RESTORE_NOTIFY, player);
            }
        });
    }
}
