package su.nightexpress.sunlight.moduleImpl.inventories;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.InventoriesProvider;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.inventories.command.ContainerCommandProvider;
import su.nightexpress.sunlight.moduleImpl.inventories.command.EnderchestCommandsProvider;
import su.nightexpress.sunlight.moduleImpl.inventories.command.InventoryCommandProvider;
import su.nightexpress.sunlight.moduleImpl.inventories.dialog.InventoryDialogKeys;
import su.nightexpress.sunlight.moduleImpl.inventories.dialog.impl.InventoryClearDialog;
import su.nightexpress.sunlight.nms.SunNMS;

public class InventoriesModule extends Module implements InventoriesProvider {

    private final SunNMS internals;

    public InventoriesModule(ModuleDefinition<InventoriesModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.internals = plugin.getInternals();
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        config.initializeOptions(InventoriesSettings.class);

        this.dialogRegistry.register(InventoryDialogKeys.CLEAR, InventoryClearDialog::new);

        if (this.internals != null) {
            this.commandRegistry.addProvider(new ContainerCommandProvider(this));
        }
        this.commandRegistry.addProvider(new EnderchestCommandsProvider(this));
        this.commandRegistry.addProvider(new InventoryCommandProvider(this));
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {

    }

    public SunNMS getInternals() {
        return this.internals;
    }

    @Override
    public boolean isClearConfirmationRequired() {
        return InventoriesSettings.CLEAR_REQUIRE_CONFIRMATION.get();
    }

    @Override
    public boolean isClearConfirmSelfOnly() {
        return InventoriesSettings.CLEAR_CONFIRM_SELF_ONLY.get();
    }
}
