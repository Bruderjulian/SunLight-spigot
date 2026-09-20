package su.nightexpress.sunlight.moduleImpl.glow;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;

public class GlowListener extends AbstractListener<SunLightPlugin> {

    private final GlowModule module;

    public GlowListener(SunLightPlugin plugin, GlowModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!this.module.getSettings().isRestoreOnJoin()) return;
        Player player = event.getPlayer();
        this.plugin.runTask(() -> {
            this.module.applyGlow(player);
            this.module.sendAllGlowsTo(player);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.module.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        this.plugin.runTask(() -> this.module.applyGlow(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        this.plugin.runTask(() -> {
            this.module.applyGlow(player);
            this.module.sendAllGlowsTo(player);
        });
    }
}
