package su.nightexpress.sunlight.command;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.Utils;

/**
 * A provider for the commands of a single {@link Module}. Commands that belong to
 * the plugin itself use {@link AbstractCommandProvider} directly.
 */
public abstract class CommandProvider<T extends Module> extends AbstractCommandProvider {

    protected final T module;

    public CommandProvider(final T module, final String id) {
        super(module.plugin(), id);
        this.module = module;
    }

    @Override
    public String permissionNamespace() {
        return this.module.getPermissionNamespace();
    }

    protected boolean runForOnlinePlayer(final CommandSender sender, final CommandArguments arguments,
            final Function<Player, Boolean> consumer) {
        final String playerName = arguments.getOrDefaultUnchecked(CommandArgumentConstants.PLAYER, sender.getName());
        final Player target = Utils.getPlayer(playerName);

        if (target == null || !this.canSee(sender, target)) {
            module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return false;
        }

        return consumer.apply(target);
    }

    protected boolean loadPlayerWithDataAndRunInMainThread(final CommandSender sender, final CommandArguments arguments,
            final BiConsumer<SunUser, Player> consumer) {

        final String playerName = arguments.getOrDefaultUnchecked(CommandArgumentConstants.PLAYER, sender.getName());

        module.userManager().loadByNameAsync(playerName).thenCompose(userOptional -> {
            final SunUser user = userOptional.orElse(null);
            if (user == null) {
                module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return CompletableFuture.completedFuture(null);
            }

            return module.userManager().loadTargetPlayer(user).thenComposeAsync(target -> {
                if (target == null || !this.canSee(sender, target)) {
                    module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                    return CompletableFuture.completedFuture(null);
                }

                consumer.accept(user, target);

                return target.isOnline() ? CompletableFuture.completedFuture(null)
                        : CompletableFuture.runAsync(
                                target::saveData);

            }, runnable -> module.plugin().runTask(runnable));
        }).whenComplete(Utils::printStacktrace);

        return true;
    }

    protected boolean loadPlayerAndRunInMainThread(final CommandSender sender, final CommandArguments arguments,
            final Consumer<Player> consumer) {
        final String playerName = arguments.getOrDefaultUnchecked(CommandArgumentConstants.PLAYER, sender.getName());

        return this.loadPlayerAndRunInMainThread(sender, playerName, consumer);
    }

    protected boolean loadPlayerAndRunInMainThread(final CommandSender sender, final String playerName,
            final Consumer<Player> consumer) {
        module.userManager().loadTargetPlayer(playerName).thenComposeAsync(target -> {
            if (target == null || !this.canSee(sender, target)) {
                module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return CompletableFuture.completedFuture(null);
            }

            consumer.accept(target);

            return target.isOnline() ? CompletableFuture.completedFuture(null)
                    : CompletableFuture.runAsync(
                            target::saveData);

        }, runnable -> module.plugin().runTask(runnable)).whenComplete(Utils::printStacktrace);

        return true;
    }

    protected boolean canSee(final CommandSender sender, final Player target) {
        return !(sender instanceof final Player player) || player.canSee(target);
    }
}