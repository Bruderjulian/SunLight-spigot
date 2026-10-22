package su.nightexpress.sunlight.moduleImpl.voting.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingLang;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingPerms;

/**
 * Staff-facing guided creation. Opens the wizard menu that collects the key,
 * question, duration and choices without memorizing command syntax.
 */
public class VotingWizardCommandProvider extends CommandProvider<VotingModule> {

    private static final String COMMAND_WIZARD = "wizard";

    public VotingWizardCommandProvider(final VotingModule module) {
        super(module, "voting-wizard");
    }

    @Override
    public void setup() {
        this.register(COMMAND_WIZARD, builder -> builder
                .withFullDescription(VotingLang.COMMAND_WIZARD_DESC.text())
                .withPermission(VotingPerms.COMMAND_WIZARD)
                .withRequirement(sender -> sender instanceof Player)
                .executes(this::wizard))
                .under(VotingCommands.ROOT_NAME);
    }

    private int wizard(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }
        return this.module.openWizard(player) ? 1 : 0;
    }
}
