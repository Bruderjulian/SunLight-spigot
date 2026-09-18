package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import su.nightexpress.sunlight.teleport.TeleportContext;
import su.nightexpress.sunlight.teleport.TeleportType;

public class SunlightPlayerTeleportEvent extends Event implements Cancellable {

    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final TeleportContext context;
    private final TeleportType type;

    private boolean intercepted;
    private boolean cancelled;

    public SunlightPlayerTeleportEvent(TeleportContext context, TeleportType type) {
        this.context = context;
        this.type = type;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override

    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public TeleportContext getContext() {
        return this.context;
    }

    public TeleportType getType() {
        return this.type;
    }

    public boolean isIntercepted() {
        return this.intercepted;
    }

    public void setIntercepted(boolean intercepted) {
        this.intercepted = intercepted;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
