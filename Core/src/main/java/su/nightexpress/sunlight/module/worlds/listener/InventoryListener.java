package su.nightexpress.sunlight.module.worlds.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.worlds.impl.WorldInventories;
import su.nightexpress.sunlight.module.worlds.WorldsModule;

public class InventoryListener extends AbstractListener<SunLightPlugin> {

    private final WorldsModule module;

    public InventoryListener(SunLightPlugin plugin, WorldsModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventorySplitJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.module.getWorldGroup(player.getWorld()) == null)
            return;

        this.module.getWorldInventory(player).loadInventory(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventorySplitChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String groupTo = this.module.getWorldGroup(player.getWorld());
        String groupFrom = this.module.getWorldGroup(event.getFrom());

        // Do not affect snapshots for the same group
        if (groupFrom != null && groupTo != null) {
            if (groupTo.equalsIgnoreCase(groupFrom)) {
                return;
            }
        }

        WorldInventories worldInventories = this.module.getWorldInventory(player);
        // And here do snapshot for current player inv for world he comes from
        if (groupFrom != null) {
            worldInventories.saveInventory(player, groupFrom);
        }

        // And now replace it by the inventory of new world
        if (groupTo != null) {
            worldInventories.loadInventory(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventorySplitQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getUniqueId().toString();

        WorldInventories worldInventories = this.module.getInventoryMap().remove(playerId);
        if (worldInventories != null) {
            String group = this.module.getWorldGroup(player.getWorld());
            if (group != null) {
                worldInventories.saveInventory(player, group);
            }
            worldInventories.save();
        }
    }
}
