package su.nightexpress.sunlight.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.utils.Utils;

public class ModuleManager {
  private final SunLightPlugin plugin;
  private final FileConfig config;

  private final List<ModuleDefinition<?>> definitionMap;
  private final Map<String, Module> byId;
  private final Map<Class<?>, Module> byType;

  public ModuleManager(final SunLightPlugin plugin) {
    this.plugin = plugin;
    this.config = FileConfig.load(this.plugin.getDataFolder().getAbsolutePath(), "modules.yml");

    this.definitionMap = new ArrayList<>();

    this.byId = new HashMap<>();
    this.byType = new HashMap<>();
  }

  public void clear() {
    for (final Module module : this.byType.values()) {
      module.shutdown();
    }
    this.byId.clear();
    this.byType.clear();
  }

  public void reload() {
    for (final Module module : this.byType.values()) {
      module.setup();
    }
  }

  public Set<Module> getModules() {
    return Set.copyOf(this.byType.values());
  }

  public boolean isPresent(final String id) {
    return this.byId.containsKey(Utils.lowercase(id));
  }

  public <T extends Module> boolean isPresent(final Class<T> type) {
    return this.byType.containsKey(type);
  }

  public Module getById(final String id) {
    return this.byId.get(Utils.lowercase(id));
  }

  public Optional<Module> getByIdOptional(final String id) {
    return Optional.ofNullable(this.getById(id));
  }

  public <T extends Module> Optional<T> getByType(final Class<T> type) {
    return Optional.ofNullable(this.byType.get(type)).map(type::cast);
  }

  public <T extends Module> void register(final String id, final String name, final ModuleFactory<T> factory) {
    this.register(id, name, factory, LoadCondition::success);
  }

  public <T extends Module> void register(final String id, String name,
      final ModuleFactory<T> factory,
      final Supplier<LoadCondition> condition) {
    final String path = "Modules." + id;

    Object enabledObj = config.get(path + ".Enabled", (Object) null);
    boolean enabled = false;
    if (enabledObj == null || !(enabledObj instanceof Boolean)) {
      config.set(path + ".Enabled", false);
      enabledObj = false;
    } else {
      enabled = (boolean) enabledObj;
    }
    Object nameObj = config.get(path + ".Name", (Object) null);
    if (nameObj == null || !(nameObj instanceof String)) {
      config.set(path + ".Name", name);
    } else {
      name = (String) nameObj;
    }
    Object prefixObj = config.get(path + ".Prefix", (Object) null);
    String prefix = null;
    if (prefixObj == null || !(prefixObj instanceof String)) {
      prefixObj = defaultPrefix(name);
      config.set(path + ".Prefix", prefixObj);
    } else {
      prefix = (String) prefixObj;
    }

    this.definitionMap.add(new ModuleDefinition<T>(enabled, id, name, prefix, factory, condition));
  }

  public void loadAll() {
    for (ModuleDefinition<?> definition : this.definitionMap) {
      try {
        this.loadModule(definition);
      } catch (final ModuleLoadException exception) {
        this.plugin
            .error(
                "Fatal error when trying to load module '%s': %s".formatted(definition.id(), exception.getMessage()));
      }
    }
    config.saveChanges();
  }

  private <T extends Module> boolean loadModule(final ModuleDefinition<T> definition)
      throws ModuleLoadException {
    if (!definition.enabled())
      return false;
    if (isPresent(definition.id())) {
      throw new ModuleLoadException("Module with such ID is already registered!");
    }

    final LoadCondition condition = definition.condition().get();
    if (!condition.isSuccess()) {
      this.plugin.error("Module '%s' can not be loaded: '%s'".formatted(
          definition.id(), condition.reason().orElse(null)));
      return false;
    }

    final Module module = definition.factory().load(definition, this.plugin);
    if (isPresent(module.getClass())) {
      throw new IllegalStateException("Module of such type is already registered!");
    }

    this.byId.put(module.getId(), module);
    this.byType.put(module.getClass(), module);

    module.init();
    module.setup();
    return true;
  }

  private static String defaultPrefix(String name) {
    return TagWrappers.GRADIENT_3.with("#FFAA00", "#FF8833", "#FF5500")
        .wrap(TagWrappers.BOLD.wrap(name.toUpperCase(Locale.ROOT))) + TagWrappers.DARK_GRAY.wrap(" » ");
  }
}
