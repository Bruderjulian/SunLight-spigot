package su.nightexpress.sunlight.moduleImpl.chat.mail;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatLang;
import su.nightexpress.sunlight.moduleImpl.chat.core.ChatPerms;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MailCommandProvider extends CommandProvider {

    private static final String COMMAND_SEND = "send";
    private static final String COMMAND_READ = "read";
    private static final String COMMAND_CLEAR = "clear";

    private final ChatModule module;
    private final UserManager userManager;

    public MailCommandProvider(SunLightPlugin plugin, ChatModule module, UserManager userManager) {
        super(plugin);
        this.module = module;
        this.userManager = userManager;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_SEND, true, new String[] { "sendmail" }, builder -> builder
                .playerOnly()
                .description(ChatLang.COMMAND_MAIL_SEND_DESC)
                .permission(ChatPerms.COMMAND_MAIL_SEND)
                .withArguments(
                        Arguments.playerName(CommandArguments.PLAYER),
                        Arguments.greedyString(CommandArguments.TEXT).localized(Lang.COMMAND_ARGUMENT_NAME_TEXT))
                .executes(this::sendMail));

        this.registerLiteral(COMMAND_READ, true, new String[] { "readmail", "mails" }, builder -> builder
                .playerOnly()
                .description(ChatLang.COMMAND_MAIL_READ_DESC)
                .permission(ChatPerms.COMMAND_MAIL_READ)
                .executes(this::readMails));

        this.registerLiteral(COMMAND_CLEAR, true, new String[] { "clearmail", "deletemail" }, builder -> builder
                .playerOnly()
                .description(ChatLang.COMMAND_MAIL_CLEAR_DESC)
                .permission(ChatPerms.COMMAND_MAIL_CLEAR)
                .executes(this::clearMails));

        this.registerRoot("Mail", true, new String[] { "mail" },
                Map.of(
                        COMMAND_SEND, "send",
                        COMMAND_READ, "read",
                        COMMAND_CLEAR, "clear"),
                builder -> builder.description(ChatLang.COMMAND_MAIL_ROOT_DESC)
                        .permission(ChatPerms.COMMAND_MAIL_ROOT));
    }

    private boolean sendMail(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        String targetName = arguments.getString(CommandArguments.PLAYER);
        String message = arguments.getString(CommandArguments.TEXT);
        if (message.isBlank())
            return false;

        this.userManager.loadTargetProfile(targetName).thenCompose(profile -> {
            if (profile == null) {
                context.errorBadPlayer();
                return CompletableFuture.completedFuture(null);
            }
            if (profile.id().equals(player.getUniqueId())) {
                this.module.sendPrefixed(ChatLang.MAIL_SEND_ERROR_SELF, player);
                return CompletableFuture.completedFuture(null);
            }
            this.module.sendMail(player, profile, message);
            return CompletableFuture.completedFuture(null);
        }).whenComplete(Utils::printStacktrace);

        return true;
    }

    private boolean readMails(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        this.module.readMails(player);
        return true;
    }

    private boolean clearMails(CommandContext context, ParsedArguments arguments) {
        Player player = context.getPlayerOrThrow();
        this.module.clearMails(player);
        return true;
    }
}
