package su.nightexpress.sunlight.command;

import java.util.ArrayList;
import java.util.List;
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
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.Utils;

public abstract class CommandProvider<T extends Module> implements LangContainer {

    protected final T module;
    protected String id;

    private final List<Node> nodes;

    public CommandProvider(final T module, final String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Command Provider id cant be empty");
        }
        this.id = Utils.lowercase(id.trim());
        this.module = module;
        this.nodes = new ArrayList<>();
    }

    public abstract void setup();

    /**
     * A command node declared by a provider.
     * <p>
     * A node is a standalone command by default. {@link #under(String...)}
     * additionally nests it
     * under the given roots, where it is reachable under its node id.
     */
    public static final class Node {

        private final String id;
        private final boolean root;
        private final Consumer<CommandAPICommand> builder;
        private final List<String> aliases;
        private final List<String> roots;

        private Node(final String id, final boolean root, final Consumer<CommandAPICommand> builder,
                final List<String> aliases, final List<String> roots) {
            this.id = id;
            this.root = root;
            this.builder = builder;
            this.aliases = aliases;
            this.roots = roots;
        }

        /**
         * Declares extra names for this command. The first one replaces the node id as
         * the primary
         * command name, the rest are registered as additional aliases. Written to a
         * fresh
         * {@code commands/<provider>.yml} and overridable there.
         */
        public Node aliases(final String... aliases) {
            for (String alias : aliases) {
                if (alias == null || alias.isBlank()) {
                    continue;
                }
                alias = Utils.lowercase(alias.trim());
                if (alias.equals(this.id) || this.aliases.contains(alias)) {
                    continue;
                }
                this.aliases.add(alias);
            }
            return this;
        }

        /**
         * Also makes this command available as a subcommand of the given roots. The
         * name used under
         * the root is always the node id; aliases only apply to the standalone command.
         */
        public Node under(final String... roots) {
            for (String root : roots) {
                if (root == null || root.isBlank()) {
                    continue;
                }
                root = Utils.lowercase(root.trim());
                if (root.equals(this.id) || this.roots.contains(root)) {
                    continue;
                }
                this.roots.add(root);
            }
            return this;
        }

        public String id() {
            return this.id;
        }

        public boolean isRoot() {
            return this.root;
        }

        public List<String> aliases() {
            return List.copyOf(this.aliases);
        }

        public List<String> roots() {
            return List.copyOf(this.roots);
        }

        Consumer<CommandAPICommand> builder() {
            return this.builder;
        }

        @Override
        public String toString() {
            return this.id;
        }
    }

    /**
     * Declares a subcommand. It becomes a command of its own unless it is nested
     * under a root with
     * {@link Node#under(String...)}.
     *
     * @param id      Node id: the config key, the name under each root, and the
     *                fallback
     *                standalone name when no alias resolves.
     * @param builder Configures the command (arguments, permission, executor).
     */
    protected Node register(final String id, final Consumer<CommandAPICommand> builder) {
        return this.addNode(id, false, builder);
    }

    /**
     * Declares a hub root. Roots are always registered as commands of their own and
     * collect the
     * subcommands that named them via {@link Node#under(String...)}.
     *
     * @param id      Node id: the config key and the fallback command name when no
     *                alias resolves.
     * @param builder Configures the command (arguments, permission, executor).
     */
    protected Node registerRoot(final String id, final Consumer<CommandAPICommand> builder) {
        return this.addNode(id, true, builder);
    }

    private Node addNode(final String id, final boolean root, final Consumer<CommandAPICommand> builder) {
        if (builder == null) {
            throw new IllegalArgumentException("Command Node '" + id + "' needs a builder");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Command Node id cant be empty");
        }

        final String key = Utils.lowercase(id.trim());
        final Node node = new Node(key, root, builder, List.of(), List.of());
        this.nodes.add(node);
        return node;
    }

    public String getId() {
        return this.id;
    }

    public List<Node> getNodes() {
        return this.nodes;
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
