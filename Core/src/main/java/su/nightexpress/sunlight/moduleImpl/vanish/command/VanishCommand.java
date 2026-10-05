package su.nightexpress.sunlight.moduleImpl.vanish.command;


import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.vanish.VanishModule;
import su.nightexpress.sunlight.moduleImpl.vanish.config.VanishLang;
import su.nightexpress.sunlight.moduleImpl.vanish.config.VanishPerms;
import su.nightexpress.sunlight.user.property.UserProperty;

public class VanishCommand extends CommandProvider<VanishModule> {

    private static final String COMMAND_ROOT = "vanish";
    private static final String COMMAND_TOGGLE = "toggle";

    public VanishCommand(final VanishModule module) {
        super(module, "vanish");
    }

    @Override
    public void setup() {
        this.register(COMMAND_TOGGLE, command -> this.buildCommand(command, ToggleMode.TOGGLE))
                .under(COMMAND_ROOT);

        this.registerRoot(COMMAND_ROOT, command -> this.buildCommand(command, ToggleMode.TOGGLE))
                .aliases("v");
    }

    private void buildCommand(final CommandAPICommand command, final ToggleMode mode) {
        command
                .withFullDescription(VanishLang.COMMAND_VANISH_DESC.text())
                .withPermission(VanishPerms.COMMAND_VANISH)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleVanish(sender, arguments, mode);
                });
    }

    private int toggleVanish(final CommandSender sender, final CommandArguments arguments, final ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, VanishPerms.COMMAND_VANISH_OTHERS, (user, targetPlayer) -> {
            final UserProperty<Boolean> setting = VanishModule.VANISH;

            final boolean state = mode.apply(user.getPropertyOrDefault(setting));
            user.setProperty(setting, state);
            user.markDirty();

            module.vanish(targetPlayer, state);

            if (sender != targetPlayer) {
                VanishLang.COMMAND_VANISH_TARGET.message().send(sender, replacer -> replacer
                        .replace(SLPlaceholders.GENERIC_STATE, CoreLang.STATE_ENABLED_DISALBED.get(state))
                        .replace(SLPlaceholders.forPlayer(targetPlayer)));
            }

            if (!target.silent()) {
                VanishLang.COMMAND_VANISH_NOTIFY.message().send(targetPlayer, replacer -> replacer
                        .replace(SLPlaceholders.GENERIC_STATE, CoreLang.STATE_ENABLED_DISALBED.get(state)));
            }
        });
    }
}
