package su.nightexpress.sunlight.command;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.util.Placeholders;
import su.nightexpress.sunlight.utils.Utils;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.user.SunUser;

public abstract class CommandProvider<T extends Module> implements LangContainer {

    protected final T module;
    protected String id;
    protected String usage;
    private final Map<String, Consumer<CommandAPICommand>> subCmdBuilders;
    private final Map<String, Consumer<CommandAPICommand>> cmdBuilders;

    public CommandProvider(final T module) {
        this.module = module;
        this.subCmdBuilders = new HashMap<>();
        this.cmdBuilders = new HashMap<>();
    }

    public abstract void setup();

    protected void register(final String id, final Consumer<CommandAPICommand> consumer) {
        this.subCmdBuilders.put(Utils.lowercase(id), consumer);
    }

    protected void registerRoot(final String id, final Consumer<CommandAPICommand> consumer) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Command Root cant be empty");
        }
        if (this.id != null && this.id != id) {
            throw new IllegalArgumentException("Command Root Id has already been set");
        }
        this.id = Utils.lowercase(id);
        this.cmdBuilders.put(id, consumer);
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getId() {
        return id;
    }

    public T getModule() {
        return module;
    }

    public Map<String, Consumer<CommandAPICommand>> getSubCommandBuilders() {
        return this.cmdBuilders;
    }

    public Map<String, Consumer<CommandAPICommand>> getCommandBuilders() {
        return this.subCmdBuilders;
    }

    protected boolean runForOnlinePlayerOrSender(final CommandSender sender, CommandArguments arguments,
            Function<Player, Boolean> consumer) {
        if (arguments.getOptional(CommandArgumentConstants.PLAYER).isEmpty() && !(sender instanceof Player)) {
            CoreLang.COMMAND_EXECUTION_MISSING_ARGUMENTS.withPrefix(module.plugin().getPrefix()).send(sender,
                    replacer -> replacer
                            .replace(Placeholders.GENERIC_COMMAND, usage));
            return false;
        }

        return this.runForOnlinePlayer(sender, arguments, consumer);
    }

    protected boolean runForOnlinePlayer(final CommandSender sender, CommandArguments arguments,
            Function<Player, Boolean> consumer) {
        String playerName = arguments.getOrDefaultUnchecked(CommandArgumentConstants.PLAYER, sender.getName());
        Player target = Utils.getPlayer(playerName);

        if (target == null || !this.canSee(sender, target)) {
            module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return false;
        }

        return consumer.apply(target);
    }

    protected boolean loadPlayerOrSenderWithDataAndRunInMainThread(final CommandSender sender,
            CommandArguments arguments,
            BiConsumer<SunUser, Player> consumer) {
        if (arguments.getOptional(CommandArgumentConstants.PLAYER).isEmpty() && !(sender instanceof Player)) {
            CoreLang.COMMAND_EXECUTION_MISSING_ARGUMENTS.withPrefix(module.plugin().getPrefix()).send(sender,
                    replacer -> replacer
                            .replace(Placeholders.GENERIC_COMMAND, usage));
            return false;
        }

        return this.loadPlayerWithDataAndRunInMainThread(sender, arguments, consumer);
    }

    protected boolean loadPlayerWithDataAndRunInMainThread(final CommandSender sender, CommandArguments arguments,
            BiConsumer<SunUser, Player> consumer) {

        String playerName = arguments.getOrDefaultUnchecked(CommandArgumentConstants.PLAYER, sender.getName());

        module.userManager().loadByNameAsync(playerName).thenCompose(userOptional -> {
            SunUser user = userOptional.orElse(null);
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

    protected boolean loadPlayerOrSenderAndRunInMainThread(final CommandSender sender, CommandArguments arguments,
            Consumer<Player> consumer) {
        if (arguments.getOptional(CommandArgumentConstants.PLAYER).isEmpty() && !(sender instanceof Player)) {
            CoreLang.COMMAND_EXECUTION_MISSING_ARGUMENTS.withPrefix(module.plugin().getPrefix()).send(sender,
                    replacer -> replacer
                            .replace(Placeholders.GENERIC_COMMAND, usage));
            return false;
        }

        return this.loadPlayerAndRunInMainThread(sender, arguments, consumer);
    }

    protected boolean loadPlayerAndRunInMainThread(final CommandSender sender, CommandArguments arguments,
            Consumer<Player> consumer) {
        String playerName = arguments.getOrDefaultUnchecked(CommandArgumentConstants.PLAYER, sender.getName());

        return this.loadPlayerAndRunInMainThread(sender, playerName, consumer);
    }

    protected boolean loadPlayerAndRunInMainThread(final CommandSender sender, String playerName,
            Consumer<Player> consumer) {
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

    protected boolean IsPlayer(final CommandSender sender, Player target) {
        return sender instanceof Player;
    }

    protected boolean canSee(final CommandSender sender, Player target) {
        return !(sender instanceof Player player) || player.canSee(target);
    }
}
