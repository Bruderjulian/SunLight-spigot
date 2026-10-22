package su.nightexpress.sunlight.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.ptp.request.TeleportRequest;

/**
 * Fired before a pending teleport request is accepted and consumed, after the request has been
 * resolved and the economy check has passed but before the request is expired or any cost is paid.
 * <p>
 * Cancelling leaves the request pending and uncharged, so the player can accept it later — this is
 * the hook for refusing a teleport, for example because the destination became unsafe in the
 * meantime.
 * <p>
 * Which of the two players actually moves depends on the request mode: for an invite the accepter
 * moves to the sender, for a request the sender moves to the accepter. Both are exposed via
 * {@link #getTeleporter()} and {@link #getDestination()}.
 */
public class PlayerTeleportRequestAcceptEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TeleportRequest request;
    private final Player accepter;
    private final Player teleporter;
    private final Location destination;
    private final double cost;
    private boolean cancelled;

    public PlayerTeleportRequestAcceptEvent(@NotNull TeleportRequest request,
            @NotNull Player accepter,
            @NotNull Player teleporter,
            @NotNull Location destination,
            double cost
    ) {
        this.request = request;
        this.accepter = accepter;
        this.teleporter = teleporter;
        this.destination = destination;
        this.cost = cost;
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

    /** The player who accepted the request. */
    public @NotNull Player getAccepter() {
        return this.accepter;
    }

    /** The player who will actually be teleported, which may be the other party. */
    public @NotNull Player getTeleporter() {
        return this.teleporter;
    }

    public @NotNull Location getDestination() {
        return this.destination;
    }

    /** The economy cost that will be charged on arrival; {@code 0} when nothing will be charged. */
    public double getCost() {
        return this.cost;
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