package su.nightexpress.sunlight.moduleImpl.nerfphantoms.command;


import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.PhantomsModule;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.PhantomsProperties;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.config.PhantomsLang;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.config.PhantomsPerms;

public class PhantomsCommandProvider extends CommandProvider<PhantomsModule> {

        private static final String COMMAND_ROOT = "phantoms";
        private static final String COMMAND_TOGGLE = "toggle";

        public PhantomsCommandProvider(final PhantomsModule module) {
                super(module, "nophantom");
        }

        @Override
        public void setup() {
                this.register(COMMAND_TOGGLE, command -> this.buildCommand(command, ToggleMode.TOGGLE))
                                .under(COMMAND_ROOT);

                this.registerRoot(COMMAND_ROOT, command -> this.buildCommand(command, ToggleMode.TOGGLE))
                                .aliases("nophantom", "phantom");
        }

        private void buildCommand(final CommandAPICommand command, final ToggleMode mode) {
                command
                                .withFullDescription(PhantomsLang.COMMAND_PHANTOMS_TOGGLE_DESC.text())
                                .withPermission(PhantomsPerms.COMMAND_PHANTOMS_TOGGLE)
                                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                                .executes((sender, arguments) -> {
                                        return this.togglePhantoms(sender, arguments, mode);
                                });
        }

        private int togglePhantoms(final CommandSender sender, final CommandArguments arguments,
                        final ToggleMode mode) {
                final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
                if (target == null) {
                        this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return 0;
                }

                return target.runAs(this.module, sender,
                                PhantomsPerms.COMMAND_PHANTOMS_TOGGLE_OTHERS,
                                (user, targetPlayer) -> {
                                        final boolean state = mode.apply(
                                                        user.getPropertyOrDefault(PhantomsProperties.ANTI_PHANTOM));
                                        user.setProperty(PhantomsProperties.ANTI_PHANTOM, state);
                                        user.markDirty();

                                        if (sender != targetPlayer) {
                                                this.module.sendPrefixed(PhantomsLang.COMMAND_NO_PHANTOM_TOGGLE_OTHERS,
                                                                sender,
                                                                builder -> builder
                                                                                .with(SLPlaceholders.GENERIC_STATE,
                                                                                                () -> CoreLang.STATE_ENABLED_DISALBED
                                                                                                                .get(state))
                                                                                .with(CommonPlaceholders.PLAYER
                                                                                                .resolver(targetPlayer)));
                                        }

                                        if (!target.silent()) {
                                                this.module.sendPrefixed(PhantomsLang.COMMAND_NO_PHANTOM_TOGGLE_NOTIFY,
                                                                targetPlayer,
                                                                replacer -> replacer
                                                                                .with(SLPlaceholders.GENERIC_STATE,
                                                                                                () -> CoreLang.STATE_ENABLED_DISALBED
                                                                                                                .get(state)));
                                        }
                                });
        }
}
