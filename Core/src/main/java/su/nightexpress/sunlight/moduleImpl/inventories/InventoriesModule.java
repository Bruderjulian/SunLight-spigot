package su.nightexpress.sunlight.moduleImpl.inventories;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleContext;
import su.nightexpress.sunlight.moduleImpl.inventories.command.ContainerCommandProvider;
import su.nightexpress.sunlight.moduleImpl.inventories.command.EnderchestCommandsProvider;
import su.nightexpress.sunlight.moduleImpl.inventories.command.InventoryCommandProvider;
import su.nightexpress.sunlight.moduleImpl.inventories.dialog.InventoryDialogKeys;
import su.nightexpress.sunlight.moduleImpl.inventories.dialog.impl.InventoryClearDialog;
import su.nightexpress.sunlight.nms.SunNMS;

public class InventoriesModule extends Module {

    private final SunNMS internals;

    public InventoriesModule(ModuleContext context, SunNMS internals) {
        super(context);
        this.internals = internals;
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        config.initializeOptions(InventoriesSettings.class);

        this.dialogRegistry.register(InventoryDialogKeys.CLEAR, InventoryClearDialog::new);
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(InventoriesPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        if (this.internals != null) {
            this.commandRegistry.addProvider("container",
                    new ContainerCommandProvider(this.plugin, this, this.internals), this);
        }

        this.commandRegistry.addProvider("enderchest",
                new EnderchestCommandsProvider(this.plugin, this, this.userManager, this.internals), this);
        this.commandRegistry.addProvider("inventory",
                new InventoryCommandProvider(this.plugin, this, this.userManager, this.internals), this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {

    }

    public boolean isClearConfirmationRequired() {
        return InventoriesSettings.CLEAR_REQUIRE_CONFIRMATION.get();
    }

    public boolean isClearConfirmSelfOnly() {
        return InventoriesSettings.CLEAR_CONFIRM_SELF_ONLY.get();
    }
}
