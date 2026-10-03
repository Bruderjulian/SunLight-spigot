package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

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

    private static final String COMMAND_TOGGLE = "toggle";

    public GodCommandProvider(final EssentialModule module) {
        super(module, "god");
    }

    @Override
    public void setup() {
        this.register(COMMAND_TOGGLE, List.of(), command -> command
                .withFullDescription(EssentialLang.COMMAND_GOD_DESC.text())
                .withPermission(EssentialPerms.COMMAND_GOD.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggleGod(sender, arguments, ToggleMode.TOGGLE);
                }));
    }

    private int toggleGod(final CommandSender sender, final CommandArguments arguments, final ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, EssentialPerms.COMMAND_GOD_OTHERS.getName(), (user, player) -> {
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