package su.nightexpress.sunlight.moduleImpl.chat.mail;

import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatLang;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatPerms;
import su.nightexpress.sunlight.utils.Utils;

public class MailCommandProvider extends CommandProvider<ChatModule> {

    private static final String COMMAND_SEND = "send";
    private static final String COMMAND_READ = "read";
    private static final String COMMAND_CLEAR = "clear";

    public MailCommandProvider(ChatModule module) {
        super(module, "chat-mail");
    }

    @Override
    public void setup() {
        this.register(COMMAND_SEND, command -> command
                .withFullDescription(ChatLang.COMMAND_MAIL_SEND_DESC.text())
                .withPermission(ChatPerms.COMMAND_MAIL_SEND)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> CommandArgumentConstants.onlinePlayerNames()),
                        new GreedyStringArgument(CommandArgumentConstants.TEXT))
                .executes(this::sendMail))
            .under("mail");

        this.register(COMMAND_READ, command -> command
                .withFullDescription(ChatLang.COMMAND_MAIL_READ_DESC.text())
                .withPermission(ChatPerms.COMMAND_MAIL_READ)
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::readMails))
            .under("mail");

        this.register(COMMAND_CLEAR, command -> command
                .withFullDescription(ChatLang.COMMAND_MAIL_CLEAR_DESC.text())
                .withPermission(ChatPerms.COMMAND_MAIL_CLEAR)
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::clearMails))
            .under("mail");

        this.registerRoot("mail", command -> command
                .withFullDescription(ChatLang.COMMAND_MAIL_ROOT_DESC.text())
                .withPermission(ChatPerms.COMMAND_MAIL_ROOT));
    }

    private int sendMail(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            return 0;
        }
        final String targetName = (String) arguments.get(CommandArgumentConstants.PLAYER);
        final String message = (String) arguments.get(CommandArgumentConstants.TEXT);
        if (message == null || message.isBlank())
            return 0;

        this.module.userManager().loadTargetProfile(targetName).thenCompose(profile -> {
            if (profile == null) {
                this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return CompletableFuture.completedFuture(null);
            }
            if (profile.id().equals(player.getUniqueId())) {
                this.module.sendPrefixed(ChatLang.MAIL_SEND_ERROR_SELF, player);
                return CompletableFuture.completedFuture(null);
            }
            this.module.sendMail(player, profile, message);
            return CompletableFuture.completedFuture(null);
        }).whenComplete(Utils::printStacktrace);

        return 1;
    }

    private int readMails(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            return 0;
        }
        this.module.readMails(player);
        return 1;
    }

    private int clearMails(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof Player player)) {
            return 0;
        }
        this.module.clearMails(player);
        return 1;
    }
}
