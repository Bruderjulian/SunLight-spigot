package su.nightexpress.sunlight.moduleImpl.afk.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.afk.ActivityType;
import su.nightexpress.sunlight.moduleImpl.afk.AfkModule;
import su.nightexpress.sunlight.moduleImpl.afk.core.AfkLang;
import su.nightexpress.sunlight.moduleImpl.afk.core.AfkSettings;

public class AfkListener extends AbstractListener<SunLightPlugin> {

    private final AfkModule module;
    private final AfkSettings settings;

    public AfkListener(SunLightPlugin plugin, AfkModule module) {
        super(plugin);
        this.module = module;
        this.settings = module.getSettings();
    }

    private boolean isAfk(Player player) {
        return this.module.isAfk(player);
    }

    private void notifyBlocked(Player player) {
        this.module.sendPrefixed(AfkLang.ERROR_ACTION_BLOCKED, player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (this.module.isWithinRejoinKickWindow(player)) {
            Players.kick(player, this.module.getPrefixed(AfkLang.KICK_REJOIN).getText());
            return;
        }

        this.module.track(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.module.untrack(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onIdleDeath(PlayerDeathEvent event) {
        this.module.exitAfk(event.getEntity(), false); // Force exit AFK mode on death.
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIdleTeleport(PlayerTeleportEvent event) {
        this.module.exitAfk(event.getPlayer(), false); // Force exit AFK mode on teleportations.
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedMove(PlayerMoveEvent event) {
        if (!this.settings.blockMovement.get())
            return;
        if (!event.hasChangedBlock())
            return;
        if (!this.isAfk(event.getPlayer()))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedBreak(BlockBreakEvent event) {
        if (!this.settings.blockBlockBreak.get())
            return;
        if (!this.isAfk(event.getPlayer()))
            return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedPlace(BlockPlaceEvent event) {
        if (!this.settings.blockBlockPlace.get())
            return;
        if (!this.isAfk(event.getPlayer()))
            return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedInteract(PlayerInteractEvent event) {
        if (!this.settings.blockInteract.get())
            return;
        if (!this.isAfk(event.getPlayer()))
            return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedEntityInteract(PlayerInteractEntityEvent event) {
        if (!this.settings.blockInteract.get())
            return;
        if (!this.isAfk(event.getPlayer()))
            return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedInventoryClick(InventoryClickEvent event) {
        if (!this.settings.blockInventory.get())
            return;
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!this.isAfk(player))
            return;

        event.setCancelled(true);
        this.notifyBlocked(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedInventoryDrag(InventoryDragEvent event) {
        if (!this.settings.blockInventory.get())
            return;
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!this.isAfk(player))
            return;

        event.setCancelled(true);
        this.notifyBlocked(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedDrop(PlayerDropItemEvent event) {
        if (!this.settings.blockItemDrop.get())
            return;
        if (!this.isAfk(event.getPlayer()))
            return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedPickup(EntityPickupItemEvent event) {
        if (!this.settings.blockItemPickup.get())
            return;
        if (!(event.getEntity() instanceof Player player))
            return;
        if (!this.isAfk(player))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedChat(AsyncPlayerChatEvent event) {
        if (!this.settings.blockChat.get())
            return;
        if (!this.isAfk(event.getPlayer()))
            return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedCommand(PlayerCommandPreprocessEvent event) {
        if (!this.settings.blockCommands.get())
            return;
        if (!this.isAfk(event.getPlayer()))
            return;
        if (this.settings.isCommandIgnored(event.getMessage()))
            return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedDamageIncoming(EntityDamageEvent event) {
        if (!this.settings.blockDamageIncoming.get())
            return;
        if (!(event.getEntity() instanceof Player player))
            return;
        if (!this.isAfk(player))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedDamageOutgoing(EntityDamageByEntityEvent event) {
        if (!this.settings.blockDamageOutgoing.get())
            return;
        if (!(event.getDamager() instanceof Player player))
            return;
        if (!this.isAfk(player))
            return;

        event.setCancelled(true);
        this.notifyBlocked(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockedMobTargeting(EntityTargetLivingEntityEvent event) {
        if (!this.settings.blockMobTargeting.get())
            return;
        if (!(event.getTarget() instanceof Player player))
            return;
        if (!this.isAfk(player))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActivityInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        this.module.trackActivity(event.getPlayer(), ActivityType.INTERACT);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActivityCommand(PlayerCommandPreprocessEvent event) {
        if (this.settings.isCommandIgnored(event.getMessage()))
            return;

        this.module.trackActivity(event.getPlayer(), ActivityType.COMMAND);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActivityBreak(BlockBreakEvent event) {
        this.module.trackActivity(event.getPlayer(), ActivityType.BLOCK_BREAK);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActivityPlace(BlockPlaceEvent event) {
        this.module.trackActivity(event.getPlayer(), ActivityType.BLOCK_PLACE);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActivityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;

        this.module.trackActivity(player, ActivityType.ITEM_PICKUP);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActivityContainer(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;

        this.module.trackActivity(player, ActivityType.CONTAINER);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActivityFish(PlayerFishEvent event) {
        switch (event.getState()) {
            case CAUGHT_FISH, CAUGHT_ENTITY -> this.module.trackActivity(event.getPlayer(), ActivityType.FISH);
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onActivityVehicle(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            this.module.trackActivity(player, ActivityType.VEHICLE);
        }
    }
}