package su.nightexpress.sunlight.moduleImpl.items;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.items.command.ItemCommandProvider;
import su.nightexpress.sunlight.moduleImpl.items.command.LoreCommandsProvider;

public class ItemsModule extends Module {

    private final ItemsSettings settings;

    public ItemsModule(ModuleDefinition<ItemsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new ItemsSettings();
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.settings.load(config);
        this.plugin.injectLang(ItemsLang.class);
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(ItemsPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("item",
                new ItemCommandProvider(this.plugin, this, this.settings, this.userManager), this);
        this.commandRegistry.addProvider("lore", new LoreCommandsProvider(this.plugin, this), this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {

    }
}
