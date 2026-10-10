package su.nightexpress.sunlight.moduleImpl.rtp.command;


import org.bukkit.World;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.arguments.WorldArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.rtp.RTPModule;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPLang;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPPerms;

public class RTPCommandProvider extends CommandProvider<RTPModule> {

    public RTPCommandProvider(final RTPModule module) {
        super(module, "rtp");
    }

    @Override
    public void setup() {
        this.register("rtp", command -> command
                .withFullDescription(RTPLang.COMMAND_RTP_DESC.text())
                .withPermission(RTPPerms.COMMAND_RTP)
                .withOptionalArguments(new WorldArgument(CommandArgumentConstants.WORLD))
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::execute));
    }

    private int execute(final CommandSender sender, final CommandArguments arguments) {
        final World world = (World) arguments.get(CommandArgumentConstants.WORLD);
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, RTPPerms.COMMAND_RTP_OTHERS, (user, targetPlayer) -> {
            this.module.plugin().runTaskAsync(() -> {
                final boolean result;
                try {
                    result = this.module.getEngine().teleportToRandomPlace(targetPlayer, world);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    return;
                }

                if (result && sender != targetPlayer) {
                    this.module.plugin().runTask(task -> this.module.sendPrefixed(RTPLang.COMMAND_RTP_OTHERS_SUCCESS, sender,
                            builder -> builder.with(CommonPlaceholders.PLAYER.resolver(targetPlayer))));
                }
            });
        });
    }
}
