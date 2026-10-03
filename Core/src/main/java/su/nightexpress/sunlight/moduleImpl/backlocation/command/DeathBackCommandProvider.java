package su.nightexpress.sunlight.moduleImpl.backlocation.command;

import java.util.List;

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

public class DeathBackCommandProvider extends CommandProvider<BackLocationModule> {

    public DeathBackCommandProvider(final BackLocationModule module) {
        super(module, "deathback");
    }

    @Override
    public void setup() {
        this.register("deathback", List.of(), command -> command
                .withFullDescription(BackLocationLang.COMMAND_DEATH_BACK_DESC.text())
                .withPermission(BackLocationPerms.COMMAND_DEATHBACK.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::moveToDeathLocation));
    }

    private int moveToDeathLocation(final CommandSender sender, final CommandArguments arguments) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, BackLocationPerms.COMMAND_DEATHBACK_OTHERS.getName(),
                (user, targetPlayer) -> {
                    final boolean silent = target.silent();
                    if (!this.module.teleportToLocation(targetPlayer, LocationType.DEATH, silent)) {
                        if (sender != targetPlayer) {
                            this.module.sendPrefixed(BackLocationLang.DEATH_ERROR_NOTHING_FEEDBACK, sender,
                                    builder -> builder.andThen(SLPlaceholders.forPlayerWithPAPI(targetPlayer)));
                        }
                        return;
                    }

                    if (sender != targetPlayer) {
                        this.module.sendPrefixed(BackLocationLang.DEATH_TELEPORT_FEEDBACK, sender,
                                builder -> builder.andThen(SLPlaceholders.forPlayerWithPAPI(targetPlayer)));
                    }
                });
    }
}
