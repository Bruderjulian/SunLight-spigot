package su.nightexpress.sunlight.moduleImpl.chat.command;

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.ChatProperties;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatLang;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatPerms;
import su.nightexpress.sunlight.utils.Utils;

public class ConversationCommandProvider extends CommandProvider<ChatModule> {

    private static final String COMMAND_MESSAGE = "message";
    private static final String COMMAND_REPLY = "reply";
    private static final String COMMAND_TOGGLE = "toggle";
    private static final String COMMAND_ON = "on";
    private static final String COMMAND_OFF = "off";

    public ConversationCommandProvider(ChatModule module) {
        super(module, "chat-conversations");
    }

    @Override
    public void setup() {
        this.register(COMMAND_MESSAGE, command -> command
                .withFullDescription(ChatLang.COMMAND_TELL_DESC.text())
                .withPermission(ChatPerms.COMMAND_TELL)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> CommandArgumentConstants.onlinePlayerNames()),
                        new GreedyStringArgument(CommandArgumentConstants.TEXT))
                .executes(this::sendConversationMessage)).aliases("msg", "tell")
            .under("conversations");

        this.register(COMMAND_REPLY, command -> command
                .withFullDescription(ChatLang.COMMAND_REPLY_DESC.text())
                .withPermission(ChatPerms.COMMAND_REPLY)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(new GreedyStringArgument(CommandArgumentConstants.TEXT))
                .executes(this::replyToConversation))
            .under("conversations");

        this.register(COMMAND_TOGGLE, command -> {
            this.buildToggleCommand(command, ChatLang.COMMAND_CONVERSATIONS_TOGGLE_DESC.text(), ToggleMode.TOGGLE);
        })
            .under("conversations");

        this.register(COMMAND_ON, command -> {
            this.buildToggleCommand(command, ChatLang.COMMAND_CONVERSATIONS_ON_DESC.text(), ToggleMode.ON);
        })
            .under("conversations");

        this.register(COMMAND_OFF, command -> {
            this.buildToggleCommand(command, ChatLang.COMMAND_CONVERSATIONS_OFF_DESC.text(), ToggleMode.OFF);
        })
            .under("conversations");

        this.registerRoot("conversations", command -> command
                .withFullDescription(ChatLang.COMMAND_CONVERSATIONS_ROOT_DESC.text())
                .withPermission(ChatPerms.COMMAND_CONVERSATIONS_ROOT)).aliases("convos");
    }

    private void buildToggleCommand(dev.jorel.commandapi.CommandAPICommand builder, String description,
            ToggleMode mode) {
        builder
                .withFullDescription(description)
                .withPermission(ChatPerms.COMMAND_CONVERSATIONS_TOGGLE)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleConversations(sender, arguments, mode);
                });
    }

    private int sendConversationMessage(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            return 0;
        }
        final String targetName = (String) arguments.get(CommandArgumentConstants.PLAYER);
        final String message = (String) arguments.get(CommandArgumentConstants.TEXT);
        Player target = targetName == null ? null : Utils.getPlayer(targetName);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        return this.module.sendPrivateMessage(player, target, message) ? 1 : 0;
    }

    private int replyToConversation(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            return 0;
        }

        UUID targetId = this.module.getChatCache(player).getLastConversationWith();
        if (targetId == null) {
            this.module.sendPrefixed(ChatLang.COMMAND_REPLY_ERROR_EMPTY, player);
            return 0;
        }

        Player target = this.module.plugin().getServer().getPlayer(targetId);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        final String message = (String) arguments.get(CommandArgumentConstants.TEXT);

        return this.module.sendPrivateMessage(player, target, message) ? 1 : 0;
    }

    private int toggleConversations(CommandSender sender, CommandArguments arguments,
            ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender,
                ChatPerms.COMMAND_CONVERSATIONS_TOGGLE_OTHERS, (user, targetPlayer) -> {
                    boolean state = mode.apply(user.getPropertyOrDefault(ChatProperties.CONVERSATIONS));

                    user.setProperty(ChatProperties.CONVERSATIONS, state);
                    user.markDirty();

                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(ChatLang.CONVERSATIONS_TOGGLE_FEEDBACK, sender, builder -> builder
                                .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state))
                                .with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                    }

                    if (!target.silent()) {
                        this.module.sendPrefixed(ChatLang.CONVERSATIONS_TOGGLE_NOTIFY, targetPlayer, builder -> builder
                                .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
                    }
                });
    }
}
