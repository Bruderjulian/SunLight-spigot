package su.nightexpress.sunlight.command;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import su.nightexpress.nightcore.commands.builder.HubNodeBuilder;
import su.nightexpress.nightcore.commands.builder.LiteralNodeBuilder;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.StringUtil;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.definitions.HubDefinition;
import su.nightexpress.sunlight.command.definitions.LiteralDefinition;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class CommandProvider implements LangContainer {

    protected final SunLightPlugin plugin;

    protected final Map<String, Consumer<LiteralNodeBuilder>> literalBuilders;
    protected final Map<String, Consumer<HubNodeBuilder>> rootBuilder;

    protected final Map<String, LiteralDefinition> defaultLiterals;
    protected final Map<String, HubDefinition> defaultRoot;

    protected final Map<String, LiteralDefinition> literals;
    protected final Map<String, HubDefinition> root;

    public CommandProvider(SunLightPlugin plugin) {
        this.plugin = plugin;
        this.literalBuilders = new HashMap<>();
        this.rootBuilder = new HashMap<>();
        this.defaultLiterals = new HashMap<>();
        this.defaultRoot = new HashMap<>();
        this.literals = new HashMap<>();
        this.root = new HashMap<>();
    }

    public void load(FileConfig config) {
        this.loadLiterals(config, "LiteralNodes");
        this.loadRoot(config, "RootNodes");
    }

    public abstract void registerDefaults();

    private void loadLiterals(FileConfig config, String path) {
        if (this.defaultLiterals.isEmpty())
            return;

        this.defaultLiterals.forEach((id, definition) -> {
            String defPath = path + "." + id;

            if (!config.contains(defPath)) {
                config.set(defPath + ".Enabled", definition.enabled());
                config.setStringArray(defPath + ".Aliases", definition.aliases());
                config.set(defPath + ".Cooldown", definition.cooldown());
                config.set(defPath + ".Cost", definition.cost());
            }
        });

        config.getSection(path).forEach(sId -> {
            if (!this.literalBuilders.containsKey(sId))
                return;

            String defPath = path + "." + sId;

            boolean enabled = config.getBoolean(defPath + ".Enabled");
            LiteralDefinition defaultDefinition = this.defaultLiterals.get(Utils.lowercase(sId));
            String[] aliases = readAliases(config, defPath + ".Aliases",
                    defaultDefinition == null ? new String[0] : defaultDefinition.aliases(), defPath);
            int cooldown = config.getInt(defPath + ".Cooldown");
            double cost = config.getDouble(defPath + ".Cost");

            this.literals.put(Utils.lowercase(sId), new LiteralDefinition(enabled, aliases, cooldown, cost));
        });
    }

    private void loadRoot(FileConfig config, String path) {
        if (this.defaultRoot.isEmpty())
            return;

        this.defaultRoot.forEach((id, definition) -> {
            String defPath = path + "." + id;

            if (!config.contains(defPath)) {
                config.set(defPath + ".Enabled", definition.enabled());
                config.setStringArray(defPath + ".Aliases", definition.aliases());
                config.set(defPath + ".Name", definition.name());
                definition.childrenAliases().forEach((childName, childAlias) -> config.set(defPath + ".Childrens." +
                        childName, childAlias));
            }
        });

        config.getSection(path).forEach(sId -> {
            if (!this.rootBuilder.containsKey(sId))
                return;

            String defPath = path + "." + sId;

            boolean enabled = config.getBoolean(defPath + ".Enabled");
            HubDefinition defaultDefinition = this.defaultRoot.get(Utils.lowercase(sId));
            String[] aliases = readAliases(config, defPath + ".Aliases",
                    defaultDefinition == null ? new String[0] : defaultDefinition.aliases(), defPath);
            String name = config.getString(defPath + ".Name", "null");

            Map<String, String> childrenAliases = new HashMap<>();
            config.getSection(defPath + ".Childrens").forEach(sId2 -> {
                String alias = config.getString(defPath + ".Childrens." + sId2);
                if (alias == null || alias.isBlank() || isListArtifact(alias))
                    return;

                if (!this.literalBuilders.containsKey(sId2))
                    return;

                childrenAliases.put(Utils.lowercase(sId2), alias.trim());
            });

            this.root.put(Utils.lowercase(sId), new HubDefinition(enabled, aliases, name, childrenAliases));
        });
    }

    /**
     * Reads command aliases from the config.
     * <p>
     * NightCore stores aliases as a single comma-separated string, but users (or
     * editors)
     * may write them as a YAML list instead. Bukkit's {@code getString()} coerces
     * such a list
     * via {@code toString()}, turning e.g. an empty list into the literal string
     * {@code "[]"}.
     * Splitting that produces a phantom command literally named {@code []} (visible
     * and
     * executable as {@code sunlight:[]}). This method reads the raw value to
     * support both
     * formats, repairs {@code "[...]"} coercion artifacts, drops blank entries, and
     * falls back
     * to (and rewrites) the defaults when nothing valid remains.
     *
     * @param config   The config to read from (and repair).
     * @param path     The aliases config path.
     * @param defaults The default aliases to restore when the configured value is
     *                 unusable.
     * @param defPath  The node path, used for warnings.
     * @return A non-null array of usable aliases (may be empty only when no
     *         defaults exist).
     */
    private String[] readAliases(FileConfig config, String path, String[] defaults, String defPath) {
        Object raw = config.get(path);

        List<String> aliases;
        if (raw instanceof List<?> list) {
            aliases = list.stream()
                    .map(entry -> entry == null ? "" : entry.toString().trim())
                    .filter(entry -> !entry.isBlank())
                    .toList();
        } else if (raw instanceof String string) {
            String cleaned = string.trim();
            // Repair YAML-list-to-string coercion artifacts like "[]" or "[a, b]".
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
            if (defaults.length == 0)
                return new String[0];

            this.plugin.warn("Command node '" + defPath + "' has no valid aliases. Restored defaults: "
                    + String.join(", ", defaults)
                    + ". To disable the command, set 'Enabled' to false instead of clearing 'Aliases'.");
            config.setStringArray(path, defaults);
            return defaults.clone();
        }

        return aliases.toArray(String[]::new);
    }

    private static boolean isListArtifact(String alias) {
        String entry = alias.trim();
        return entry.equals("[]") || entry.equals("[") || entry.equals("]");
    }

    protected void registerLiteral(String id, boolean enabled, String[] aliases,
            Consumer<LiteralNodeBuilder> consumer) {
        this.defaultLiterals.put(Utils.lowercase(id), new LiteralDefinition(enabled, aliases, 0, 0D));
        this.literalBuilders.put(Utils.lowercase(id), consumer);
    }

    protected void registerRoot(String name, boolean enabled, String[] aliases,
            Consumer<Map<String, String>> mapConsumer, Consumer<HubNodeBuilder> consumer) {
        Map<String, String> childrenAliases = new HashMap<>();
        mapConsumer.accept(childrenAliases);

        this.registerRoot(name, enabled, aliases, childrenAliases, consumer);
    }

    protected void registerRoot(String name, boolean enabled, String[] aliases, Map<String, String> childrenAliases,
            Consumer<HubNodeBuilder> consumer) {
        this.defaultRoot.put(Utils.lowercase(name), new HubDefinition(enabled, aliases, StringUtil
                .capitalizeUnderscored(name), childrenAliases));
        this.rootBuilder.put(Utils.lowercase(name), consumer);
    }

    public Map<String, HubDefinition> getRootDefinitions() {
        return this.root;
    }

    public Map<String, Consumer<HubNodeBuilder>> getRootBuilders() {
        return this.rootBuilder;
    }

    public Map<String, LiteralDefinition> getLiteralDefinitions() {
        return this.literals;
    }

    public Map<String, Consumer<LiteralNodeBuilder>> getLiteralBuilders() {
        return this.literalBuilders;
    }

    protected World getWorld(CommandContext context, ParsedArguments arguments, String argName) {
        if (arguments.contains(argName)) {
            return arguments.getWorld(argName);
        }

        if (!context.isPlayer()) {
            context.printUsage();
            return null;
        }

        return context.getPlayerOrThrow().getWorld();
    }

    protected boolean runForOnlinePlayerOrSender(CommandContext context, ParsedArguments arguments, Module module,
            Function<Player, Boolean> consumer) {
        if (!arguments.contains(CommandArguments.PLAYER) && !context.isPlayer()) {
            context.printUsage();
            return false;
        }

        return this.runForOnlinePlayer(context, arguments, module, consumer);
    }

    protected boolean runForOnlinePlayer(CommandContext context, ParsedArguments arguments, Module module,
            Function<Player, Boolean> consumer) {
        String playerName = arguments.getString(CommandArguments.PLAYER, context.getSender().getName());
        Player target = Utils.getPlayer(playerName);

        if (target == null || !this.canSee(context, target)) {
            module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, context.getSender());
            return false;
        }

        return consumer.apply(target);
    }

    protected boolean loadPlayerOrSenderWithDataAndRunInMainThread(CommandContext context, ParsedArguments arguments,
            Module module, UserManager userManager, BiConsumer<SunUser, Player> consumer) {
        if (!arguments.contains(CommandArguments.PLAYER) && !context.isPlayer()) {
            context.printUsage();
            return false;
        }

        return this.loadPlayerWithDataAndRunInMainThread(context, arguments, module, userManager, consumer);
    }

    protected boolean loadPlayerWithDataAndRunInMainThread(CommandContext context, ParsedArguments arguments,
            Module module, UserManager userManager, BiConsumer<SunUser, Player> consumer) {

        CommandSender sender = context.getSender();
        String playerName = arguments.getString(CommandArguments.PLAYER, sender.getName());

        userManager.loadByNameAsync(playerName).thenCompose(userOptional -> {
            SunUser user = userOptional.orElse(null);
            if (user == null) {
                module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return CompletableFuture.completedFuture(null);
            }

            return userManager.loadTargetPlayer(user).thenComposeAsync(target -> {
                if (target == null || !this.canSee(context, target)) {
                    module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                    return CompletableFuture.completedFuture(null);
                }

                consumer.accept(user, target);

                return target.isOnline() ? CompletableFuture.completedFuture(null)
                        : CompletableFuture.runAsync(
                                target::saveData);

            }, this.plugin::runTask);
        }).whenComplete(Utils::printStacktrace);

        return true;
    }

    protected boolean loadPlayerOrSenderAndRunInMainThread(CommandContext context, ParsedArguments arguments,
            Module module, UserManager userManager, Consumer<Player> consumer) {
        if (!arguments.contains(CommandArguments.PLAYER) && !context.isPlayer()) {
            context.printUsage();
            return false;
        }

        return this.loadPlayerAndRunInMainThread(context, arguments, module, userManager, consumer);
    }

    protected boolean loadPlayerAndRunInMainThread(CommandContext context, ParsedArguments arguments, Module module,
            UserManager userManager, Consumer<Player> consumer) {
        String playerName = arguments.getString(CommandArguments.PLAYER, context.getSender().getName());

        return this.loadPlayerAndRunInMainThread(context, playerName, module, userManager, consumer);
    }

    protected boolean loadPlayerAndRunInMainThread(CommandContext context, String playerName, Module module,
            UserManager userManager, Consumer<Player> consumer) {
        userManager.loadTargetPlayer(playerName).thenComposeAsync(target -> {
            if (target == null || !this.canSee(context, target)) {
                module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, context.getSender());
                return CompletableFuture.completedFuture(null);
            }

            consumer.accept(target);

            return target.isOnline() ? CompletableFuture.completedFuture(null)
                    : CompletableFuture.runAsync(
                            target::saveData);

        }, this.plugin::runTask).whenComplete(Utils::printStacktrace);

        return true;
    }

    protected boolean canSee(CommandContext context, Player target) {
        return !(context.getSender() instanceof Player sender) || sender.canSee(target);
    }
}
