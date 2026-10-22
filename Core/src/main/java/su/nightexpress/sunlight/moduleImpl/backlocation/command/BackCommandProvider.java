package su.nightexpress.sunlight.moduleImpl.backlocation.command;


import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.backlocation.BackLocationModule;
import su.nightexpress.sunlight.moduleImpl.backlocation.config.BackLocationLang;
import su.nightexpress.sunlight.moduleImpl.backlocation.config.BackLocationPerms;
import su.nightexpress.sunlight.moduleImpl.backlocation.data.LocationType;

public class BackCommandProvider extends CommandProvider<BackLocationModule> {

    public BackCommandProvider(final BackLocationModule module) {
        super(module, "back");
    }

    @Override
    public void setup() {
        this.register("back", command -> command
                .withFullDescription(BackLocationLang.COMMAND_BACK_DESC.text())
                .withPermission(BackLocationPerms.COMMAND_BACK)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::moveToPreviousLocation));
    }

    private int moveToPreviousLocation(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, BackLocationPerms.COMMAND_BACK_OTHERS,
                (user, targetPlayer) -> {
                    final boolean silent = target.silent();
                    if (!this.module.teleportToLocation(targetPlayer, LocationType.PREVIOUS, silent)) {
                        if (sender != targetPlayer) {
                            this.module.sendPrefixed(BackLocationLang.PREVIOUS_ERROR_NOTHING_FEEDBACK, sender,
                                    builder -> builder.andThen(SLPlaceholders.forPlayerWithPAPI(targetPlayer)));
                        }
                        return;
                    }

                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(BackLocationLang.PREVIOUS_TELEPORT_FEEDBACK, sender,
                                builder -> builder.andThen(SLPlaceholders.forPlayerWithPAPI(targetPlayer)));
                    }
                });
    }
}
