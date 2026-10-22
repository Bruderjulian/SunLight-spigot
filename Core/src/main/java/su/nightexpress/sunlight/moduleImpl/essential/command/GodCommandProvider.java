package su.nightexpress.sunlight.moduleImpl.essential.command;


import org.bukkit.command.CommandSender;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialLang;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.core.CoreLang.getEnabledOrDisabled;

public class GodCommandProvider extends CommandProvider<EssentialModule> {

    private static final String COMMAND_ROOT = "god";

    public GodCommandProvider(final EssentialModule module) {
        super(module, "god");
    }

    @Override
    public void setup() {
        this.register("toggle", command -> this.buildCommand(command, ToggleMode.TOGGLE))
                .under(COMMAND_ROOT);

        this.registerRoot(COMMAND_ROOT, command -> this.buildCommand(command, ToggleMode.TOGGLE))
                .aliases("godmode");
    }

    private void buildCommand(final CommandAPICommand command, final ToggleMode mode) {
        command
                .withFullDescription(EssentialLang.COMMAND_GOD_DESC.text())
                .withPermission(EssentialPerms.COMMAND_GOD)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleGod(sender, arguments, mode);
                });
    }

    private int toggleGod(final CommandSender sender, final CommandArguments arguments, final ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, EssentialPerms.COMMAND_GOD_OTHERS, (user, player) -> {
            final boolean state = mode.apply(user.getPropertyOrDefault(EssentialModule.GOD));
            user.setProperty(EssentialModule.GOD, state);
            user.markDirty();

            if (state) {
                this.clearAggro(player);
            }

            if (sender != player) {
                this.module.sendPrefixed(EssentialLang.GOD_TOGGLE_FEEDBACK, sender, builder -> builder
                        .with(CommonPlaceholders.PLAYER.resolver(player))
                        .with(SLPlaceholders.GENERIC_STATE, () -> getEnabledOrDisabled(state)));
            }

            if (!target.silent()) {
                this.module.sendPrefixed(EssentialLang.GOD_TOGGLE_NOTIFY, player, builder -> builder
                        .with(SLPlaceholders.GENERIC_STATE, () -> getEnabledOrDisabled(state)));
            }
        });
    }

    private void clearAggro(final Player player) {
        player.getNearbyEntities(32D, 32D, 32D).forEach(entity -> {
            if (entity instanceof final Mob mob && player.equals(mob.getTarget())) {
                mob.setTarget(null);
            }
        });
    }
}