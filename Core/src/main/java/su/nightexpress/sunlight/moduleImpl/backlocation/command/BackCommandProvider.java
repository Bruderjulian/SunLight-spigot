package su.nightexpress.sunlight.moduleImpl.backlocation.command;

import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.provider.type.AbstractCommandProvider;
import su.nightexpress.sunlight.moduleImpl.backlocation.BackLocationModule;
import su.nightexpress.sunlight.moduleImpl.backlocation.config.BackLocationLang;
import su.nightexpress.sunlight.moduleImpl.backlocation.config.BackLocationPerms;
import su.nightexpress.sunlight.moduleImpl.backlocation.data.LocationType;
import su.nightexpress.sunlight.user.UserManager;

public class BackCommandProvider extends AbstractCommandProvider {

    private final BackLocationModule module;
    private final UserManager userManager;

    public BackCommandProvider(SunLightPlugin plugin, BackLocationModule module, UserManager userManager) {
        super(plugin);
        this.module = module;
        this.userManager = userManager;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral("back", true, new String[] { "back" }, builder -> builder
                .description(BackLocationLang.COMMAND_BACK_DESC)
                .permission(BackLocationPerms.COMMAND_BACK)
                .withArguments(Arguments.playerName(CommandArguments.PLAYER).optional()
                        .permission(BackLocationPerms.COMMAND_BACK_OTHERS))
                .withFlags(CommandArguments.FLAG_SILENT)
                .executes(this::moveToPreviousLocation));
    }

    private boolean moveToPreviousLocation(CommandContext context, ParsedArguments arguments) {
        return this.loadPlayerOrSenderAndRunInMainThread(context, arguments, this.module, this.userManager, target -> {

            boolean silent = context.hasFlag(CommandArguments.FLAG_SILENT);
            if (!this.module.teleportToLocation(target, LocationType.PREVIOUS, silent)) {
                if (context.getSender() != target) {
                    this.module.sendPrefixed(BackLocationLang.PREVIOUS_ERROR_NOTHING_FEEDBACK, context.getSender(),
                            builder -> builder.andThen(SLPlaceholders.forPlayerWithPAPI(target)));
                }
                return;
            }

            if (context.getSender() != target) {
                this.module.sendPrefixed(BackLocationLang.PREVIOUS_TELEPORT_FEEDBACK, context.getSender(),
                        builder -> builder.andThen(SLPlaceholders.forPlayerWithPAPI(target)));
            }
        });
    }
}
