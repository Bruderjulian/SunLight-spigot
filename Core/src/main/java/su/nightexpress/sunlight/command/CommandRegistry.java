package su.nightexpress.sunlight.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    for (final CommandProvider<?> provider : providers) {
      final String providerId = provider.getId();
      final FileConfig config = FileConfig.load(this.plugin.getDataFolder() + "/commands/",
          FileConfig.withExtension(providerId));

      this.plugin.injectLang(provider);
      provider.setup();

      // Hub roots first, so their subcommands can be attached before anything is
      // registered.
      final Map<String, CommandAPICommand> roots = new LinkedHashMap<>();
      for (final CommandProvider.Node node : provider.getNodes()) {
        if (!node.isRoot()) {
          continue;
        }

        final CommandAPICommand root = this.buildStandalone(provider, node, config);
        if (root != null) {
          roots.put(node.id(), root);
        }
      }
      for (final CommandProvider.Node node : provider.getNodes()) {
        if (node.isRoot()) {
          continue;
        }

        if (node.roots().isEmpty()) {
          // Never nested, so this node is a command of its own.
          this.register(this.buildStandalone(provider, node, config));
          continue;
        }

        for (final String rootId : node.roots()) {
          final CommandAPICommand root = roots.get(rootId);
          if (root == null) {
            // The root is disabled or unknown; the node stays unavailable rather than
            // silently leaking out as a global command.
            this.plugin.warn("Command '%s' of '%s' is attached to the unknown root '%s'."
                .formatted(node.id(), providerId, rootId));
            break;
          }
          final CommandAPICommand sub = this.buildSubCommand(provider, node, config);
          if (sub != null) {
            root.withSubcommand(sub);
          }
        }
      }
      roots.values().forEach(this::register);
      config.saveChanges();
    }
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

  private CommandAPICommand buildStandalone(final CommandProvider<?> provider, final CommandProvider.Node node,
      final FileConfig config) {
    final String nodeId = node.id();
    if (!config.contains(nodeId)) {
      config.set(nodeId + ".enabled", true);
      config.setStringArray(nodeId + ".aliases", node.aliases().toArray(EMPTY));
      config.set(nodeId + ".cooldown", 0);
      config.set(nodeId + ".cost", 0);
    } else if (!config.getBoolean(nodeId + ".enabled", true)) {
      return null;
    }

    String[] aliases = readAliases(config, nodeId + ".aliases");
    if (aliases.length == 0) {
      aliases = node.aliases().toArray(EMPTY);
    }

    final CommandAPICommand command = new CommandAPICommand(aliases.length == 0 ? nodeId : aliases[0]);
    node.builder().accept(command);
    if (aliases.length > 1) {
      command.withAliases(Arrays.copyOfRange(aliases, 1, aliases.length));
    }
    config.setStringArray(nodeId + ".aliases", aliases);

    this.guard(provider, nodeId, command, config.getInt(nodeId + ".cooldown", 0),
        config.getDouble(nodeId + ".cost", 0));
    return command;
  }

  private CommandAPICommand buildSubCommand(final CommandProvider<?> provider, final CommandProvider.Node node,
      final FileConfig config) {
    final String nodeId = node.id();
    if (!config.contains(nodeId)) {
      config.set(nodeId + ".enabled", true);
      config.set(nodeId + ".cooldown", 0);
      config.set(nodeId + ".cost", 0);
    } else if (!config.getBoolean(nodeId + ".enabled", true)) {
      return null;
    }

    // Subcommands are reached under their node id, so aliases and standalone cost
    // handling do not apply to them.
    final CommandAPICommand command = new CommandAPICommand(nodeId);
    node.builder().accept(command);

    this.guard(provider, nodeId, command, config.getInt(nodeId + ".cooldown", 0),
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

  private void register(final CommandAPICommand command) {
    if (command == null) {
      return;
    }

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
    try {
      CommandAPI.unregister(name, true);
      command.register("sunlight");
    } catch (final Exception e) {
      this.plugin.warn("Failed to register command '/%s': %s".formatted(name, e.getMessage()));
      e.printStackTrace();
      return;
    }

    final CommandAPICommand previous = this.commands.put(name, command);
    if (previous != null) {
      this.plugin.warn("Command '/%s' is declared more than once; the last declaration wins."
          .formatted(name));
    }
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
    public boolean hasAnyExecutors() {
      return true;
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
