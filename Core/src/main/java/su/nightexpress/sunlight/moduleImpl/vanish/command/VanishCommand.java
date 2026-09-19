package su.nightexpress.sunlight.moduleImpl.vanish.command;

import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.core.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.vanish.VanishModule;
import su.nightexpress.sunlight.moduleImpl.vanish.config.VanishLang;
import su.nightexpress.sunlight.moduleImpl.vanish.config.VanishPerms;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.user.property.UserProperty;

public class VanishCommand extends CommandProvider {

    private static final String COMMAND_OFF = "off";
    private static final String COMMAND_ON = "on";
    private static final String COMMAND_TOGGLE = "toggle";

    private final VanishModule module;
    private final UserManager userManager;

    public VanishCommand(SunLightPlugin plugin, VanishModule module, UserManager userManager) {
        super(plugin);
        this.module = module;
        this.userManager = userManager;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_TOGGLE, true, new String[] { "vanish" }, builder -> builder
                .description(VanishLang.COMMAND_VANISH_DESC)
                .permission(VanishPerms.COMMAND_VANISH)
                .withArguments(Arguments.playerName(CommandArguments.PLAYER)
                        .permission(VanishPerms.COMMAND_VANISH_OTHERS).optional())
                .withFlags(CommandArguments.FLAG_SILENT)
                .executes((context, arguments) -> this.toggleVanish(context, arguments, ToggleMode.TOGGLE)));
    }

    private boolean toggleVanish(CommandContext context, ParsedArguments arguments, ToggleMode mode) {
        this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
                (user, target) -> {
                    UserProperty<Boolean> setting = VanishModule.VANISH;

                    boolean state = mode.apply(user.getPropertyOrDefault(setting));
                    user.setProperty(setting, state);
                    user.markDirty();

                    module.vanish(target, state);

                    if (context.getSender() != target) {
                        VanishLang.COMMAND_VANISH_TARGET.message().send(context.getSender(), replacer -> replacer
                                .replace(SLPlaceholders.GENERIC_STATE, CoreLang.getEnabledOrDisabled(state))
                                .replace(SLPlaceholders.forPlayer(target)));
                    }

                    if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                        VanishLang.COMMAND_VANISH_NOTIFY.message().send(target, replacer -> replacer
                                .replace(SLPlaceholders.GENERIC_STATE, CoreLang.getEnabledOrDisabled(state)));
                    }
                });

        return true;
    }
}
