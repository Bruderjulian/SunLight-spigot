package su.nightexpress.sunlight.moduleImpl.nametags.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;

public class NametagsListener extends AbstractListener<SunLightPlugin> {

    private final su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule module;

    public NametagsListener(SunLightPlugin plugin, su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.plugin.runTask(() -> {
            this.module.applyNametag(player);
            this.module.sendAllTagsTo(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.module.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        this.module.handleWorldChange(event.getPlayer(), event.getFrom());
    }
}