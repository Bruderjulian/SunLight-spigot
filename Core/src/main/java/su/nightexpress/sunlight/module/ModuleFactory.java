package su.nightexpress.sunlight.module;

import su.nightexpress.sunlight.SunLightPlugin;

@FunctionalInterface
public interface ModuleFactory<T extends Module> {

    T load(ModuleDefinition<T> definition, SunLightPlugin plugin);
}
