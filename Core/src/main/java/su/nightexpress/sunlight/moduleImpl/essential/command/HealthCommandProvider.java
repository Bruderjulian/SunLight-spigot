package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.List;
import java.util.stream.IntStream;

import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.EntityUtil;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ModifyMode;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class HealthCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_RESTORE = "restore";
    private static final String COMMAND_ADD = "add";
    private static final String COMMAND_SET = "set";
    private static final String COMMAND_REMOVE = "remove";

    private static final Permission PERM_ADD = EssentialPerms.COMMAND.permission("health.add");
    private static final Permission PERM_SET = EssentialPerms.COMMAND.permission("health.set");
    private static final Permission PERM_REMOVE = EssentialPerms.COMMAND.permission("health.remove");
    private static final Permission PERM_RESTORE = EssentialPerms.COMMAND.permission("health.restore");
    private static final Permission PERM_ROOT = EssentialPerms.COMMAND.permission("health.root");
    private static final Permission PERM_OTHERS = EssentialPerms.COMMAND.permission("health.others");

    private static final TextLocale DESCRIPTION_ROOT = LangEntry.builder("Command.Health.Root.Desc")
            .text("Health commands.");
    private static final TextLocale DESCRIPTION_ADD = LangEntry.builder("Command.Health.Add.Desc")
            .text("Add health points.");
    private static final TextLocale DESCRIPTION_SET = LangEntry.builder("Command.Health.Set.Desc")
            .text("Set health points.");
    private static final TextLocale DESCRIPTION_REMOVE = LangEntry.builder("Command.Health.Remove.Desc")
            .text("Remove health points.");
    private static final TextLocale DESCRIPTION_RESTORE = LangEntry.builder("Command.Health.Restore.Desc")
            .text("Restore player's health or revive.");

    private static final MessageLocale MESSAGE_ADD_FEEDBACK = LangEntry.builder("Command.Health.Add.Target")
            .chatMessage(
                    Sound.ENTITY_WITCH_DRINK,
                    GRAY.wrap("You have " + GREEN.wrap("healed") + " "
                            + WHITE.wrap(PLAYER_DISPLAY_NAME) + " for "
                            + GREEN.wrap(GENERIC_AMOUNT + "❤") + " ("
                            + WHITE.wrap(GENERIC_OLD_VALUE + "❤")
                            + DARK_GRAY.wrap(" → ") + GREEN.wrap(GENERIC_NEW_VALUE + "❤")
                            + ")"));

    private static final MessageLocale MESSAGE_REMOVE_FEEDBACK = LangEntry.builder("Command.Health.Remove.Target")
            .chatMessage(
                    Sound.ENTITY_PLAYER_HURT,
                    GRAY.wrap("You have " + RED.wrap("damaged") + " "
                            + WHITE.wrap(PLAYER_DISPLAY_NAME) + " for "
                            + RED.wrap(GENERIC_AMOUNT + "❤") + " ("
                            + WHITE.wrap(GENERIC_OLD_VALUE + "❤")
                            + DARK_GRAY.wrap(" → ") + RED.wrap(GENERIC_NEW_VALUE + "❤")
                            + ")"));

    private static final MessageLocale MESSAGE_SET_FEEDBACK = LangEntry.builder("Command.Health.Set.Target")
            .chatMessage(
                    Sound.ENTITY_WITCH_DRINK,
                    GRAY.wrap("You have " + YELLOW.wrap("set") + " "
                            + WHITE.wrap(PLAYER_DISPLAY_NAME) + "'s health to "
                            + YELLOW.wrap(GENERIC_AMOUNT + "❤") + " ("
                            + WHITE.wrap(GENERIC_OLD_VALUE + "❤")
                            + DARK_GRAY.wrap(" → ") + RED.wrap(GENERIC_NEW_VALUE + "❤")
                            + ")"));

    private static final MessageLocale MESSAGE_ADD_NOTIFY = LangEntry.builder("Command.Health.Add.Notify")
            .chatMessage(
                    Sound.ENTITY_WITCH_DRINK,
                    GRAY.wrap("You have been " + GREEN.wrap("healed") + " for "
                            + GREEN.wrap(GENERIC_AMOUNT + "❤") + "."));

    private static final MessageLocale MESSAGE_REMOVE_NOTIFY = LangEntry.builder("Command.Health.Remove.Notify")
            .chatMessage(
                    Sound.ENTITY_PLAYER_HURT,
                    GRAY.wrap("You have been " + RED.wrap("damaged") + " for "
                            + RED.wrap(GENERIC_AMOUNT + "❤") + "."));

    private static final MessageLocale MESSAGE_SET_NOTIFY = LangEntry.builder("Command.Health.Set.Notify")
            .chatMessage(
                    Sound.ENTITY_WITCH_DRINK,
                    GRAY.wrap("Your health has been set to " + YELLOW.wrap(GENERIC_AMOUNT + "❤")
                            + "."));

    private static final MessageLocale MESSAGE_RESTORE_NOTIFY = LangEntry.builder("Command.Health.Restore.Notfiy")
            .chatMessage(
                    Sound.ENTITY_WITCH_DRINK,
                    GRAY.wrap("You have been fully healed."));

    private static final MessageLocale MESSAGE_RESTORE_FEEDBACK = LangEntry.builder("Command.Health.Restore.Info")
            .chatMessage(
                    Sound.ENTITY_WITCH_DRINK,
                    GRAY.wrap("You have restored " + WHITE.wrap(PLAYER_DISPLAY_NAME)
                            + "'s health."));

    private static final MessageLocale MESSAGE_REVIVE_NOTIFY = LangEntry.builder("Command.Health.Revive.Feedback")
            .chatMessage(
                    Sound.ITEM_TOTEM_USE,
                    GRAY.wrap("You have been revived."));

    private static final MessageLocale MESSAGE_REVIVE_FEEDBACK = LangEntry.builder("Command.Health.Revive.Notify")
            .chatMessage(
                    Sound.ITEM_TOTEM_USE,
                    GRAY.wrap("You have revived " + WHITE.wrap(PLAYER_DISPLAY_NAME) + "."));

    private static final MessageLocale MESSAGE_DEAD_FEEDBACK = LangEntry
            .builder("Command.Health.TargetDead.Feedback")
            .chatMessage(
                    Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(WHITE.wrap(PLAYER_DISPLAY_NAME)
                            + "'s health can not modified, because they are dead."));

    private static final MessageLocale MESSAGE_DEAD_NOTIFY = LangEntry.builder("Command.Health.TargetDead.Notify")
            .chatMessage(
                    Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("You can't change your health while you are dead."));

    public HealthCommandProvider(final EssentialModule module) {
        super(module, "health");
    }

    @Override
    public void setup() {
        this.register(COMMAND_ADD, List.of(), command -> this.buildMode(command, ModifyMode.ADD));
        this.register(COMMAND_SET, List.of(), command -> this.buildMode(command, ModifyMode.SET));
        this.register(COMMAND_REMOVE, List.of(), command -> this.buildMode(command, ModifyMode.REMOVE));

        this.register(COMMAND_RESTORE, List.of(), command -> command
                .withFullDescription(DESCRIPTION_RESTORE.text())
                .withPermission(PERM_RESTORE.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.restoreHealth(sender, arguments);
                }));

        this.registerRoot("health", command -> command
                .withFullDescription(DESCRIPTION_ROOT.text())
                .withPermission(PERM_ROOT.getName()));
    }

    private void buildMode(final CommandAPICommand builder, final ModifyMode mode) {
        final TextLocale description = switch (mode) {
            case ADD -> DESCRIPTION_ADD;
            case SET -> DESCRIPTION_SET;
            case REMOVE -> DESCRIPTION_REMOVE;
        };

        final Permission permission = switch (mode) {
            case ADD -> PERM_ADD;
            case SET -> PERM_SET;
            case REMOVE -> PERM_REMOVE;
        };

        builder
                .withFullDescription(description.text())
                .withPermission(permission.getName())
                .withArguments(new DoubleArgument(CommandArgumentConstants.AMOUNT, 0D)
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(
                                info -> IntStream.range(0, 21).boxed().map(String::valueOf).toList())))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.changeHealth(sender, arguments, mode);
                });
    }

    private int changeHealth(final CommandSender sender, final CommandArguments arguments, final ModifyMode mode) {
        final Object amountArg = arguments.get(CommandArgumentConstants.AMOUNT);
        if (!(amountArg instanceof final Double amount)) {
            return 0;
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERM_OTHERS.getName(), (user, player) -> {
            if (player.isDead()) {
                (sender == player ? MESSAGE_DEAD_NOTIFY : MESSAGE_DEAD_FEEDBACK).message()
                        .send(sender, replacer -> replacer.replace(forPlayer(player)));
                return;
            }

            final double oldHealth = player.getHealth();
            final double maxHealth = EntityUtil.getAttributeValue(player, Attribute.MAX_HEALTH);
            final double newHealth = Math.clamp(mode.modify(oldHealth, amount), 0D, maxHealth);

            if (newHealth < oldHealth) {
                player.sendHurtAnimation(0f); // Play hurt animation.
            }

            player.setHealth(newHealth);

            final MessageLocale feedbackMessage = switch (mode) {
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
                this.module.sendPrefixed(feedbackMessage, sender,
                        builder -> builder
                                .with(GENERIC_OLD_VALUE, () -> NumberUtil.format(oldHealth))
                                .with(GENERIC_NEW_VALUE, () -> NumberUtil.format(player.getHealth()))
                                .with(GENERIC_AMOUNT, () -> NumberUtil.format(amount))
                                .with(GENERIC_CURRENT, () -> NumberUtil.format(player.getHealth())) // old
                                .with(GENERIC_MAX, () -> NumberUtil.format(maxHealth)) // old
                                .with(CommonPlaceholders.PLAYER.resolver(player)));
            }
            if (!target.silent()) {
                this.module.sendPrefixed(notifyMessage, player, builder -> builder
                        .with(GENERIC_OLD_VALUE, () -> NumberUtil.format(oldHealth))
                        .with(GENERIC_NEW_VALUE, () -> NumberUtil.format(player.getHealth()))
                        .with(GENERIC_AMOUNT, () -> NumberUtil.format(amount))
                        .with(GENERIC_CURRENT, () -> NumberUtil.format(player.getHealth())) // old
                        .with(GENERIC_MAX, () -> NumberUtil.format(maxHealth)) // old
                );
            }
        });
    }

    private int restoreHealth(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, PERM_OTHERS.getName(), (user, player) -> {
            final MessageLocale feedbackLocale;
            final MessageLocale notifyLocale;

            if (player.isDead()) {
                feedbackLocale = MESSAGE_REVIVE_FEEDBACK;
                notifyLocale = MESSAGE_REVIVE_NOTIFY;

                player.spigot().respawn();
            } else {
                feedbackLocale = MESSAGE_RESTORE_FEEDBACK;
                notifyLocale = MESSAGE_RESTORE_NOTIFY;

                final double maxHealth = EntityUtil.getAttributeValue(player, Attribute.MAX_HEALTH);

                player.setHealth(maxHealth);
                if (this.module.settings().healthClearEffects.get()) {
                    player.getActivePotionEffects().forEach(effect -> player
                            .removePotionEffect(effect.getType()));
                }
            }

            if (player != sender) {
                this.module.sendPrefixed(feedbackLocale, sender, builder -> builder.with(CommonPlaceholders.PLAYER
                        .resolver(player)));
            }
            if (!target.silent()) {
                this.module.sendPrefixed(notifyLocale, player);
            }
        });
    }
}