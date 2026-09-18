package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import su.nightexpress.sunlight.moduleImpl.ptp.request.TeleportRequest;

public class PlayerTeleportRequestEvent extends Event implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();

    private final TeleportRequest request;

    private boolean isCancelled;

    public PlayerTeleportRequestEvent(TeleportRequest request) {
        this.request = request;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    public TeleportRequest getRequest() {
        return this.request;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }
}
