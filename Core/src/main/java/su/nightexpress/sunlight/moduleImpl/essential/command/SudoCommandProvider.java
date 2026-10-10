package su.nightexpress.sunlight.moduleImpl.essential.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

public class SudoCommandProvider extends CommandProvider<EssentialModule> {

    private static final String PERMISSION = EssentialPerms.COMMAND + ".sudo";

    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Sudo.Desc").text(
            "Force a player to run a command or chat.");

    public SudoCommandProvider(final EssentialModule module) {
        super(module, "sudo");
    }

    @Override
    public void setup() {
        this.register("sudo", command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> CommandArgumentConstants.onlinePlayerNames()),
                        new GreedyStringArgument("command"))
                .executes(this::execute));
    }

    private int execute(final CommandSender sender, final CommandArguments arguments) {
        String targetName = (String) arguments.get(CommandArgumentConstants.PLAYER);
        String input = (String) arguments.get("command");
        if (targetName == null || input == null || input.isBlank()) return 0;
        Player target = su.nightexpress.sunlight.utils.Utils.getPlayer(targetName);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        String trimmed = input.trim();
        if (trimmed.startsWith("/")) {
            this.module.plugin().getServer().dispatchCommand(target, trimmed.substring(1));
        } else {
            target.chat(trimmed);
        }
        return 1;
    }
}
