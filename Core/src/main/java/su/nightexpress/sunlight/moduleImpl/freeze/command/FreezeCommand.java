package su.nightexpress.sunlight.moduleImpl.freeze.command;


import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.freeze.FreezeModule;
import su.nightexpress.sunlight.moduleImpl.freeze.config.FreezeLang;
import su.nightexpress.sunlight.moduleImpl.freeze.config.FreezePerms;
import su.nightexpress.sunlight.user.property.UserProperty;

public class FreezeCommand extends CommandProvider<FreezeModule> {

    private static final String COMMAND_ROOT = "freeze";
    private static final String COMMAND_TOGGLE = "toggle";
    private static final String COMMAND_ON = "on";
    private static final String COMMAND_OFF = "off";

    public FreezeCommand(final FreezeModule module) {
        super(module, "freeze");
    }

    @Override
    public void setup() {
        this.register(COMMAND_TOGGLE, command -> this.buildCommand(command, ToggleMode.TOGGLE))
                .under(COMMAND_ROOT);
        this.register(COMMAND_ON, command -> this.buildCommand(command, ToggleMode.ON))
                .under(COMMAND_ROOT);
        this.register(COMMAND_OFF, command -> this.buildCommand(command, ToggleMode.OFF))
                .under(COMMAND_ROOT);

        this.registerRoot(COMMAND_ROOT, command -> this.buildCommand(command, ToggleMode.TOGGLE))
                .aliases("freezes");
    }

    private void buildCommand(final CommandAPICommand command, final ToggleMode mode) {
        command
                .withFullDescription(FreezeLang.COMMAND_FREEZE_DESC.text())
                .withPermission(FreezePerms.COMMAND_FREEZE)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleFreeze(sender, arguments, mode);
                });
    }

    private int toggleFreeze(final CommandSender sender, final CommandArguments arguments, final ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, FreezePerms.COMMAND_FREEZE_OTHERS, (user, targetPlayer) -> {
            if (targetPlayer.hasPermission(FreezePerms.BYPASS_IMMUNE)
                    && !sender.hasPermission(FreezePerms.BYPASS_IMMUNE)) {
                this.module.sendPrefixed(FreezeLang.ERROR_IMMUNE, sender,
                        builder -> builder.with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
                return;
            }

            final UserProperty<Boolean> setting = FreezeModule.FROZEN;

            final boolean state = mode.apply(user.getPropertyOrDefault(setting));
            this.module.setFrozen(targetPlayer, state);

            if (sender != targetPlayer) {
                this.module.sendPrefixed(FreezeLang.COMMAND_FREEZE_TARGET, sender,
                        builder -> builder
                                .with(CommonPlaceholders.PLAYER.resolver(targetPlayer))
                                .with(SLPlaceholders.GENERIC_STATE,
                                        () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(FreezeLang.COMMAND_FREEZE_NOTIFY, targetPlayer, builder -> builder
                        .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
            }
        });
    }
}
