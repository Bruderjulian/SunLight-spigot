package su.nightexpress.sunlight.moduleImpl.chat.command;

import java.util.Arrays;

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
import su.nightexpress.sunlight.moduleImpl.chat.spy.SpyType;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.utils.Utils;

public class SpyCommandProvider extends CommandProvider<ChatModule> {

        private static final String COMMAND_ROOT = "spy";

        public SpyCommandProvider(ChatModule module) {
                super(module, "chat-spy");
        }

        @Override
        public void setup() {
                this.register("logger", command -> command
                                .withFullDescription(ChatLang.COMMAND_SPY_LOGGER_DESC.text())
                                .withPermission(ChatPerms.COMMAND_SPY_LOGGER)
                                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.TYPE,
                                                info -> Arrays.stream(SpyType.values()).map(Enum::name).sorted()
                                                                .toList()),
                                                CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                                info -> CommandArgumentConstants.onlinePlayerNames()))
                                .executes((sender, arguments) -> {
                                        return this.toggleLogger(sender, arguments, ToggleMode.TOGGLE);
                                }))
                                .aliases("logger")
                                .under(COMMAND_ROOT);

                this.register("chat-toggle",
                                command -> this.builderMode(command, SpyType.CHAT, ToggleMode.TOGGLE))
                                .aliases("chatspy-toggle").under(COMMAND_ROOT);
                this.register("chat-on",
                                command -> this.builderMode(command, SpyType.CHAT, ToggleMode.ON))
                                .aliases("chatspy-on").under(COMMAND_ROOT);
                this.register("chat-off",
                                command -> this.builderMode(command, SpyType.CHAT, ToggleMode.OFF))
                                .aliases("chatspy-off").under(COMMAND_ROOT);

                this.register("command-toggle",
                                command -> this.builderMode(command, SpyType.COMMAND, ToggleMode.TOGGLE))
                                .aliases("commandspy-toggle").under(COMMAND_ROOT);
                this.register("command-on",
                                command -> this.builderMode(command, SpyType.COMMAND, ToggleMode.ON))
                                .aliases("commandspy-on").under(COMMAND_ROOT);
                this.register("command-off",
                                command -> this.builderMode(command, SpyType.COMMAND, ToggleMode.OFF))
                                .aliases("commandspy-off").under(COMMAND_ROOT);

                this.register("social-toggle",
                                command -> this.builderMode(command, SpyType.SOCIAL, ToggleMode.TOGGLE))
                                .aliases("socialspy-toggle").under(COMMAND_ROOT);
                this.register("social-on",
                                command -> this.builderMode(command, SpyType.SOCIAL, ToggleMode.ON))
                                .aliases("socialspy-on").under(COMMAND_ROOT);
                this.register("social-off",
                                command -> this.builderMode(command, SpyType.SOCIAL, ToggleMode.OFF))
                                .aliases("socialspy-off").under(COMMAND_ROOT);

                this.registerRoot(COMMAND_ROOT, command -> command
                                .withFullDescription(ChatLang.COMMAND_SPY_ROOT_DESC.text())
                                .withPermission(ChatPerms.COMMAND_SPY_ROOT));
        }

        private void builderMode(dev.jorel.commandapi.CommandAPICommand builder, SpyType spyType, ToggleMode mode) {
                String description = switch (mode) {
                        case TOGGLE -> ChatLang.COMMAND_SPY_MODE_TOGGLE_DESC.text();
                        case ON -> ChatLang.COMMAND_SPY_MODE_ON_DESC.text();
                        case OFF -> ChatLang.COMMAND_SPY_MODE_OFF_DESC.text();
                };

                String permission = switch (spyType) {
                        case CHAT -> ChatPerms.COMMAND_SPY_CHAT;
                        case COMMAND -> ChatPerms.COMMAND_SPY_COMMAND;
                        case SOCIAL -> ChatPerms.COMMAND_SPY_SOCIAL;
                };

                builder
                                .withFullDescription(description.replace(SLPlaceholders.GENERIC_TYPE,
                                                ChatLang.SPY_TYPE.getLocalized(spyType)))
                                .withPermission(permission)
                                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                                .executes((sender, arguments) -> {
                                        return this.toggleMode(sender, arguments, spyType, mode);
                                });
        }

        private int toggleMode(CommandSender sender, CommandArguments arguments, SpyType spyType,
                        ToggleMode mode) {
                final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
                if (target == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                final String othersPermission = switch (spyType) {
                        case CHAT -> ChatPerms.COMMAND_SPY_CHAT_OTHERS;
                        case COMMAND -> ChatPerms.COMMAND_SPY_COMMAND_OTHERS;
                        case SOCIAL -> ChatPerms.COMMAND_SPY_SOCIAL_OTHERS;
                };

                return target.runAs(this.module, sender, othersPermission, (user, targetPlayer) -> {
                        UserProperty<Boolean> property = ChatProperties.getSpyInfoProperty(spyType);
                        boolean state = mode.apply(user.getPropertyOrDefault(property));

                        user.setProperty(property, state);
                        user.markDirty();

                        if (sender != targetPlayer) {
                                this.module.sendPrefixed(ChatLang.SPY_MODE_TOGGLE_FEEDBACK, sender,
                                                replacer -> replacer
                                                                .with(CommonPlaceholders.PLAYER
                                                                                .resolver(targetPlayer))
                                                                .with(SLPlaceholders.GENERIC_TYPE,
                                                                                () -> ChatLang.SPY_TYPE
                                                                                                .getLocalized(spyType))
                                                                .with(SLPlaceholders.GENERIC_STATE,
                                                                                () -> CoreLang.STATE_ENABLED_DISALBED
                                                                                                .get(state)));
                        }

                        if (!target.silent()) {
                                this.module.sendPrefixed(ChatLang.SPY_MODE_TOGGLE_NOTIFY, targetPlayer,
                                                replacer -> replacer
                                                                .with(SLPlaceholders.GENERIC_TYPE,
                                                                                () -> ChatLang.SPY_TYPE
                                                                                                .getLocalized(spyType))
                                                                .with(SLPlaceholders.GENERIC_STATE,
                                                                                () -> CoreLang.STATE_ENABLED_DISALBED
                                                                                                .get(state)));
                        }
                });
        }

        private int toggleLogger(CommandSender sender, CommandArguments arguments, ToggleMode mode) {
                final String typeRaw = (String) arguments.get(CommandArgumentConstants.TYPE);
                final SpyType spyType = typeRaw == null ? null : Utils.enumValueOf(typeRaw, SpyType.class);
                if (spyType == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                boolean dispatched = this.loadPlayerWithDataAndRunInMainThread(sender, arguments,
                                (user, target) -> {
                                        UserProperty<Boolean> property = ChatProperties.getSpyLogProperty(spyType);
                                        boolean state = mode.apply(user.getPropertyOrDefault(property));

                                        user.setProperty(property, state);
                                        user.markDirty();

                                        this.module.sendPrefixed(ChatLang.SPY_LOGGER_TOGGLE_FEEDBACK, sender,
                                                        replacer -> replacer
                                                                        .with(CommonPlaceholders.PLAYER
                                                                                        .resolver(target))
                                                                        .with(SLPlaceholders.GENERIC_TYPE,
                                                                                        () -> ChatLang.SPY_TYPE
                                                                                                        .getLocalized(spyType))
                                                                        .with(SLPlaceholders.GENERIC_STATE,
                                                                                        () -> CoreLang.STATE_ENABLED_DISALBED
                                                                                                        .get(state)));
                                });
                return dispatched ? 1 : 0;
        }
}
