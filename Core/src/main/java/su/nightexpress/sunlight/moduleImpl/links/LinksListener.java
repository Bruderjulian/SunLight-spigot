package su.nightexpress.sunlight.moduleImpl.links;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;

public class LinksListener extends AbstractListener<SunLightPlugin> {

    private final LinksModule module;

    public LinksListener(@NotNull SunLightPlugin plugin, @NotNull LinksModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        // Cooldowns are session state: the inner expiry map prunes itself lazily, but the outer
        // UUID key would otherwise live until module unload.
        this.module.clearCooldowns(event.getPlayer());
    }
}
