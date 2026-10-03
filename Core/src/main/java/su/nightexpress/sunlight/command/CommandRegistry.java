package su.nightexpress.sunlight.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.defaults.BukkitCommand;
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
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.EconomyUtils;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;
import su.nightexpress.nightcore.util.CommandUtil;

/**
 * Registry for the CommandAPI-based command system.
 * <p>
 * Providers declare standalone nodes via
 * {@code register(id, aliases, roots, ...)} and hub roots via
 * {@code registerRoot(id, aliases, ...)}. A node listed under {@code roots}
 * is additionally attached as a subcommand (named by its node id) to each of
 * those roots, mirroring the legacy literal + hub layout. The
 * {@code commands/<provider>.yml} file keeps per-node {@code enabled} /
 * {@code aliases} / {@code cooldown} / {@code cost} entries plus the same
 * cost and cooldown semantics around executors.
 */
public class CommandRegistry extends SimpleManager<SunLightPlugin> {

  private static final int SUCCESS = 1;
  private static final int FAILURE = 0;
  private static final String[] EMPTY = new String[0];

  private final CommandSettings settings;

  private final List<CommandProvider<?>> providers;
  private final Map<String, CommandAPICommand> commands;

  public CommandRegistry(final SunLightPlugin plugin) {
    super(plugin);
    this.settings = new CommandSettings();
    this.providers = new ArrayList<>();
    this.commands = new HashMap<>();
  }

  @Override
  protected void onLoad() {
    this.settings.load(this.plugin.getConfig());
    this.registerCommands();
  }

  @Override
  protected void onShutdown() {
    this.commands.forEach((id, cmd) -> CommandAPI.unregister(id, true));
    this.commands.clear();
    this.providers.clear();
  }

  /**
   * Registers a provider under an explicit config id.
   *
   * @param id       The config namespace: the {@code commands/<id>.yml} file.
   * @param provider The provider.
   */
  public void addProvider(final CommandProvider<?> provider) {
    if (provider == null) {
      throw new IllegalArgumentException("Module's Command Provider cant be null");
    }
    for (final CommandProvider<?> entry : this.providers) {
      if (provider.id.equals(entry.getId())) {
        throw new IllegalStateException("Command Provider '" + provider.id + "' has already been registered");
      }
    }
    this.providers.add(provider);
  }

  public List<CommandProvider<?>> getProviders() {
    return this.providers;
  }

  private void registerCommands() {
    for (final CommandProvider<?> provider : providers) {
      final String providerId = provider.getId();
      final FileConfig config = FileConfig.load(this.plugin.getDataFolder() + "/commands/",
          FileConfig.withExtension(providerId));

      this.plugin.injectLang(provider);
      provider.setup();

      final Map<String, String> subRoots = provider.getSubRoots() == null ? Map.of() : provider.getSubRoots();

      // Hub roots first, with the subcommands that declared them as their root.
      final Map<String, CommandAPICommand> roots = new LinkedHashMap<>();
      final Set<String> attached = new HashSet<>();
      provider.getRootCommandBuilders().forEach((rootId, builder) -> {
        final CommandAPICommand root = buildCommand(provider, rootId, config, builder);
        if (root == null) {
          return;
        }
        roots.put(rootId, root);

        subRoots.forEach((nodeId, nodeRootId) -> {
          if (!nodeRootId.equals(rootId)) {
            return;
          }
          final Consumer<CommandAPICommand> nodeBuilder = provider.getSubCommandBuilders().get(nodeId);
          if (nodeBuilder == null) {
            return;
          }
          final CommandAPICommand node = buildCommand(provider, nodeId, config, nodeBuilder);
          if (node != null) {
            root.withSubcommand(node);
            attached.add(nodeId);
          }
        });
      });

      // Everything that is not a child of a root is a command of its own.
      provider.getSubCommandBuilders().forEach((nodeId, builder) -> {
        if (attached.contains(nodeId)) {
          return;
        }
        final CommandAPICommand node = buildCommand(provider, nodeId, config, builder);
        if (node != null) {
          this.registerCommand(node);
        }
      });

      roots.values().forEach(this::registerCommand);
      config.saveChanges();
    }
  }

  private CommandAPICommand buildCommand(final CommandProvider<?> provider, final String nodeId,
      final FileConfig config, final Consumer<CommandAPICommand> builder) {
    if (!config.contains(nodeId)) {
      config.set(nodeId + ".enabled", true);
      config.setStringArray(nodeId + ".aliases", EMPTY);
      config.set(nodeId + ".cooldown", 0);
      config.set(nodeId + ".cost", 0);
    }
    if (!config.getBoolean(nodeId + ".enabled", true)) {
      return null;
    }
    final String[] aliases = readAliases(config, nodeId + ".aliases");
    config.setStringArray(nodeId + ".aliases", aliases);

    final CommandAPICommand command = new CommandAPICommand(aliases.length == 0 ? nodeId : aliases[0]);
    builder.accept(command);
    if (aliases.length > 1) {
      command.withAliases(Arrays.copyOfRange(aliases, 1, aliases.length));
    }

    guard(provider, nodeId, command, config.getInt(nodeId + ".cooldown", 0),
        config.getDouble(nodeId + ".cost", 0));
    return command;
  }

  private void guard(final CommandProvider<?> provider, final String nodeId,
      final CommandAPICommand command, final int cooldown, final double cost) {
    final CommandAPIExecutor<CommandSender, AbstractCommandSender<? extends CommandSender>> original = command
        .getExecutor();
    if (original == null || !original.hasAnyExecutors()) {
      return;
    }
    command.setExecutor(new GuardedExecutor(original, this.settings, provider, nodeId, cooldown, cost));
  }

  private void registerCommand(final CommandAPICommand command) {
    final String name = command.getName();
    if (command.getAliases() != null) {
      for (final String alias : command.getAliases()) {
        this.unregisterConflict(alias, true);
        if (this.settings.isConflictUnregisterEnabled()) {
          this.unregisterConflict(alias, false);
        }
      }
    }
    this.unregisterConflict(name, true);
    if (this.settings.isConflictUnregisterEnabled()) {
      this.unregisterConflict(name, false);
    }
    CommandAPI.unregister(name, true);
    command.register("sunlight");
    this.commands.put(name, command);
  }

  private String[] readAliases(final FileConfig config, final String path) {
    final Object raw = config.get(path, EMPTY);

    final Stream<String> aliases;
    if (raw instanceof final List<?> list) {
      aliases = list.stream().map(entry -> entry == null ? "" : entry.toString());
    } else if (raw instanceof final String string) {
      String cleaned = string.trim();
      if (cleaned.startsWith("[") && cleaned.endsWith("]") && cleaned.length() >= 2) {
        cleaned = cleaned.substring(1, cleaned.length() - 1);
      }
      aliases = Arrays.stream(cleaned.split(","));
    } else {
      return EMPTY;
    }
    return aliases.filter(alias -> alias != null && !alias.isBlank())
        .map(String::trim)
        .filter(alias -> !isListArtifact(alias))
        .toArray(String[]::new);
  }

  private static boolean isListArtifact(final String alias) {
    return alias.equals("[]") || alias.equals("[") || alias.equals("]");
  }

  private static final class GuardedExecutor
      extends CommandAPIExecutor<CommandSender, AbstractCommandSender<? extends CommandSender>> {

    private final CommandAPIExecutor<CommandSender, AbstractCommandSender<? extends CommandSender>> delegate;
    private final CommandSettings settings;
    private final CommandKey key;
    private final CommandProvider<?> provider;
    private final int cooldown;
    private final double cost;

    private GuardedExecutor(
        final CommandAPIExecutor<CommandSender, AbstractCommandSender<? extends CommandSender>> delegate,
        final CommandSettings settings,
        final CommandProvider<?> provider, final String nodeId, final int cooldown,
        final double cost) {
      this.delegate = delegate;
      this.settings = settings;
      this.key = new CommandKey(provider.id, nodeId);
      this.provider = provider;
      this.cooldown = cooldown;
      this.cost = cost;
    }

    @Override
    public int execute(final ExecutionInfo<CommandSender, AbstractCommandSender<? extends CommandSender>> info)
        throws CommandSyntaxException {
      final Player player = info.sender() instanceof final Player p ? p : null;
      if (!this.check(info, player)) {
        return FAILURE;
      }

      final int result = this.delegate.execute(info);
      if (result < SUCCESS) {
        return FAILURE;
      }

      this.settle(info, player);
      return SUCCESS;
    }

    /**
     * @return {@code true} when the command may run.
     */
    private boolean check(final ExecutionInfo<CommandSender, AbstractCommandSender<? extends CommandSender>> info,
        final Player player) {
      if (player == null) {
        return true;
      }

      if (this.settings.isCooldownsEnabled() && this.cooldown != 0
          && !EconomyUtils.hasCooldownBypass(player, this.provider.module)) {
        final SunUser user = this.provider.module.userManager().getOrFetch(player);
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
      return !EconomyUtils.hasBypass(player, this.provider.module) && EconomyBridge.api().hasVaultCurrency();
    }

    /**
     * Charges the cost and starts the cooldown after a successful run.
     */
    private void settle(final ExecutionInfo<CommandSender, AbstractCommandSender<? extends CommandSender>> info,
        final Player player) {
      if (player == null) {
        return;
      }

      if (this.mustCharge(player)) {
        EconomyUtils.withdraw(player, this.cost);
      }

      if (this.settings.isCooldownsEnabled() && this.cooldown != 0) {
        this.provider.module.userManager().getOrFetch(player)
            .setCommandCooldown(this.key, TimeUtil.createFutureTimestamp(this.cooldown));
      }
    }
  }

  /**
   * Drops commands that would otherwise shadow SunLight's alternative.
   *
   * @param vanillaOnly When true, leave third-party plugin commands alone; they
   *                    are only
   *                    removed when {@code Commands.Conflict-Unregister.Enabled}
   *                    is on.
   */
  private void unregisterConflict(final String alias, final boolean vanillaOnly) {
    CommandUtil.getCommand(alias).ifPresent(other -> {
      // Skip our own commands, e.g. on a reload.
      if (other instanceof final PluginIdentifiableCommand identifiable
          && identifiable.getPlugin().getName().equalsIgnoreCase(this.plugin.getName())) {
        return;
      }

      final String owner = getCommandOwner(other);
      final boolean isVanilla = owner.equalsIgnoreCase("Vanilla") || owner.equalsIgnoreCase("Bukkit")
          || owner.equalsIgnoreCase("Unknown") || owner.equalsIgnoreCase("Minecraft");

      if (vanillaOnly && !isVanilla) {
        return;
      }
      if (this.settings.getConflictUnregisterBlacklist().contains(Utils.lowercase(owner))) {
        return;
      }

      if (CommandUtil.unregister(other)) {
        this.plugin.info("Unregistered conflicting '%s' (%s) command in favor of SunLight's alternative."
            .formatted(other.getName(), owner));
      } else {
        this.plugin.warn(
            "Could not unregister conflicting command '%s' (%s) in favor of SunLight's alternative."
                .formatted(other.getName(), owner));
      }
    });
  }

  private static String getCommandOwner(final Command command) {
    if (command instanceof final PluginIdentifiableCommand identifiable) {
      return identifiable.getPlugin().getName();
    }

    if (command instanceof final BukkitCommand bukkitCommand) {
      final String permission = bukkitCommand.getPermission();
      if (permission != null && (permission.startsWith("minecraft") || permission.startsWith("bukkit"))) {
        return "Vanilla";
      }

      // Paper wraps vanilla (Brigadier) commands; they are not
      // PluginIdentifiableCommand.
      final String className = command.getClass().getName().toLowerCase();
      if (className.contains("vanilla") || className.contains("minecraft") || className.contains("mojang")
          || className.contains("brigadier")) {
        return "Vanilla";
      }

      return "Bukkit";
    }

    return "Unknown";
  }
}
