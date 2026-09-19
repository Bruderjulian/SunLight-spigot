package su.nightexpress.sunlight.moduleImpl.freeze.command;

import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.builder.LiteralNodeBuilder;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.freeze.FreezeModule;
import su.nightexpress.sunlight.moduleImpl.freeze.config.FreezeLang;
import su.nightexpress.sunlight.moduleImpl.freeze.config.FreezePerms;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.user.property.UserProperty;

public class FreezeCommand extends CommandProvider {

    private static final String COMMAND_TOGGLE = "toggle";
    private static final String COMMAND_ON = "on";
    private static final String COMMAND_OFF = "off";

    private final FreezeModule module;
    private final UserManager userManager;

    public FreezeCommand(SunLightPlugin plugin, FreezeModule module, UserManager userManager) {
        super(plugin);
        this.module = module;
        this.userManager = userManager;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_TOGGLE, true, new String[] { "freeze" },
                builder -> this.buildCommand(builder, ToggleMode.TOGGLE));
        this.registerLiteral(COMMAND_ON, false, new String[] { "freeze-on" },
                builder -> this.buildCommand(builder, ToggleMode.ON));
        this.registerLiteral(COMMAND_OFF, false, new String[] { "freeze-off" },
                builder -> this.buildCommand(builder, ToggleMode.OFF));
    }

    private void buildCommand(LiteralNodeBuilder builder, ToggleMode mode) {
        builder.description(FreezeLang.COMMAND_FREEZE_DESC)
                .permission(FreezePerms.COMMAND_FREEZE)
                .withArguments(Arguments.playerName(CommandArguments.PLAYER)
                        .permission(FreezePerms.COMMAND_FREEZE_OTHERS).optional())
                .withFlags(CommandArguments.FLAG_SILENT)
                .executes((context, arguments) -> this.toggleFreeze(context, arguments, mode));
    }

    private boolean toggleFreeze(CommandContext context, ParsedArguments arguments, ToggleMode mode) {
        this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
                (user, target) -> {
                    if (target.hasPermission(FreezePerms.BYPASS_IMMUNE)
                            && !context.getSender().hasPermission(FreezePerms.BYPASS_IMMUNE)) {
                        this.module.sendPrefixed(FreezeLang.ERROR_IMMUNE, context.getSender(),
                                builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
                        return;
                    }

                    UserProperty<Boolean> setting = FreezeModule.FROZEN;

                    boolean state = mode.apply(user.getPropertyOrDefault(setting));
                    this.module.setFrozen(target, state);

                    if (context.getSender() != target) {
                        this.module.sendPrefixed(FreezeLang.COMMAND_FREEZE_TARGET, context.getSender(),
                                builder -> builder
                                        .with(CommonPlaceholders.PLAYER.resolver(target))
                                        .with(SLPlaceholders.GENERIC_STATE,
                                                () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
                    }

                    if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                        this.module.sendPrefixed(FreezeLang.COMMAND_FREEZE_NOTIFY, target, builder -> builder
                                .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
                    }
                });

        return true;
    }
}
