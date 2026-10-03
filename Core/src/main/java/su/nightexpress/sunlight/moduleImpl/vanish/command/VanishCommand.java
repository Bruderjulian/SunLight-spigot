package su.nightexpress.sunlight.moduleImpl.vanish.command;

import java.util.List;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.vanish.VanishModule;
import su.nightexpress.sunlight.moduleImpl.vanish.config.VanishLang;
import su.nightexpress.sunlight.moduleImpl.vanish.config.VanishPerms;
import su.nightexpress.sunlight.user.property.UserProperty;

public class VanishCommand extends CommandProvider<VanishModule> {

    private static final String COMMAND_TOGGLE = "toggle";

    public VanishCommand(final VanishModule module) {
        super(module);
    }

    @Override
    public void setup() {
        this.register(COMMAND_TOGGLE, new String[] { "vanish" }, List.of(), command -> command
                .withFullDescription(VanishLang.COMMAND_VANISH_DESC.text())
                .withPermission(VanishPerms.COMMAND_VANISH.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> this.toggleVanish(sender, arguments, ToggleMode.TOGGLE)));
    }

    private int toggleVanish(final CommandSender sender, final CommandArguments arguments, final ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, VanishPerms.COMMAND_VANISH_OTHERS.getName(), (user, targetPlayer) -> {
            final UserProperty<Boolean> setting = VanishModule.VANISH;

            final boolean state = mode.apply(user.getPropertyOrDefault(setting));
            user.setProperty(setting, state);
            user.markDirty();

            module.vanish(targetPlayer, state);

            if (sender != targetPlayer) {
                VanishLang.COMMAND_VANISH_TARGET.message().send(sender, replacer -> replacer
                        .replace(SLPlaceholders.GENERIC_STATE, CoreLang.getEnabledOrDisabled(state))
                        .replace(SLPlaceholders.forPlayer(targetPlayer)));
            }

            if (!target.silent()) {
                VanishLang.COMMAND_VANISH_NOTIFY.message().send(targetPlayer, replacer -> replacer
                        .replace(SLPlaceholders.GENERIC_STATE, CoreLang.getEnabledOrDisabled(state)));
            }
        });
    }
}
