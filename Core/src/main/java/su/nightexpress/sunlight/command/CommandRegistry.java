package su.nightexpress.sunlight.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.NodeExecutor;
import su.nightexpress.nightcore.commands.builder.LiteralNodeBuilder;
import su.nightexpress.nightcore.commands.command.NightCommand;
import su.nightexpress.nightcore.commands.tree.LiteralNode;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.integration.currency.EconomyBridge;
import su.nightexpress.nightcore.manager.SimpleManager;
import su.nightexpress.nightcore.util.CommandUtil;
import su.nightexpress.nightcore.util.bridge.Software;
import su.nightexpress.sunlight.utils.Utils;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.definitions.HubDefinition;
import su.nightexpress.sunlight.command.definitions.LiteralDefinition;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.EconomyUtils;
import su.nightexpress.sunlight.utils.TimeUtil;

public class CommandRegistry extends SimpleManager<SunLightPlugin> {

    private final CommandSettings settings;

    private final Map<String, CommandProvider> providers;
    private final Map<String, Module> providerModules;
    private final Set<NightCommand> commands;

    public CommandRegistry(SunLightPlugin plugin) {
        super(plugin);
        this.settings = new CommandSettings();
        this.providers = new LinkedHashMap<>();
        this.providerModules = new HashMap<>();
        this.commands = new HashSet<>();
    }

    @Override
    protected void onLoad() {
        this.settings.load(this.plugin.getConfig());
        this.registerCommands();
    }

    @Override
    protected void onShutdown() {
        this.commands.forEach(this::unregister);
        this.commands.clear();
        this.providers.clear();
        this.providerModules.clear();
    }

    public void addProvider(String id, CommandProvider provider) {
        this.addProvider(id, provider, null);
    }

    public void addProvider(String id, CommandProvider provider, Module module) {
        String key = Utils.lowercase(id);
        this.providers.put(key, provider);

        if (module != null) {
            this.providerModules.put(key, module);
        }
    }

    private void registerCommands() {
        this.providers.forEach((providerId, provider) -> {
            FileConfig config = FileConfig.load(this.plugin.getDataFolder() + "/commands/",
                    FileConfig.withExtension(providerId));

            this.plugin.injectLang(provider); // Register and load command's locales.

            provider.registerDefaults();
            provider.load(config);

            Module module = this.providerModules.get(providerId);

            provider.getLiteralBuilders().forEach((nodeId, consumer) -> {
                LiteralDefinition literalDefinition = provider.getLiteralDefinitions().get(nodeId);
                if (literalDefinition == null || !literalDefinition.enabled())
                    return;

                String[] aliases = sanitizeAliases(literalDefinition.aliases());
                if (aliases.length == 0) {
                    this.plugin.warn("Command node '" + nodeId + "' in '" + providerId
                            + ".yml' was skipped: no valid aliases configured.");
                    return;
                }

                this.register(NightCommand.literal(this.plugin, aliases, builder -> {
                    consumer.accept(builder);

                    if (this.settings.isCostsEnabled()) {
                        this.wrapExecutorWithCost(providerId, nodeId, literalDefinition, builder, module);
                    }

                    if (this.settings.isCooldownsEnabled()) {
                        this.wrapExecutorWithCooldown(providerId, nodeId, literalDefinition, builder, module);
                    }
                }));
            });

            provider.getRootBuilders().forEach((rootId, rootBuilder) -> {
                HubDefinition rootDefinition = provider.getRootDefinitions().get(rootId);
                if (rootDefinition == null || !rootDefinition.enabled())
                    return;

                String[] aliases = sanitizeAliases(rootDefinition.aliases());
                if (aliases.length == 0) {
                    this.plugin.warn("Root command '" + rootDefinition.name() + "' in '" + providerId
                            + ".yml' was skipped: no valid aliases configured.");
                    return;
                }

                List<LiteralNode> childrens = new ArrayList<>();

                provider.getLiteralBuilders().forEach((nodeId, consumer) -> {
                    String alias = rootDefinition.childrenAliases().get(nodeId);
                    if (alias == null || alias.isBlank())
                        return;

                    childrens.add(Commands.literal(alias, consumer));
                });

                if (childrens.isEmpty()) {
                    this.plugin.warn("Root command '" + rootDefinition.name()
                            + "' was not registered due to no sub-commands available.");
                    return;
                }

                this.register(NightCommand.hub(this.plugin, aliases, builder -> {
                    rootBuilder.accept(builder);
                    builder.localized(rootDefinition.name());
                    builder.branch(childrens.toArray(new LiteralNode[0]));
                }));
            });

            config.saveChanges();
        });
    }

    /**
     * Drops blank entries so an empty/blank alias can never be registered as a
     * (phantom) Bukkit command. Empty arrays would also make NightCore throw.
     */
    private static String[] sanitizeAliases(String[] aliases) {
        if (aliases == null)
            return new String[0];

        return Arrays.stream(aliases).filter(alias -> alias != null && !alias.isBlank()).toArray(String[]::new);
    }

    private void register(NightCommand command) {
        // Always replace server built-in (vanilla/bukkit) commands, otherwise Bukkit
        // registers
        // ours with the fallback prefix (sunlight:label) and command.register() returns
        // false.
        this.unregisterConflicts(command, true);

        if (this.settings.isConflictUnregisterEnabled()) {
            this.unregisterConflicts(command, false);
        }

        // Bukkit registers the plain labels *and* the '<plugin>:<label>' forms in one go,
        // so '/sunlight:<cmd>' and '/sunlight:<alias>' are always available - no matter
        // whether the plain label was free or already taken by another command. A failed
        // plain registration is therefore not an error and must stay silent.
        command.register();

        this.commands.add(command);
    }

    private void unregister(NightCommand command) {
        this.dropPrefixedLabels(command);
        command.unregister();
    }

    /**
     * Removes the {@code <plugin>:<label>} entries of the given {@code command}.
     * <p>
     * Bukkit adds them next to the plain labels on every registration, but NightCore's
     * {@code unregister()} only drops the plain ones. Left behind, they keep pointing at
     * the dead command instance after a reload and still dispatch to it.
     */
    private void dropPrefixedLabels(NightCommand command) {
        Map<String, Command> knownCommands = Software.instance().getKnownCommands(Software.instance().getCommandMap());
        if (knownCommands.isEmpty()) {
            return;
        }

        String prefix = Utils.lowercase(this.plugin.getName()) + ':';
        Set<String> labels = new HashSet<>();
        labels.add(Utils.lowercase(command.getName()));
        command.getAliases().forEach(alias -> labels.add(Utils.lowercase(alias)));

        knownCommands.keySet().removeIf(key -> {
            String lower = Utils.lowercase(key);
            return lower.startsWith(prefix) && labels.contains(lower.substring(prefix.length()));
        });
    }

    public Set<CommandProvider> getProviders() {
        return new HashSet<>(this.providers.values());
    }

    private void wrapExecutorWithCost(String providerId, String nodeId, LiteralDefinition definition,
            LiteralNodeBuilder builder, Module module) {
        NodeExecutor executor = builder.getExecutor(); // Original executor set by the provider implementation.

        double cost = definition.cost();

        builder.executes((context, arguments) -> {
            Player player = context.getPlayer();

            boolean charge = cost > 0D && player != null && !EconomyUtils.hasBypass(player, module)
                    && EconomyBridge.api().hasVaultCurrency();

            if (charge && !EconomyUtils.canAfford(player, cost)) {
                Lang.COMMAND_COST_ERROR.message().sendWith(player, replacer -> replacer
                        .with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost))
                        .with(SLPlaceholders.GENERIC_COMMAND, () -> "/" + context.getInput()));
                return false;
            }

            boolean result = executor.run(context, arguments);
            if (result && charge) {
                EconomyUtils.withdraw(player, cost);
            }

            return result;
        });
    }

    private void wrapExecutorWithCooldown(String providerId, String nodeId, LiteralDefinition definition,
            LiteralNodeBuilder builder, Module module) {
        NodeExecutor executor = builder.getExecutor(); // Original executor set by the provider implementation.

        int cooldown = definition.cooldown();

        builder.executes((context, arguments) -> {
            Player player = context.getPlayer();
            SunUser user = player == null ? null : this.plugin.userManager().getOrFetch(player);
            CommandKey key = new CommandKey(providerId, nodeId);

            if (cooldown != 0 && user != null && !EconomyUtils.hasCooldownBypass(player, module)) {
                Long expireDate = user.getCommandCooldown(key);
                if (expireDate != null && !TimeUtil.isPassed(expireDate)) {
                    (expireDate < 0 ? Lang.COMMAND_COOLDOWN_ONE_TIME : Lang.COMMAND_COOLDOWN_DEFAULT).message()
                            .sendWith(player, replacer -> replacer
                                    .with(SLPlaceholders.GENERIC_TIME,
                                            () -> TimeFormats.formatDuration(expireDate, TimeFormatType.LITERAL))
                                    .with(SLPlaceholders.GENERIC_COMMAND, () -> "/" + context.getInput()));
                    return false;
                }
            }

            boolean result = executor.run(context, arguments);
            if (result && cooldown != 0 && user != null) {
                long expireDate = TimeUtil.createFutureTimestamp(cooldown);

                user.setCommandCooldown(key, expireDate);
            }

            return result;
        });
    }

    private void unregisterConflicts(NightCommand command, boolean vanillaOnly) {
        Set<String> aliases = new HashSet<>(command.getAliases());
        aliases.add(command.getName());

        aliases.forEach(alias -> {
            CommandUtil.getCommand(alias).ifPresent(other -> {
                // Skip our own commands (e.g. re-registration on reload).
                if (other instanceof PluginIdentifiableCommand identifiableCommand
                        && identifiableCommand.getPlugin().getName().equalsIgnoreCase(this.plugin.getName())) {
                    return;
                }

                String owner = getCommandOwner(other);
                boolean isVanilla = owner.equalsIgnoreCase("Vanilla") || owner.equalsIgnoreCase("Bukkit")
                        || owner.equalsIgnoreCase("Unknown") || owner.equalsIgnoreCase("Minecraft");

                // When vanillaOnly, leave third-party plugin commands alone.
                // They are handled only when 'Commands.Conflict-Unregister.Enabled' is true.
                if (vanillaOnly && !isVanilla) {
                    return;
                }

                if (this.settings.getConflictUnregisterBlacklist().contains(Utils.lowercase(owner))) {
                    return;
                }

                boolean result = CommandUtil.unregister(other);
                if (result) {
                    this.plugin.info("Unregistered conflicting '%s' (%s) command in favor of SunLight's alternative."
                            .formatted(other.getName(), owner));
                } else {
                    this.plugin.warn(
                            "Could not unregister conflicting command '%s' (%s) in favor of SunLight's alternative."
                                    .formatted(other.getName(), owner));
                }
            });
        });
    }

    private static String getCommandOwner(Command command) {
        if (command instanceof PluginIdentifiableCommand identifiableCommand) {
            return identifiableCommand.getPlugin().getName();
        }

        if (command instanceof BukkitCommand bukkitCommand) {
            String permission = bukkitCommand.getPermission();
            if (permission != null && (permission.startsWith("minecraft") || permission.startsWith("bukkit"))) {
                return "Vanilla";
            }

            // Paper wraps vanilla (brigadier) commands, they are not
            // PluginIdentifiableCommand.
            String className = command.getClass().getName().toLowerCase();
            if (className.contains("vanilla") || className.contains("minecraft") || className.contains("mojang")
                    || className.contains("brigadier")) {
                return "Vanilla";
            }

            return "Bukkit";
        }

        return "Unknown";
    }
}
