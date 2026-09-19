package su.nightexpress.sunlight.module;

import su.nightexpress.nightcore.manager.SimpleManager;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.sunlight.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ModuleRegistry {

    // private final SunLightPlugin plugin;
    private final Map<String, Module> byId;
    private final Map<Class<?>, Module> byType;

    public ModuleRegistry(/* SunLightPlugin plugin */) {
        // this.plugin = plugin;
        this.byId = new HashMap<>();
        this.byType = new HashMap<>();
    }

    public void reload() {
        this.getModules().forEach(SimpleManager::setup);
    }

    public boolean register(Module module) throws IllegalStateException {
        if (this.isPresent(module.getId()))
            throw new IllegalStateException("Module with such ID is already registered!");
        if (this.isPresent(module.getClass()))
            throw new IllegalStateException("Module of such type is already registered!");

        this.byId.put(module.getId(), module);
        this.byType.put(module.getClass(), module);

        module.init();
        module.setup();
        return true;
    }

    public void clear() {
        this.getModules().forEach(Module::shutdown);
        this.byId.clear();
        this.byType.clear();
    }

    public boolean isPresent(String id) {
        return this.byId(id).isPresent();
    }

    public <T extends Module> boolean isPresent(Class<T> type) {
        return this.byType(type).isPresent();
    }

    public Optional<Module> byId(String id) {
        return Optional.ofNullable(this.getById(id));
    }

    public <T extends Module> Optional<T> byType(Class<T> type) {
        return Optional.ofNullable(this.byType.get(type)).map(type::cast);
    }

    public Module getById(String id) {
        return this.byId.get(Utils.lowercase(id));
    }

    public Set<Module> getModules() {
        return Set.copyOf(this.byType.values());
    }
}
