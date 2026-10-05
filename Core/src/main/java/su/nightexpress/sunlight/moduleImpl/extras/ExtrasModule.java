package su.nightexpress.sunlight.moduleImpl.extras;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.extras.config.ExtrasConfig;
import su.nightexpress.sunlight.moduleImpl.extras.config.ExtrasLang;
import su.nightexpress.sunlight.moduleImpl.extras.listener.ExtrasGenericListener;
import su.nightexpress.sunlight.moduleImpl.extras.listener.PhysicsExplosionListener;

public class ExtrasModule extends Module {

    public ExtrasModule(ModuleDefinition<ExtrasModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
    }

    @Override
    protected void loadModule(FileConfig config) {
        config.initializeOptions(ExtrasConfig.class);
        this.plugin.injectLang(ExtrasLang.class);

        if (ExtrasConfig.PHYSIC_EXPLOSIONS_ENABLED.get()) {
            this.addListener(new PhysicsExplosionListener(this.plugin));
        }
        this.addListener(new ExtrasGenericListener(this.plugin, this));
    }

    @Override
    protected void unloadModule() {
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
    }
}
