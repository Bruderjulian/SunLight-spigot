package su.nightexpress.sunlight.moduleImpl.ptp.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.ptp.PTPModule;

public class PTPListener extends AbstractListener<SunLightPlugin> {

    private final PTPModule module;

    public PTPListener(SunLightPlugin plugin, PTPModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.module.clearRequests(event.getPlayer());
    }
}
