package su.nightexpress.sunlight.moduleImpl.spawns.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import su.nightexpress.sunlight.moduleImpl.spawns.Spawn;

public class PlayerSpawnTeleportEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final Spawn spawn;

    private boolean cancelled;

    public PlayerSpawnTeleportEvent(Player player, Spawn spawn) {
        this.player = player;
        this.spawn = spawn;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public Player getPlayer() {
        return player;
    }

    public Spawn getSpawn() {
        return spawn;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
