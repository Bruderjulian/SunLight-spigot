package su.nightexpress.sunlight.module.extras.command;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.core.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.command.provider.type.AbstractCommandProvider;
import su.nightexpress.sunlight.module.extras.ExtrasModule;
import su.nightexpress.sunlight.module.extras.ExtrasProperties;
import su.nightexpress.sunlight.module.extras.config.ExtrasLang;
import su.nightexpress.sunlight.module.extras.config.ExtrasPerms;
import su.nightexpress.sunlight.user.UserManager;

public class GodCommandProvider extends AbstractCommandProvider {

    private final ExtrasModule module;
    private final UserManager  userManager;

    public GodCommandProvider(@NotNull SunLightPlugin plugin, @NotNull ExtrasModule module, @NotNull UserManager userManager) {
        super(plugin);
        this.module = module;
        this.userManager = userManager;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral("toggle", true, new String[]{"god"}, builder -> builder
            .description(ExtrasLang.COMMAND_GOD_DESC)
            .permission(ExtrasPerms.COMMAND_GOD)
            .withArguments(Arguments.playerName(CommandArguments.PLAYER).permission(ExtrasPerms.COMMAND_GOD_OTHERS).optional())
            .withFlags(CommandArguments.FLAG_SILENT)
            .executes((context, arguments) -> this.toggleGod(context, arguments, ToggleMode.TOGGLE))
        );
    }

    private boolean toggleGod(@NotNull CommandContext context, @NotNull ParsedArguments arguments, @NotNull ToggleMode mode) {
        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager, (user, target) -> {
            boolean state = mode.apply(user.getPropertyOrDefault(ExtrasProperties.GOD));
            user.setProperty(ExtrasProperties.GOD, state);
            user.markDirty();

            if (state) this.clearAggro(target);

            if (context.getSender() != target) {
                this.module.sendPrefixed(ExtrasLang.COMMAND_GOD_TARGET, context.getSender(), builder -> builder
                    .with(CommonPlaceholders.PLAYER.resolver(target))
                    .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.getEnabledOrDisabled(state))
                );
            }

            if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                this.module.sendPrefixed(ExtrasLang.COMMAND_GOD_NOTIFY, target, builder -> builder
                    .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.getEnabledOrDisabled(state))
                );
            }
        });
    }

    private void clearAggro(@NotNull Player player) {
        player.getNearbyEntities(32D, 32D, 32D).forEach(entity -> {
            if (entity instanceof Mob mob && player.equals(mob.getTarget())) {
                mob.setTarget(null);
            }
        });
    }
}
