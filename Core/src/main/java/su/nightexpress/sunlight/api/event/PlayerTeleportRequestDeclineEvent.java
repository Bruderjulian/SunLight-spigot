package su.nightexpress.sunlight.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.ptp.request.TeleportRequest;

/**
 * Fired before a pending teleport request is declined and consumed, before the request is expired.
 * <p>
 * Cancelling keeps the request pending, so it can still be accepted later. Use it to override a
 * refusal — for example to force-close a request that another plugin has flagged as abusive.
 */
public class PlayerTeleportRequestDeclineEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TeleportRequest request;
    private final Player decliner;
    private final Player sender;
    private boolean cancelled;

    public PlayerTeleportRequestDeclineEvent(@NotNull TeleportRequest request,
            @NotNull Player decliner,
            @NotNull Player sender
    ) {
        this.request = request;
        this.decliner = decliner;
        this.sender = sender;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull TeleportRequest getRequest() {
        return this.request;
    }

    /** The player who refused the request. */
    public @NotNull Player getDecliner() {
        return this.decliner;
    }

    /** The player who sent the request and is being notified. */
    public @NotNull Player getSender() {
        return this.sender;
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