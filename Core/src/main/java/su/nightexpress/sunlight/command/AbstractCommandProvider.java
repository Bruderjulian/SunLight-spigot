package su.nightexpress.sunlight.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import dev.jorel.commandapi.CommandAPICommand;
import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.Utils;

/**
 * Declares a group of commands and the nodes they consist of.
 * <p>
 * Everything that is independent of a {@link su.nightexpress.sunlight.module.Module}
 * lives here: the plugin's own commands use this class directly, module commands go
 * through {@link CommandProvider}.
 */
public abstract class AbstractCommandProvider implements LangContainer {

    protected final SunLightPlugin plugin;
    protected String id;

    private final List<Node> nodes;

    protected AbstractCommandProvider(final SunLightPlugin plugin, final String id) {
        if (plugin == null) {
            throw new IllegalArgumentException("Command Provider needs a plugin");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Command Provider id cant be empty");
        }
        this.plugin = plugin;
        this.id = Utils.lowercase(id.trim());
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
        private List<String> aliases;
        private List<String> roots;

        private Node(final String id, final boolean root, final Consumer<CommandAPICommand> builder) {
            this.id = id;
            this.root = root;
            this.builder = builder;
            this.aliases = null;
            this.roots = null;
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
                if (alias.equals(this.id) || this.aliases != null && this.aliases.contains(alias)) {
                    continue;
                }
                if (this.aliases == null) {
                    this.aliases = new ArrayList<>();
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
                if (root.equals(this.id) || this.roots != null && this.roots.contains(root)) {
                    continue;
                }
                if (this.roots == null) {
                    this.roots = new ArrayList<>();
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
            return this.aliases == null ? List.of() : this.aliases;
        }

        public List<String> roots() {
            return this.roots == null ? List.of() : this.roots;
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
        final Node node = new Node(key, root, builder);
        this.nodes.add(node);
        return node;
    }

    public String getId() {
        return this.id;
    }

    public List<Node> getNodes() {
        return this.nodes;
    }

    public SunLightPlugin plugin() {
        return this.plugin;
    }

    public UserManager userManager() {
        return this.plugin.userManager();
    }

    /**
     * The node module-level bypass permissions hang off, e.g. {@code kits} for
     * {@code sunlight.kits.bypass.cost}.
     *
     * @return The namespace, or null for commands that belong to the plugin itself
     *         and have no module to bypass from.
     */
    @Nullable
    public String permissionNamespace() {
        return null;
    }
}