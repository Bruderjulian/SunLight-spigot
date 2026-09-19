package su.nightexpress.sunlight.moduleImpl.essential.command;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

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
import su.nightexpress.sunlight.moduleImpl.essential.EssentialLang;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.user.UserManager;

public class GodCommandProvider extends AbstractCommandProvider {

    private final EssentialModule module;
    private final UserManager userManager;

    public GodCommandProvider(SunLightPlugin plugin, EssentialModule module, UserManager userManager) {
        super(plugin);
        this.module = module;
        this.userManager = userManager;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral("toggle", true, new String[] { "god" }, builder -> builder
                .description(EssentialLang.COMMAND_GOD_DESC)
                .permission(EssentialPerms.COMMAND_GOD)
                .withArguments(Arguments.playerName(CommandArguments.PLAYER).permission(EssentialPerms.COMMAND_GOD_OTHERS)
                        .optional())
                .withFlags(CommandArguments.FLAG_SILENT)
                .executes((context, arguments) -> this.toggleGod(context, arguments, ToggleMode.TOGGLE)));
    }

    private boolean toggleGod(CommandContext context, ParsedArguments arguments, ToggleMode mode) {
        return this.loadPlayerOrSenderWithDataAndRunInMainThread(context, arguments, this.module, this.userManager,
                (user, target) -> {
                    boolean state = mode.apply(user.getPropertyOrDefault(EssentialModule.GOD));
                    user.setProperty(EssentialModule.GOD, state);
                    user.markDirty();

                    if (state)
                        this.clearAggro(target);

                    if (context.getSender() != target) {
                        this.module.sendPrefixed(EssentialLang.GOD_TOGGLE_FEEDBACK, context.getSender(), builder -> builder
                                .with(CommonPlaceholders.PLAYER.resolver(target))
                                .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.getEnabledOrDisabled(state)));
                    }

                    if (!context.hasFlag(CommandArguments.FLAG_SILENT)) {
                        this.module.sendPrefixed(EssentialLang.GOD_TOGGLE_NOTIFY, target, builder -> builder
                                .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.getEnabledOrDisabled(state)));
                    }
                });
    }

    private void clearAggro(Player player) {
        player.getNearbyEntities(32D, 32D, 32D).forEach(entity -> {
            if (entity instanceof Mob mob && player.equals(mob.getTarget())) {
                mob.setTarget(null);
            }
        });
    }
}
