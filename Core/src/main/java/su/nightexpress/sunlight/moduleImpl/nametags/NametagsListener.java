package su.nightexpress.sunlight.moduleImpl.nametags;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;

/**
 * Triggers recomputation on the events that can change a nameplate. Every handler defers to
 * a task, because TAB is not necessarily ready during the event itself.
 */
public class NametagsListener extends AbstractListener<SunLightPlugin> {

    private final NametagsModule module;

    public NametagsListener(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.plugin.runTask(() -> this.module.getNameplates().recompute(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.module.getNameplates().forget(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        this.plugin.runTask(() -> this.module.getNameplates().recompute(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        // Profiles can be world-scoped, so the whole nameplate is rebuilt.
        this.plugin.runTask(() -> this.module.getNameplates().recompute(player));
    }
}
