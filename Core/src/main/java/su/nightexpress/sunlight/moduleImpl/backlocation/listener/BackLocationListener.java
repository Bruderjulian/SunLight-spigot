package su.nightexpress.sunlight.moduleImpl.backlocation.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.backlocation.BackLocationModule;

public class BackLocationListener extends AbstractListener<SunLightPlugin> {

    private final BackLocationModule module;

    public BackLocationListener(SunLightPlugin plugin, BackLocationModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBackLocationSave(PlayerTeleportEvent event) {
        this.module.handleTeleport(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeathLocationSave(PlayerDeathEvent event) {
        this.module.handleDeath(event.getPlayer(), event);
    }
}
