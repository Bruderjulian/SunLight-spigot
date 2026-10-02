package su.nightexpress.sunlight.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
 * This is the successor of the legacy {@code CommandRegistry}: providers
 * declare the
 * same nodes, read the same {@code commands/<id>.yml} layout and get the same
 * cost
 * and cooldown semantics around their executors. It runs next to the legacy
 * registry
 * so modules can be migrated one at a time.
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
  public void addProvider(final String id, final CommandProvider<?> provider) {
    if (provider == null) {
      throw new IllegalArgumentException("Module's Command Provider cant be null");
    }
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("Command Provider id cant be empty");
    }

    final String key = Utils.lowercase(id);
    for (final CommandProvider<?> entry : this.providers) {
      if (entry.id.equals(key)) {
        throw new IllegalStateException("Command Provider '" + key + "' has already been registered");
      }
    }

    this.providers.add(provider);
  }

  public void addProvider(final CommandProvider<?> provider) {
    this.addProvider(provider.getId(), provider);
  }

  public List<CommandProvider<?>> getProviders() {
    return this.providers;
  }

  private void registerCommands() {
    for (final CommandProvider<?> provider : providers) {
      final FileConfig config = FileConfig.load(this.plugin.getDataFolder() + "/commands/",
          FileConfig.withExtension(provider.id));

      this.plugin.injectLang(provider);
      provider.setup();

      if (provider.getCommandBuilders().isEmpty()) {
        return;
      }
      provider.getCommandBuilders().forEach((id, builder) -> {
        this.commands.put(id, buildCommand(provider, provider.id, config, builder, id));
      });
      config.saveChanges();
    }
  }

  private CommandAPICommand buildCommand(final CommandProvider<?> provider, final String cmdKey,
      final FileConfig config,
      final Consumer<CommandAPICommand> builder,
      final String path) {
    boolean enabled = true;
    String[] aliases = null;
    int cooldown = 0;
    double cost = 0;
    Set<String> subcommands = null;

    if (!config.contains(path)) {
      config.set(path + ".enabled", true);
      config.setStringArray(path + ".aliases", EMPTY);
      config.set(path + ".cooldown", 0);
      config.set(path + ".cost", 0);
      config.set(path + ".subcommands", EMPTY);
    } else {
      enabled = config.getBoolean(path + ".enabled", true);
      aliases = readAliases(config, path + ".aliases");
      cooldown = config.getInt(path + ".cooldown", 0);
      cost = config.getDouble(path + ".cost", 0);
      subcommands = config.getSection(path + ".subcommands");
    }

    if (!enabled) {
      return null;
    }
    final CommandAPICommand command = new CommandAPICommand(cmdKey);
    builder.accept(command);

    if (subcommands != null && subcommands.size() > 0) {
      for (final String key : subcommands) {
        final Consumer<CommandAPICommand> subBuilder = provider.getSubCommandBuilders().get(key);
        if (subBuilder == null) {
          continue;
        }
        command.withSubcommand(buildCommand(provider, key, config, subBuilder, path + ".subcommands." + key));
      }
    }

    final CommandAPIExecutor<CommandSender, AbstractCommandSender<? extends CommandSender>> original = command
        .getExecutor();
    if (original == null || !original.hasAnyExecutors()) {
      return command;
    }
    command.setExecutor(new GuardedExecutor(original, this.settings, provider, cmdKey, cooldown, cost));

    aliases = handleAliases(aliases);
    if (aliases != null) {
      command.withAliases(aliases);
    }
    this.unregisterConflict(cmdKey, true);
    if (this.settings.isConflictUnregisterEnabled()) {
      this.unregisterConflict(cmdKey, false);
    }
    CommandAPI.unregister(cmdKey, true);
    command.register("sunlight");
    return command;
  }

  private String[] readAliases(final FileConfig config, final String path) {
    final Object raw = config.get(path, EMPTY);

    final Stream<String> aliases;
    if (raw instanceof final List<?> list) {
      aliases = list.stream()
          .map(entry -> entry == null ? "" : entry.toString().trim())
          .filter(entry -> !entry.isEmpty());
    } else if (raw instanceof final String string) {
      String cleaned = string.trim();
      if (cleaned.startsWith("[") && cleaned.endsWith("]") && cleaned.length() >= 2) {
        cleaned = cleaned.substring(1, cleaned.length() - 1);
      }
      aliases = Arrays.stream(cleaned.split(","))
          .map(String::trim)
          .filter(entry -> !entry.isBlank() && !isListArtifact(entry));
    } else {
      return null;
    }
    return aliases.toArray(String[]::new);
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
        final CommandProvider<?> provider, final String nodeId, final int cooldown, final double cost) {
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

  private String[] handleAliases(String[] aliases) {
    if (aliases == null || aliases.length == 0) {
      return null;
    }
    aliases = Arrays.stream(aliases).filter(alias -> alias == null || alias.isEmpty()).map(alias -> alias.trim())
        .filter(alias -> alias.isEmpty()).toArray(String[]::new);
    if (aliases.length == 0) {
      return null;
    }
    for (String alias : aliases) {
      this.unregisterConflict(alias, true);
      if (this.settings.isConflictUnregisterEnabled()) {
        this.unregisterConflict(alias, false);
      }
    }
    return aliases;
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
