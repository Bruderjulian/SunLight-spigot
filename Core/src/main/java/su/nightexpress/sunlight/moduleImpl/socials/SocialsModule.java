package su.nightexpress.sunlight.moduleImpl.socials;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;

public class SocialsModule extends Module {

    public SocialsModule(ModuleDefinition<SocialsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {

    }

    @Override
    protected void unloadModule() {

    }

    @Override
    protected void registerPermissions(PermissionTree root) {

    }

    @Override
    protected void registerCommands() {

    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {

    }
}
