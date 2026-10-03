package su.nightexpress.sunlight.moduleImpl.chat.command;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

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

        public SpyCommandProvider(ChatModule module) {
                super(module, "chat-spy");
        }

        @Override
        public void setup() {
                this.register("logger", List.of(), command -> command
                                .withFullDescription(ChatLang.COMMAND_SPY_LOGGER_DESC.text())
                                .withPermission(ChatPerms.COMMAND_SPY_LOGGER.getName())
                                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.TYPE,
                                                info -> Arrays.stream(SpyType.values()).map(Enum::name).sorted()
                                                                .toList()),
                                                CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                                info -> CommandArgumentConstants.onlinePlayerNames()))
                                .executes((sender, arguments) -> {
                                        return this.toggleLogger(sender, arguments, ToggleMode.TOGGLE);
                                }));

                this.register("chatspy-toggle", List.of(),
                                command -> this.builderMode(command, SpyType.CHAT, ToggleMode.TOGGLE));
                this.register("chatspy-on", List.of(),
                                command -> this.builderMode(command, SpyType.CHAT, ToggleMode.ON));
                this.register("chatspy-off", List.of(),
                                command -> this.builderMode(command, SpyType.CHAT, ToggleMode.OFF));

                this.register("commandspy-toggle", List.of(),
                                command -> this.builderMode(command, SpyType.COMMAND, ToggleMode.TOGGLE));
                this.register("commandspy-on", List.of(),
                                command -> this.builderMode(command, SpyType.COMMAND, ToggleMode.ON));
                this.register("commandspy-off", List.of(),
                                command -> this.builderMode(command, SpyType.COMMAND, ToggleMode.OFF));

                this.register("socialspy-toggle", List.of(),
                                command -> this.builderMode(command, SpyType.SOCIAL, ToggleMode.TOGGLE));
                this.register("socialspy-on", List.of(),
                                command -> this.builderMode(command, SpyType.SOCIAL, ToggleMode.ON));
                this.register("socialspy-off", List.of(),
                                command -> this.builderMode(command, SpyType.SOCIAL, ToggleMode.OFF));
        }

        private void builderMode(dev.jorel.commandapi.CommandAPICommand builder, SpyType spyType, ToggleMode mode) {
                String description = switch (mode) {
                        case TOGGLE -> ChatLang.COMMAND_SPY_MODE_TOGGLE_DESC.text();
                        case ON -> ChatLang.COMMAND_SPY_MODE_ON_DESC.text();
                        case OFF -> ChatLang.COMMAND_SPY_MODE_OFF_DESC.text();
                };

                Permission permission = switch (spyType) {
                        case CHAT -> ChatPerms.COMMAND_SPY_CHAT;
                        case COMMAND -> ChatPerms.COMMAND_SPY_COMMAND;
                        case SOCIAL -> ChatPerms.COMMAND_SPY_SOCIAL;
                };

                builder
                                .withFullDescription(description.replace(SLPlaceholders.GENERIC_TYPE,
                                                ChatLang.SPY_TYPE.getLocalized(spyType)))
                                .withPermission(permission.getName())
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
                        case CHAT -> ChatPerms.COMMAND_SPY_CHAT_OTHERS.getName();
                        case COMMAND -> ChatPerms.COMMAND_SPY_COMMAND_OTHERS.getName();
                        case SOCIAL -> ChatPerms.COMMAND_SPY_SOCIAL_OTEHRS.getName();
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
