package su.nightexpress.sunlight.moduleImpl.chat.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.channel.ChatChannel;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatLang;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatPerms;

public class ChannelCommandsProvider extends CommandProvider<ChatModule> {

        private static final String ARG_CHANNEL = "channel";

        private static final String COMMAND_JOIN = "join";
        private static final String COMMAND_LEAVE = "leave";

        public ChannelCommandsProvider(ChatModule module) {
                super(module, "chat-channel");
        }

        @Override
        public void setup() {
                this.register(COMMAND_JOIN, List.of(), command -> command
                                .withFullDescription(ChatLang.COMMAND_CHANNEL_JOIN_DESC.text())
                                .withPermission(ChatPerms.COMMAND_CHANNEL_JOIN.getName())
                                .withRequirement(sender -> sender instanceof Player)
                                .withArguments(CommandArgumentConstants.string(ARG_CHANNEL,
                                                info -> this.module.getChannelRepository().getChannels().stream()
                                                                .map(ChatChannel::getId).sorted().toList()))
                                .executes(this::joinChannel));

                this.register(COMMAND_LEAVE, List.of(), command -> command
                                .withFullDescription(ChatLang.COMMAND_CHANNEL_LEAVE_DESC.text())
                                .withPermission(ChatPerms.COMMAND_CHANNEL_LEAVE.getName())
                                .withRequirement(sender -> sender instanceof Player)
                                .withArguments(CommandArgumentConstants.string(ARG_CHANNEL,
                                                info -> this.module.getChannelRepository().getChannels().stream()
                                                                .map(ChatChannel::getId).sorted().toList()))
                                .executes(this::leaveChannel));

                this.registerRoot("channel", command -> command
                                .withFullDescription(ChatLang.COMMAND_CHANNEL_ROOT_DESC.text())
                                .withPermission(ChatPerms.COMMAND_CHANNEL_ROOT.getName()));
        }

        private int joinChannel(CommandSender sender, CommandArguments arguments) {
                if (!(sender instanceof Player player)) {
                        return 0;
                }
                final String channelId = (String) arguments.get(ARG_CHANNEL);
                final ChatChannel channel = channelId == null ? null
                                : this.module.getChannelRepository().getById(channelId);
                if (channel == null) {
                        this.module.sendPrefixed(ChatLang.COMMAND_SYNTAX_INVALID_CHANNEL, sender);
                        return 0;
                }

                return this.module.joinChannel(player, channel) ? 1 : 0;
        }

        private int leaveChannel(CommandSender sender, CommandArguments arguments) {
                if (!(sender instanceof Player player)) {
                        return 0;
                }
                final String channelId = (String) arguments.get(ARG_CHANNEL);
                final ChatChannel channel = channelId == null ? null
                                : this.module.getChannelRepository().getById(channelId);
                if (channel == null) {
                        this.module.sendPrefixed(ChatLang.COMMAND_SYNTAX_INVALID_CHANNEL, sender);
                        return 0;
                }

                return this.module.leaveChannel(player, channel) ? 1 : 0;
        }
}
