package su.nightexpress.sunlight.module;

import java.nio.file.Path;
import java.util.function.Supplier;

import su.nightexpress.sunlight.SunLightPlugin;

public record ModuleDefinition<T extends Module>(boolean enabled, String id, String name, String prefix,
        ModuleFactory<T> factory, Supplier<LoadCondition> condition) {

    public Path path(SunLightPlugin plugin) {
        return Path.of(plugin.getDataFolder() + "/modules/", id);
    }
}
