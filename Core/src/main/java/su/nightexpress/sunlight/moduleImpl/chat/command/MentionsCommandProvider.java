package su.nightexpress.sunlight.moduleImpl.chat.command;

import java.util.List;

import org.bukkit.command.CommandSender;

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

public class MentionsCommandProvider extends CommandProvider<ChatModule> {

    private static final String COMMAND_OFF = "off";
    private static final String COMMAND_ON = "on";
    private static final String COMMAND_TOGGLE = "toggle";

    public MentionsCommandProvider(ChatModule module) {
        super(module, "chat-mentions");
    }

    @Override
    public void setup() {
        this.register(COMMAND_TOGGLE, List.of(), command -> {
            this.buildToggleCommand(command, ChatLang.COMMAND_MENTIONS_TOGGLE_DESC.text(), ToggleMode.TOGGLE);
        });

        this.register(COMMAND_ON, List.of(), command -> {
            this.buildToggleCommand(command, ChatLang.COMMAND_MENTIONS_ON_DESC.text(), ToggleMode.ON);
        });

        this.register(COMMAND_OFF, List.of(), command -> {
            this.buildToggleCommand(command, ChatLang.COMMAND_MENTIONS_OFF_DESC.text(), ToggleMode.OFF);
        });

        this.registerRoot("mentions", command -> command
                .withFullDescription(ChatLang.COMMAND_MENTIONS_ROOT_DESC.text())
                .withPermission(ChatPerms.COMMAND_MENTIONS_ROOT.getName()));
    }

    private void buildToggleCommand(dev.jorel.commandapi.CommandAPICommand builder, String description,
            ToggleMode mode) {
        builder
                .withFullDescription(description)
                .withPermission(ChatPerms.COMMAND_MENTIONS_TOGGLE.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleMentions(sender, arguments, mode);
                });
    }

    private int toggleMentions(CommandSender sender, CommandArguments arguments,
            ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender,
                ChatPerms.COMMAND_MENTIONS_TOGGLE_OTHERS.getName(), (user, targetPlayer) -> {
                    boolean state = mode.apply(user.getPropertyOrDefault(ChatProperties.MENTIONS));

                    user.setProperty(ChatProperties.MENTIONS, state);
                    user.markDirty();

                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(ChatLang.MENTIONS_TOGGLE_FEEDBACK, sender, builder -> builder
                                .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state))
                                .with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                    }

                    if (!target.silent()) {
                        this.module.sendPrefixed(ChatLang.MENTIONS_TOGGLE_NOTIFY, targetPlayer, builder -> builder
                                .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
                    }
                });
    }
}
