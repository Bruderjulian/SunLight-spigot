package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.List;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

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

public class ExperienceCommandsProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_LEVEL_ADD = "level_add";
    private static final String COMMAND_LEVEL_SET = "level_set";
    private static final String COMMAND_LEVEL_REMOVE = "level_remove";

    private static final String COMMAND_XP_ADD = "xp_add";
    private static final String COMMAND_XP_SET = "xp_set";
    private static final String COMMAND_XP_REMOVE = "xp_remove";

    private static final Permission PERMISSION_LEVEL = EssentialPerms.COMMAND.permission("experience.level");
    private static final Permission PERMISSION_LEVEL_OTHERS = EssentialPerms.COMMAND
            .permission("experience.level.others");

    private static final Permission PERMISSION_XP = EssentialPerms.COMMAND.permission("experience.xp");
    private static final Permission PERMISSION_XP_OTHERS = EssentialPerms.COMMAND
            .permission("experience.xp.others");

    private static final Permission PERMISSION_ROOT = EssentialPerms.COMMAND.permission("experience.root");

    private static final TextLocale DESCRIPTION_ROOT = LangEntry.builder("Command.Experience.Root.Desc")
            .text("Experience commands.");

    private static final TextLocale DESCRIPTION_LEVEL_ADD = LangEntry.builder("Command.Experience.AddLevel.Desc")
            .text("Add XP levels.");
    private static final TextLocale DESCRIPTION_LEVEL_SET = LangEntry.builder("Command.Experience.SetLevel.Desc")
            .text("Set XP levels.");
    private static final TextLocale DESCRIPTION_LEVEL_REMOVE = LangEntry
            .builder("Command.Experience.RemoveLevel.Desc")
            .text("Remove XP levels.");

    private static final TextLocale DESCRIPTION_XP_ADD = LangEntry.builder("Command.Experience.AddXP.Desc")
            .text("Add experience points.");
    private static final TextLocale DESCRIPTION_XP_SET = LangEntry.builder("Command.Experience.SetXP.Desc")
            .text("Set experience points.");
    private static final TextLocale DESCRIPTION_XP_REMOVE = LangEntry.builder("Command.Experience.RemoveXP.Desc")
            .text("Remove experience points.");

    private static final MessageLocale MESSAGE_XP_SET_FEEDBACK = LangEntry
            .builder("Command.Experience.SetXP.Feedback")
            .chatMessage(
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("You have set " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s total XP to "
                            + SOFT_YELLOW.wrap(GENERIC_VALUE) + " ("
                            + YELLOW.wrap(GENERIC_OLD_VALUE)
                            + DARK_GRAY.wrap(" → ") + GREEN.wrap(GENERIC_NEW_VALUE) + ")"));

    private static final MessageLocale MESSAGE_XP_SET_NOTIFY = LangEntry.builder("Command.Experience.SetXP.Notify")
            .chatMessage(
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("Your total XP has been set to " + SOFT_YELLOW.wrap(GENERIC_VALUE)
                            + "."));

    private static final MessageLocale MESSAGE_XP_ADD_FEEDBACK = LangEntry
            .builder("Command.Experience.AddXP.Feedback")
            .chatMessage(
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("You have added " + SOFT_GREEN.wrap(GENERIC_VALUE) + " XP to "
                            + WHITE.wrap(PLAYER_DISPLAY_NAME) + " ("
                            + YELLOW.wrap(GENERIC_OLD_VALUE)
                            + DARK_GRAY.wrap(" → ") + GREEN.wrap(GENERIC_NEW_VALUE) + ")"));

    private static final MessageLocale MESSAGE_XP_ADD_NOTIFY = LangEntry.builder("Command.Experience.AddXP.Notify")
            .chatMessage(
                    Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("You have been given " + SOFT_GREEN.wrap(GENERIC_VALUE) + " XP."));

    private static final MessageLocale MESSAGE_XP_REMOVE_FEEDBACK = LangEntry
            .builder("Command.Experience.RemoveXP.Feedback").chatMessage(
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            GRAY.wrap("You have took " + SOFT_RED.wrap(GENERIC_VALUE) + " XP from "
                    + WHITE.wrap(PLAYER_DISPLAY_NAME) + " ("
                    + YELLOW.wrap(GENERIC_OLD_VALUE)
                    + DARK_GRAY.wrap(" → ") + GREEN.wrap(GENERIC_NEW_VALUE) + ")"));

    private static final MessageLocale MESSAGE_XP_REMOVE_NOTIFY = LangEntry
            .builder("Command.Experience.RemoveXP.Notify").chatMessage(
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            GRAY.wrap("You have been taken " + SOFT_RED.wrap(GENERIC_VALUE) + " XP."));

    private static final MessageLocale MESSAGE_LEVEL_SET_FEEDBACK = LangEntry
            .builder("Command.Experience.SetLevel.Feedback")
            .chatMessage(
                    Sound.ENTITY_PLAYER_LEVELUP,
                    GRAY.wrap("You have set " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s XP level to "
                            + SOFT_YELLOW.wrap(GENERIC_VALUE) + " ("
                            + YELLOW.wrap(GENERIC_OLD_VALUE)
                            + DARK_GRAY.wrap(" → ") + GREEN.wrap(GENERIC_NEW_VALUE) + ")"));

    private static final MessageLocale MESSAGE_LEVEL_SET_NOTIFY = LangEntry
            .builder("Command.Experience.SetLevel.Notify")
            .chatMessage(
                    Sound.ENTITY_PLAYER_LEVELUP,
                    GRAY.wrap("Your XP level has been set to " + SOFT_YELLOW.wrap(GENERIC_VALUE)
                            + "."));

    private static final MessageLocale MESSAGE_LEVEL_ADD_FEEDBACK = LangEntry
            .builder("Command.Experience.AddLevel.Feedback")
            .chatMessage(
                    Sound.ENTITY_PLAYER_LEVELUP,
                    GRAY.wrap("You have added " + SOFT_GREEN.wrap(GENERIC_VALUE) + " XP levels to "
                            + WHITE.wrap(PLAYER_DISPLAY_NAME) + " ("
                            + YELLOW.wrap(GENERIC_OLD_VALUE)
                            + DARK_GRAY.wrap(" → ") + GREEN.wrap(GENERIC_NEW_VALUE) + ")"));

    private static final MessageLocale MESSAGE_LEVEL_ADD_NOTIFY = LangEntry
            .builder("Command.Experience.AddLevel.Notify")
            .chatMessage(
                    Sound.ENTITY_PLAYER_LEVELUP,
                    GRAY.wrap("You have been given " + SOFT_GREEN.wrap(GENERIC_VALUE)
                            + " XP levels."));

    private static final MessageLocale MESSAGE_LEVEL_REMOVE_FEEDBACK = LangEntry
            .builder("Command.Experience.RemoveLevel.Feedback")
            .chatMessage(
                    Sound.ENTITY_PLAYER_LEVELUP,
                    GRAY.wrap("You have took " + SOFT_RED.wrap(GENERIC_VALUE) + " XP levels from "
                            + WHITE.wrap(PLAYER_DISPLAY_NAME) + " ("
                            + YELLOW.wrap(GENERIC_OLD_VALUE)
                            + DARK_GRAY.wrap(" → ") + GREEN.wrap(GENERIC_NEW_VALUE) + ")"));

    private static final MessageLocale MESSAGE_LEVEL_REMOVE_NOTIFY = LangEntry
            .builder("Command.Experience.RemoveLevel.Notify")
            .chatMessage(
                    Sound.ENTITY_PLAYER_LEVELUP,
                    GRAY.wrap("You have been taken " + SOFT_RED.wrap(GENERIC_VALUE)
                            + " XP levels."));

    public ExperienceCommandsProvider(final EssentialModule module) {
        super(module, "experience");
    }

    @Override
    public void setup() {
        this.register(COMMAND_LEVEL_ADD, List.of(), command -> this.buildLevelCommand(command,
                DESCRIPTION_LEVEL_ADD, ModifyMode.ADD));
        this.register(COMMAND_LEVEL_SET, List.of(), command -> this.buildLevelCommand(command,
                DESCRIPTION_LEVEL_SET, ModifyMode.SET));
        this.register(COMMAND_LEVEL_REMOVE, List.of(), command -> this.buildLevelCommand(command,
                DESCRIPTION_LEVEL_REMOVE, ModifyMode.REMOVE));

        this.register(COMMAND_XP_ADD, List.of(), command -> this.buildXPCommand(command,
                DESCRIPTION_XP_ADD, ModifyMode.ADD));
        this.register(COMMAND_XP_SET, List.of(), command -> this.buildXPCommand(command,
                DESCRIPTION_XP_SET, ModifyMode.SET));
        this.register(COMMAND_XP_REMOVE, List.of(), command -> this.buildXPCommand(command,
                DESCRIPTION_XP_REMOVE, ModifyMode.REMOVE));

        this.registerRoot("experience", command -> command
                .withFullDescription(DESCRIPTION_ROOT.text())
                .withPermission(PERMISSION_ROOT.getName()));
    }

    private void buildLevelCommand(final dev.jorel.commandapi.CommandAPICommand command,
            final TextLocale description, final ModifyMode mode) {
        command
                .withFullDescription(description.text())
                .withPermission(PERMISSION_LEVEL.getName())
                .withArguments(new IntegerArgument(CommandArgumentConstants.AMOUNT))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.changeLevels(sender, arguments, mode);
                });
    }

    private void buildXPCommand(final dev.jorel.commandapi.CommandAPICommand command,
            final TextLocale description, final ModifyMode mode) {
        command
                .withFullDescription(description.text())
                .withPermission(PERMISSION_XP.getName())
                .withArguments(new IntegerArgument(CommandArgumentConstants.AMOUNT))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.changeXP(sender, arguments, mode);
                });
    }

    private int changeXP(final CommandSender sender, final CommandArguments arguments, final ModifyMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final Object amountArg = arguments.get(CommandArgumentConstants.AMOUNT);
        if (!(amountArg instanceof final Integer amount)) {
            return 0;
        }

        return target.runAs(this.module, sender, PERMISSION_XP_OTHERS.getName(), (user, player) -> {
            final int oldXP = player.getTotalExperience();
            MessageLocale feedbackMessage;
            MessageLocale notifyMessage;

            switch (mode) {
                case SET -> {
                    player.setTotalExperience(0);
                    player.setLevel(0);
                    player.setExp(0F);
                    player.giveExp(amount);

                    feedbackMessage = MESSAGE_XP_SET_FEEDBACK;
                    notifyMessage = MESSAGE_XP_SET_NOTIFY;
                }
                case ADD -> {
                    player.giveExp(amount);

                    feedbackMessage = MESSAGE_XP_ADD_FEEDBACK;
                    notifyMessage = MESSAGE_XP_ADD_NOTIFY;
                }
                case REMOVE -> {
                    player.giveExp(-amount);

                    feedbackMessage = MESSAGE_XP_REMOVE_FEEDBACK;
                    notifyMessage = MESSAGE_XP_REMOVE_NOTIFY;
                }
                default -> {
                    return;
                }
            }

            if (player != sender) {
                this.module.sendPrefixed(feedbackMessage, sender,
                        builder -> builder
                                .with(GENERIC_VALUE, () -> NumberUtil.format(amount))
                                .with(GENERIC_OLD_VALUE, () -> NumberUtil.format(oldXP))
                                .with(GENERIC_NEW_VALUE, () -> NumberUtil.format(player.getTotalExperience()))
                                .with(CommonPlaceholders.PLAYER.resolver(player)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(notifyMessage, player, builder -> builder
                        .with(GENERIC_VALUE, () -> NumberUtil.format(amount))
                        .with(GENERIC_OLD_VALUE, () -> NumberUtil.format(oldXP))
                        .with(GENERIC_NEW_VALUE, () -> NumberUtil.format(player.getTotalExperience())));
            }
        });
    }

    private int changeLevels(final CommandSender sender, final CommandArguments arguments, final ModifyMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final Object amountArg = arguments.get(CommandArgumentConstants.AMOUNT);
        if (!(amountArg instanceof final Integer amount)) {
            return 0;
        }

        return target.runAs(this.module, sender, PERMISSION_LEVEL_OTHERS.getName(), (user, player) -> {
            final int oldLevel = player.getLevel();

            MessageLocale feedbackMessage;
            MessageLocale notifyMessage;

            switch (mode) {
                case SET -> {
                    player.setLevel(amount);

                    feedbackMessage = MESSAGE_LEVEL_SET_FEEDBACK;
                    notifyMessage = MESSAGE_LEVEL_SET_NOTIFY;
                }
                case ADD -> {
                    player.giveExpLevels(amount);

                    feedbackMessage = MESSAGE_LEVEL_ADD_FEEDBACK;
                    notifyMessage = MESSAGE_LEVEL_ADD_NOTIFY;
                }
                case REMOVE -> {
                    player.giveExpLevels(-amount);

                    feedbackMessage = MESSAGE_LEVEL_REMOVE_FEEDBACK;
                    notifyMessage = MESSAGE_LEVEL_REMOVE_NOTIFY;
                }
                default -> {
                    return;
                }
            }

            if (player != sender) {
                this.module.sendPrefixed(feedbackMessage, sender,
                        builder -> builder
                                .with(GENERIC_VALUE, () -> NumberUtil.format(amount))
                                .with(GENERIC_OLD_VALUE, () -> NumberUtil.format(oldLevel))
                                .with(GENERIC_NEW_VALUE, () -> NumberUtil.format(player.getLevel()))
                                .with(CommonPlaceholders.PLAYER.resolver(player)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(notifyMessage, player, builder -> builder
                        .with(GENERIC_VALUE, () -> NumberUtil.format(amount))
                        .with(GENERIC_OLD_VALUE, () -> NumberUtil.format(oldLevel))
                        .with(GENERIC_NEW_VALUE, () -> NumberUtil.format(player.getLevel())));
            }
        });
    }
}
