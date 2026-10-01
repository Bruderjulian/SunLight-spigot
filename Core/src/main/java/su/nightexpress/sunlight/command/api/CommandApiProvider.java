package su.nightexpress.sunlight.command.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dev.jorel.commandapi.CommandAPICommand;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.util.StringUtil;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.definitions.HubDefinition;
import su.nightexpress.sunlight.command.definitions.LiteralDefinition;
import su.nightexpress.sunlight.utils.Utils;

/**
 * Base class for providers registered in the {@link CommandApiRegistry}.
 * <p>
 * It mirrors the legacy {@code CommandProvider} contract, so a module declares
 * the
 * same nodes, aliases, cooldowns and costs and reads the very same
 * {@code commands/<id>.yml} layout. Only the tree construction differs: nodes
 * are
 * {@link CommandAPICommand} sub-commands instead of NightCore nodes.
 */
public abstract class CommandApiProvider implements LangContainer {

    protected final SunLightPlugin plugin;

    private final Map<String, Consumer<CommandAPICommand>> nodeBuilders;
    private final Map<String, LiteralDefinition> defaultNodes;

    private final Map<String, Consumer<CommandAPICommand>> rootBuilders;
    private final Map<String, HubDefinition> defaultRoots;

    private final Map<String, LiteralDefinition> nodes;
    private final Map<String, HubDefinition> roots;

    public CommandApiProvider(final SunLightPlugin plugin) {
        this.plugin = plugin;
        this.nodeBuilders = new HashMap<>();
        this.defaultNodes = new HashMap<>();
        this.rootBuilders = new HashMap<>();
        this.defaultRoots = new HashMap<>();
        this.nodes = new HashMap<>();
        this.roots = new HashMap<>();
    }

    public void load(final FileConfig config) {
        this.loadNodes(config, "LiteralNodes");
        this.loadRoots(config, "RootNodes");
    }

    public abstract void registerDefaults();

    private void loadNodes(final FileConfig config, final String path) {
        if (this.defaultNodes.isEmpty()) {
            return;
        }

        this.defaultNodes.forEach((id, definition) -> {
            final String defPath = path + "." + id;

            if (!config.contains(defPath)) {
                config.set(defPath + ".Enabled", definition.enabled());
                config.setStringArray(defPath + ".Aliases", definition.aliases());
                config.set(defPath + ".Cooldown", definition.cooldown());
                config.set(defPath + ".Cost", definition.cost());
            }
        });

        config.getSection(path).forEach(sId -> {
            if (!this.nodeBuilders.containsKey(sId)) {
                return;
            }

            final String defPath = path + "." + sId;
            final LiteralDefinition defaultDefinition = this.defaultNodes.get(Utils.lowercase(sId));

            this.nodes.put(Utils.lowercase(sId), new LiteralDefinition(
                    config.getBoolean(defPath + ".Enabled"),
                    this.readAliases(config, defPath + ".Aliases",
                            defaultDefinition == null ? new String[0] : defaultDefinition.aliases(), defPath, true),
                    config.getInt(defPath + ".Cooldown"),
                    config.getDouble(defPath + ".Cost")));
        });
    }

    private void loadRoots(final FileConfig config, final String path) {
        if (this.defaultRoots.isEmpty()) {
            return;
        }

        this.defaultRoots.forEach((id, definition) -> {
            final String defPath = path + "." + id;

            if (!config.contains(defPath)) {
                config.set(defPath + ".Enabled", definition.enabled());
                config.setStringArray(defPath + ".Aliases", definition.aliases());
                config.set(defPath + ".Name", definition.name());
                definition.childrenAliases().forEach((childName, childAlias) -> config
                        .set(defPath + ".Childrens." + childName, childAlias));
            }
        });

        config.getSection(path).forEach(sId -> {
            if (!this.rootBuilders.containsKey(sId)) {
                return;
            }

            final String defPath = path + "." + sId;
            final HubDefinition defaultDefinition = this.defaultRoots.get(Utils.lowercase(sId));

            final Map<String, String> childrenAliases = new HashMap<>();
            config.getSection(defPath + ".Childrens").forEach(childId -> {
                final String alias = config.getString(defPath + ".Childrens." + childId);
                if (alias == null || alias.isBlank() || isListArtifact(alias)) {
                    return;
                }
                if (!this.nodeBuilders.containsKey(childId)) {
                    return;
                }
                childrenAliases.put(Utils.lowercase(childId), alias.trim());
            });

            this.roots.put(Utils.lowercase(sId), new HubDefinition(
                    config.getBoolean(defPath + ".Enabled"),
                    this.readAliases(config, defPath + ".Aliases",
                            defaultDefinition == null ? new String[0] : defaultDefinition.aliases(), defPath, false),
                    config.getString(defPath + ".Name", defaultDefinition == null ? sId : defaultDefinition.name()),
                    childrenAliases));
        });
    }

    /**
     * Reads command aliases from the config.
     * <p>
     * NightCore stores aliases as a single comma-separated string, but users may
     * write them as a YAML list instead. Bukkit's {@code getString()} coerces such
     * a
     * list via {@code toString()}, turning e.g. an empty list into the literal
     * string
     * {@code "[]"}, which would register a phantom command named {@code []}. This
     * reads the raw value, supports both formats, repairs coercion artifacts and
     * drops
     * blank entries.
     * <p>
     * A literal node with no usable alias cannot be registered at all, so its
     * defaults
     * are restored (and the file rewritten) with a warning. Roots are different: an
     * empty alias list is a valid configuration because the registry then falls
     * back to
     * the root's own id as command name, so nothing is restored for them.
     */
    private String[] readAliases(final FileConfig config, final String path, final String[] defaults,
            final String defPath, final boolean restoreDefaults) {
        final Object raw = config.get(path);

        final List<String> aliases;
        if (raw instanceof List<?> list) {
            aliases = list.stream()
                    .map(entry -> entry == null ? "" : entry.toString().trim())
                    .filter(entry -> !entry.isBlank())
                    .toList();
        } else if (raw instanceof String string) {
            String cleaned = string.trim();
            if (cleaned.startsWith("[") && cleaned.endsWith("]") && cleaned.length() >= 2) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }
            aliases = Arrays.stream(cleaned.split(","))
                    .map(String::trim)
                    .filter(entry -> !entry.isBlank() && !isListArtifact(entry))
                    .toList();
        } else {
            aliases = List.of();
        }

        if (aliases.isEmpty()) {
            if (!restoreDefaults || defaults.length == 0) {
                return new String[0];
            }

            this.plugin.warn("Command node '" + defPath + "' has no valid aliases. Restored defaults: "
                    + String.join(", ", defaults)
                    + ". To disable the command, set 'Enabled' to false instead of clearing 'Aliases'.");
            config.setStringArray(path, defaults);
            return defaults.clone();
        }

        return aliases.toArray(String[]::new);
    }

    private static boolean isListArtifact(final String alias) {
        final String entry = alias.trim();
        return entry.equals("[]") || entry.equals("[") || entry.equals("]");
    }

    protected void registerNode(final String id, final boolean enabled, final String[] aliases,
            final Consumer<CommandAPICommand> consumer) {
        this.defaultNodes.put(Utils.lowercase(id), new LiteralDefinition(enabled, aliases, 0, 0D));
        this.nodeBuilders.put(Utils.lowercase(id), consumer);
    }

    protected void registerRoot(final String name, final boolean enabled, final String[] aliases,
            final Consumer<Map<String, String>> mapConsumer, final Consumer<CommandAPICommand> consumer) {
        final Map<String, String> childrenAliases = new HashMap<>();
        mapConsumer.accept(childrenAliases);
        this.registerRoot(name, enabled, aliases, childrenAliases, consumer);
    }

    protected void registerRoot(final String name, final boolean enabled, final String[] aliases,
            final Map<String, String> childrenAliases, final Consumer<CommandAPICommand> consumer) {
        this.defaultRoots.put(Utils.lowercase(name),
                new HubDefinition(enabled, aliases, StringUtil.capitalizeUnderscored(name), childrenAliases));
        this.rootBuilders.put(Utils.lowercase(name), consumer);
    }

    public Map<String, LiteralDefinition> getNodeDefinitions() {
        return this.nodes;
    }

    public Map<String, Consumer<CommandAPICommand>> getNodeBuilders() {
        return this.nodeBuilders;
    }

    public Map<String, HubDefinition> getRootDefinitions() {
        return this.roots;
    }

    public Map<String, Consumer<CommandAPICommand>> getRootBuilders() {
        return this.rootBuilders;
    }
}
