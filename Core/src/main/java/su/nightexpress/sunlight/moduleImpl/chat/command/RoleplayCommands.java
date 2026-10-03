package su.nightexpress.sunlight.moduleImpl.chat.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatLang;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatPerms;

public class RoleplayCommands extends CommandProvider<ChatModule> {

    public RoleplayCommands(ChatModule module) {
        super(module, "chat-roleplay");
    }

    @Override
    public void setup() {
        this.register("me", List.of(), command -> command
                .withFullDescription(ChatLang.COMMAND_ME_DESC.text())
                .withPermission(ChatPerms.COMMAND_ME.getName())
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(new GreedyStringArgument(CommandArgumentConstants.TEXT))
                .executes(this::showAction));
    }

    private int showAction(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            return 0;
        }
        final String text = (String) arguments.get(CommandArgumentConstants.TEXT);
        String format = this.module.getSettings().getRoleplayMeFormat();

        String formatted = PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_MESSAGE, () -> text)
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .build()
                .apply(format);

        this.module.plugin().getServer().getOnlinePlayers().forEach(other -> Players.sendMessage(other, formatted));
        return 1;
    }
}
