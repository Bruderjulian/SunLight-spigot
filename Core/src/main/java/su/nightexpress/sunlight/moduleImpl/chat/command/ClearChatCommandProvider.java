package su.nightexpress.sunlight.moduleImpl.chat.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SLUtils;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatLang;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatPerms;

import java.util.Collection;

public class ClearChatCommandProvider extends CommandProvider<ChatModule> {

    public ClearChatCommandProvider(ChatModule module) {
        super(module, "chat-clearchat");
    }

    @Override
    public void setup() {
        this.register("clearchat", List.of(), command -> command
                .withFullDescription(ChatLang.COMMAND_CLEAR_CHAT_DESC.text())
                .withPermission(ChatPerms.COMMAND_CLEARCHAT.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::clearChat));
    }

    private int clearChat(CommandSender sender, CommandArguments arguments) {
        Collection<? extends Player> players = this.module.plugin().getServer().getOnlinePlayers();

        for (int i = 0; i < 100; i++) {
            players.forEach(player -> player.sendMessage(" "));
        }

        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        final boolean silent = target != null && target.silent();

        if (!silent) {
            this.module.broadcastPrefixed(ChatLang.CLEAR_CHAT_NOTIFY, replacer -> replacer.with(
                    SLPlaceholders.GENERIC_NAME, () -> SLUtils.getSenderName(sender)));
        }
        return 1;
    }
}
