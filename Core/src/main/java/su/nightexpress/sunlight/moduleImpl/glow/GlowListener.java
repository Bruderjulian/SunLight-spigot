package su.nightexpress.sunlight.moduleImpl.glow;

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

    public GlowListener(final SunLightPlugin plugin, final GlowModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        if (!this.module.settings().isRestoreOnJoin()) {
            return;
        }
        this.plugin.runTask(() -> this.module.handler().applyGlow(module.userManager().getOrFetch(
                event.getPlayer()), event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(final PlayerQuitEvent event) {
        this.module.handler().handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent event) {
        this.plugin.runTask(() -> this.module.handler().applyGlow(module.userManager().getOrFetch(
                event.getPlayer()), event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        this.plugin.runTask(() -> this.module.handler().applyGlow(module.userManager().getOrFetch(
                event.getPlayer()), event.getPlayer()));
    }
}
