package su.nightexpress.sunlight.moduleImpl.nametags.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;

public class NametagsListener extends AbstractListener<SunLightPlugin> {

    private final NametagsModule module;

    public NametagsListener(SunLightPlugin plugin, NametagsModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.module.handleJoin(event);
    }
}
