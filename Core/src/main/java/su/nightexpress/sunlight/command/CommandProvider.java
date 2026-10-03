package su.nightexpress.sunlight.command;

import java.util.HashMap;
import java.util.List;
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
    private final Map<String, Consumer<CommandAPICommand>> rootBuilders;
    private Map<String, String> subRoots;

    public CommandProvider(final T module, final String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Command Provider id cant be empty");
        }
        this.id = Utils.lowercase(id.trim());
        this.module = module;
        this.subCmdBuilders = new HashMap<>();
        this.rootBuilders = new HashMap<>();
        this.subRoots = null;
    }

    public abstract void setup();

    protected void register(final String id, final Consumer<CommandAPICommand> consumer) {
        this.register(id, List.of(), consumer);
    }

    /**
     * Declares a standalone command that is additionally attached as a
     * subcommand (named {@code id}) to each of the given root commands.
     * <p>
     * This mirrors the legacy system where a literal had standalone aliases
     * and additionally appeared as a child of hub roots.
     *
     * @param id       The node id: config key, subcommand name under each root,
     *                 and standalone name fallback when no aliases resolve.
     * @param aliases  Default standalone aliases written to a fresh config.
     * @param roots    Root command ids this node is attached to.
     * @param enabled  Default for the {@code enabled} config flag.
     * @param consumer Configures the command (arguments, permission, executor).
     */
    protected void register(final String id, final List<String> roots, final Consumer<CommandAPICommand> consumer) {
        final String key = Utils.lowercase(id);
        this.subCmdBuilders.put(key, consumer);
        if (roots == null || roots.isEmpty()) {
            return;
        }
        if (subRoots == null) {
            this.subRoots = new HashMap<>();
        }
        for (String root : roots) {
            if (root == null || root.isBlank()) {
                continue;
            }
            root = Utils.lowercase(root.trim());
            if (root.equals(key)) {
                continue;
            }
            this.subRoots.put(key, root);
        }
    }

    protected void registerRoot(final String id, final Consumer<CommandAPICommand> consumer) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Command Root cant be empty");
        }
        this.rootBuilders.put(Utils.lowercase(id.trim()), consumer);
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
        return this.subCmdBuilders;
    }

    public Map<String, Consumer<CommandAPICommand>> getRootCommandBuilders() {
        return this.rootBuilders;
    }

    public Map<String, String> getSubRoots() {
        return this.subRoots;
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
