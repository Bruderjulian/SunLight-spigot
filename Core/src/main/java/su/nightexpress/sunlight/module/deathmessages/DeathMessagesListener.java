package su.nightexpress.sunlight.module.deathmessages;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;

public class DeathMessagesListener extends AbstractListener<SunLightPlugin> {

    private final DeathMessagesModule module;

    public DeathMessagesListener(SunLightPlugin plugin, DeathMessagesModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        this.module.handleDeathEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilPlace(BlockPlaceEvent event) {
        if (!event.getBlock().getType().name().contains("ANVIL"))
            return;

        this.module.trackAnvilPlacement(event.getBlock().getLocation(), event.getPlayer());
    }
}
