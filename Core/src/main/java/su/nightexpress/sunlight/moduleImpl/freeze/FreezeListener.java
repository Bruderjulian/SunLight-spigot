package su.nightexpress.sunlight.moduleImpl.freeze;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.freeze.config.FreezeConfig;
import su.nightexpress.sunlight.moduleImpl.freeze.config.FreezeLang;

public class FreezeListener extends AbstractListener<SunLightPlugin> {

    private final FreezeModule module;

    public FreezeListener(SunLightPlugin plugin, FreezeModule module) {
        super(plugin);
        this.module = module;
    }

    private boolean isFrozen(Player player) {
        return this.module.isFrozen(player);
    }

    private void notifyBlocked(Player player) {
        this.module.sendPrefixed(FreezeLang.ERROR_ACTION_BLOCKED, player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFreezeMove(PlayerMoveEvent event) {
        if (!FreezeConfig.BLOCK_MOVEMENT.get()) return;

        Player player = event.getPlayer();
        if (!this.isFrozen(player)) return;
        if (!event.hasChangedBlock()) return;

        event.setCancelled(true);
        this.notifyBlocked(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeTeleport(PlayerTeleportEvent event) {
        if (!FreezeConfig.BLOCK_TELEPORT.get()) return;

        Player player = event.getPlayer();
        if (!this.isFrozen(player)) return;

        switch (event.getCause()) {
            case PLUGIN, UNKNOWN -> {
                return;
            }
            default -> {}
        }

        event.setCancelled(true);
        this.notifyBlocked(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeDamageIncoming(EntityDamageEvent event) {
        if (!FreezeConfig.BLOCK_DAMAGE_INCOMING.get()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!this.isFrozen(player)) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeDamageOutgoing(EntityDamageByEntityEvent event) {
        if (!FreezeConfig.BLOCK_DAMAGE_OUTGOING.get()) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!this.isFrozen(player)) return;

        event.setCancelled(true);
        this.notifyBlocked(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeBreak(BlockBreakEvent event) {
        if (!FreezeConfig.BLOCK_BLOCK_BREAK.get()) return;
        if (!this.isFrozen(event.getPlayer())) return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezePlace(BlockPlaceEvent event) {
        if (!FreezeConfig.BLOCK_BLOCK_PLACE.get()) return;
        if (!this.isFrozen(event.getPlayer())) return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeInteract(PlayerInteractEvent event) {
        if (!FreezeConfig.BLOCK_INTERACT.get()) return;
        if (!this.isFrozen(event.getPlayer())) return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeEntityInteract(PlayerInteractEntityEvent event) {
        if (!FreezeConfig.BLOCK_INTERACT.get()) return;
        if (!this.isFrozen(event.getPlayer())) return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeInventoryClick(InventoryClickEvent event) {
        if (!FreezeConfig.BLOCK_INVENTORY.get()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!this.isFrozen(player)) return;

        event.setCancelled(true);
        this.notifyBlocked(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeInventoryDrag(InventoryDragEvent event) {
        if (!FreezeConfig.BLOCK_INVENTORY.get()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!this.isFrozen(player)) return;

        event.setCancelled(true);
        this.notifyBlocked(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeDrop(PlayerDropItemEvent event) {
        if (!FreezeConfig.BLOCK_ITEM_DROP.get()) return;
        if (!this.isFrozen(event.getPlayer())) return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezePickup(EntityPickupItemEvent event) {
        if (!FreezeConfig.BLOCK_ITEM_PICKUP.get()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!this.isFrozen(player)) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeCommand(PlayerCommandPreprocessEvent event) {
        if (!FreezeConfig.BLOCK_COMMANDS.get()) return;

        Player player = event.getPlayer();
        if (!this.isFrozen(player)) return;

        String input = event.getMessage().substring(1).trim().toLowerCase();
        for (String allowed : FreezeConfig.ALLOWED_COMMANDS.get()) {
            String normalized = allowed.startsWith("/") ? allowed.substring(1) : allowed;
            normalized = normalized.trim().toLowerCase();
            if (normalized.isEmpty()) continue;
            if (input.equals(normalized) || input.startsWith(normalized + " ") || input.startsWith(normalized + ":")) {
                return;
            }
        }

        event.setCancelled(true);
        this.notifyBlocked(player);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFreezeChat(AsyncPlayerChatEvent event) {
        if (!FreezeConfig.BLOCK_CHAT.get()) return;
        if (!this.isFrozen(event.getPlayer())) return;

        event.setCancelled(true);
        this.notifyBlocked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFreezeJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!this.isFrozen(player)) return;

        this.module.sendPrefixed(FreezeLang.JOIN_FROZEN, player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFreezeQuit(PlayerQuitEvent event) {
        if (!FreezeConfig.UNFREEZE_ON_QUIT.get()) return;

        Player player = event.getPlayer();
        if (!this.isFrozen(player)) return;

        this.module.setFrozen(player, false);
    }
}
