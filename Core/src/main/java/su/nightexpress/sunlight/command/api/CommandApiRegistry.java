package su.nightexpress.sunlight.command.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandAPIExecutor;
import dev.jorel.commandapi.commandsenders.AbstractCommandSender;
import dev.jorel.commandapi.executors.ExecutionInfo;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.integration.currency.EconomyBridge;
import su.nightexpress.nightcore.manager.SimpleManager;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandKey;
import su.nightexpress.sunlight.command.CommandSettings;
import su.nightexpress.sunlight.command.definitions.LiteralDefinition;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.EconomyUtils;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;

/**
 * Registry for the CommandAPI-based command system.
 * <p>
 * This is the successor of the legacy {@code CommandRegistry}: providers
 * declare the
 * same nodes, read the same {@code commands/<id>.yml} layout and get the same
 * cost
 * and cooldown semantics around their executors. It runs next to the legacy
 * registry
 * so modules can be migrated one at a time.
 */
public class CommandApiRegistry extends SimpleManager<SunLightPlugin> {

    private static final int SUCCESS = 1;
    private static final int FAILURE = 0;

    private final CommandSettings settings;

    private final Map<String, CommandApiProvider> providers;
    private final Map<String, Module> providerModules;
    private final Set<String> registeredCommands;

    public CommandApiRegistry(final SunLightPlugin plugin) {
        super(plugin);
        this.settings = new CommandSettings();
        this.providers = new HashMap<>();
        this.providerModules = new HashMap<>();
        this.registeredCommands = new HashSet<>();
    }

    @Override
    protected void onLoad() {
        this.settings.load(this.plugin.getConfig());
        this.registerCommands();
    }

    @Override
    protected void onShutdown() {
        this.registeredCommands.forEach(CommandAPI::unregister);
        this.registeredCommands.clear();
        this.providers.clear();
        this.providerModules.clear();
    }

    public void addProvider(final Module module, final CommandApiProvider provider) {
        if (module == null) {
            throw new IllegalArgumentException("Module cant be null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("Module's Command Provider cant be null");
        }
        this.providers.put(module.getId(), provider);
        this.providerModules.put(module.getId(), module);
    }

    public Collection<CommandApiProvider> getProviders() {
        return this.providers.values();
    }

    private void registerCommands() {
        this.providers.forEach((providerId, provider) -> {
            final FileConfig config = FileConfig.load(this.plugin.getDataFolder() + "/commands/",
                    FileConfig.withExtension(providerId));

            this.plugin.injectLang(provider);

            provider.registerDefaults();
            provider.load(config);

            final Module module = this.providerModules.get(providerId);

            provider.getNodeDefinitions().forEach((nodeId, definition) -> {
                if (!definition.enabled()) {
                    return;
                }

                final String[] aliases = sanitizeAliases(definition.aliases());
                if (aliases.length == 0) {
                    this.plugin.debug("Node '" + nodeId + "' of provider '" + providerId
                            + "' has no standalone command name and stays reachable as a sub-command only.");
                    return;
                }

                final CommandAPICommand command = this.buildNode(provider, providerId, nodeId, definition, module,
                        aliases[0], Arrays.copyOfRange(aliases, 1, aliases.length));
                command.register();
                this.registeredCommands.add(Utils.lowercase(aliases[0]));
            });

            provider.getRootDefinitions().forEach((rootId, definition) -> {
                if (!definition.enabled()) {
                    return;
                }

                // A hub has no standalone node, so unlike the literal nodes above it falls
                // back to its own id as command name. Providers commonly ship an empty
                // alias list for the root, and the legacy registry silently dropped the
                // whole command in that case.
                final String[] aliases = sanitizeAliases(definition.aliases());
                final String name;
                final String[] extraAliases;
                if (aliases.length > 0) {
                    name = aliases[0];
                    extraAliases = Arrays.copyOfRange(aliases, 1, aliases.length);
                } else {
                    name = rootId;
                    extraAliases = new String[0];
                }

                final CommandAPICommand root = new CommandAPICommand(name);
                if (extraAliases.length > 0) {
                    root.withAliases(extraAliases);
                }

                provider.getRootBuilders().get(rootId).accept(root);

                final List<CommandAPICommand> children = new ArrayList<>();
                definition.childrenAliases().forEach((nodeId, childAlias) -> {
                    final LiteralDefinition nodeDefinition = provider.getNodeDefinitions().get(nodeId);
                    if (nodeDefinition == null || !nodeDefinition.enabled()) {
                        return;
                    }
                    children.add(this.buildNode(provider, providerId, nodeId, nodeDefinition, module, childAlias,
                            sanitizeAliases(nodeDefinition.aliases())));
                });

                if (children.isEmpty()) {
                    this.plugin.warn("Root command '" + definition.name()
                            + "' was not registered due to no sub-commands available.");
                    return;
                }

                root.withSubcommands(children.toArray(new CommandAPICommand[0]));
                root.register();
                this.registeredCommands.add(Utils.lowercase(name));
            });

            config.saveChanges();
        });
    }

    private CommandAPICommand buildNode(final CommandApiProvider provider, final String providerId, final String nodeId,
            final LiteralDefinition definition, final Module module, final String name, final String[] aliases) {
        final CommandAPICommand command = new CommandAPICommand(name);
        if (aliases.length > 0) {
            command.withAliases(aliases);
        }

        provider.getNodeBuilders().get(nodeId).accept(command);

        final CommandAPIExecutor<CommandSender, AbstractCommandSender<? extends CommandSender>> original = command
                .getExecutor();
        if (original == null || !original.hasAnyExecutors()) {
            this.plugin.warn("Node '" + nodeId + "' of provider '" + providerId + "' has no executor, skipping.");
            return command;
        }

        command.setExecutor(
                new GuardedExecutor(original, this.settings, providerId, nodeId, definition, module));
        return command;
    }

    /**
     * Drops blank entries so an empty/blank alias can never be registered as a
     * (phantom) command.
     */
    private static String[] sanitizeAliases(final String[] aliases) {
        if (aliases == null) {
            return new String[0];
        }
        return Arrays.stream(aliases)
                .filter(alias -> alias != null && !alias.isBlank())
                .map(String::trim)
                .toArray(String[]::new);
    }

    /**
     * Runs the cost/cooldown gate and then the original executor.
     */
    private static final class GuardedExecutor
            extends CommandAPIExecutor<CommandSender, AbstractCommandSender<? extends CommandSender>> {

        private final CommandAPIExecutor<CommandSender, AbstractCommandSender<? extends CommandSender>> delegate;
        private final CommandSettings settings;
        private final CommandKey key;
        private final Module module;
        private final int cooldown;
        private final double cost;

        private GuardedExecutor(
                final CommandAPIExecutor<CommandSender, AbstractCommandSender<? extends CommandSender>> delegate,
                final CommandSettings settings, final String providerId,
                final String nodeId, final LiteralDefinition definition, final Module module) {
            this.delegate = delegate;
            this.settings = settings;
            this.key = new CommandKey(providerId, nodeId);
            this.module = module;
            this.cooldown = definition.cooldown();
            this.cost = definition.cost();
        }

        @Override
        public int execute(final ExecutionInfo<CommandSender, AbstractCommandSender<? extends CommandSender>> info)
                throws CommandSyntaxException {
            final Player player = info.sender() instanceof Player p ? p : null;
            if (this.check(info, player)) {
                return FAILURE;
            }

            final int result = this.delegate.execute(info);
            if (result < SUCCESS) {
                return FAILURE;
            }

            this.settle(info, player);
            return SUCCESS;
        }

        private boolean check(final ExecutionInfo<CommandSender, AbstractCommandSender<? extends CommandSender>> info,
                Player player) {
            if (player == null) {
                return true;
            }

            if (this.settings.isCooldownsEnabled() && this.cooldown != 0
                    && !EconomyUtils.hasCooldownBypass(player, this.module)) {
                final SunUser user = this.module.userManager().getOrFetch(player);
                final Long expireDate = user.getCommandCooldown(this.key);
                if (expireDate != null && !TimeUtil.isPassed(expireDate)) {
                    (expireDate < 0 ? Lang.COMMAND_COOLDOWN_ONE_TIME : Lang.COMMAND_COOLDOWN_DEFAULT).message()
                            .sendWith(player, replacer -> replacer
                                    .with(SLPlaceholders.GENERIC_TIME,
                                            () -> TimeFormats.formatDuration(expireDate, TimeFormatType.LITERAL))
                                    .with(SLPlaceholders.GENERIC_COMMAND, () -> "/" + info.args().fullInput()));
                    return false;
                }
            }

            if (this.mustCharge(player) && !EconomyUtils.canAfford(player, this.cost)) {
                Lang.COMMAND_COST_ERROR.message().sendWith(player, replacer -> replacer
                        .with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(this.cost))
                        .with(SLPlaceholders.GENERIC_COMMAND, () -> "/" + info.args().fullInput()));
                return false;
            }

            return true;
        }

        private boolean mustCharge(final Player player) {
            if (player == null || !this.settings.isCostsEnabled() || this.cost <= 0D) {
                return false;
            }
            return !EconomyUtils.hasBypass(player, this.module) && EconomyBridge.api().hasVaultCurrency();
        }

        /**
         * Charges the cost and starts the cooldown after a successful run.
         */
        private void settle(final ExecutionInfo<CommandSender, AbstractCommandSender<? extends CommandSender>> info,
                Player player) {
            if (player == null) {
                return;
            }

            if (this.mustCharge(player)) {
                EconomyUtils.withdraw(player, this.cost);
            }

            if (this.settings.isCooldownsEnabled() && this.cooldown != 0) {
                this.module.userManager().getOrFetch(player)
                        .setCommandCooldown(this.key, TimeUtil.createFutureTimestamp(this.cooldown));
            }
        }
    }
}
