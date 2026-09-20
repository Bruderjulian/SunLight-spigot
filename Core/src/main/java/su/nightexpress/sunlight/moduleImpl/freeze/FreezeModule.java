package su.nightexpress.sunlight.moduleImpl.freeze;

import org.bukkit.entity.Player;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.api.provider.FreezeProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.freeze.command.FreezeCommand;
import su.nightexpress.sunlight.moduleImpl.freeze.config.FreezeConfig;
import su.nightexpress.sunlight.moduleImpl.freeze.config.FreezeLang;
import su.nightexpress.sunlight.moduleImpl.freeze.config.FreezePerms;
import su.nightexpress.sunlight.moduleImpl.freeze.event.PlayerFreezeEvent;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;

public class FreezeModule extends Module implements FreezeProvider {

    public static final UserProperty<Boolean> FROZEN = UserProperty.create("freeze", Boolean.class, false, true);

    public FreezeModule(ModuleDefinition<FreezeModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
    }

    @Override
    protected void loadModule(FileConfig config) {
        config.initializeOptions(FreezeConfig.class);
        this.plugin.injectLang(FreezeLang.class);
        UserPropertyRegistry.register(FROZEN);

        this.addListener(new FreezeListener(this.plugin, this));
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(FreezePerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("freeze", new FreezeCommand(this.plugin, this, this.userManager), this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        registry.register("freeze_state", (player, payload) -> {
            return CoreLang.STATE_YES_NO.get(this.userManager.getOrFetch(player).getPropertyOrDefault(FROZEN));
        });
    }

    @Override
    public boolean isFrozen(Player player) {
        SunUser user = this.plugin.userManager().getOrFetch(player);
        return user.getPropertyOrDefault(FROZEN);
    }

    @Override
    public void setFrozen(Player player, boolean frozen) {
        PlayerFreezeEvent event = new PlayerFreezeEvent(player, frozen);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        SunUser user = this.plugin.userManager().getOrFetch(player);
        user.setProperty(FROZEN, frozen);
        user.markDirty();
    }
}
