package su.nightexpress.sunlight.api.event;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.backlocation.data.LocationType;

/**
 * Fired before a player is teleported back to a stored location, after the location has been
 * resolved and the player can afford it but before the cost is charged or the teleport starts.
 * <p>
 * Cancelling keeps the player where they are and charges nothing.
 * <p>
 * This fires earlier than the generic {@code SunlightPlayerTeleportEvent}, before the economy
 * withdrawal and before any warmup is started — use it to veto a return outright. A return that
 * triggers a warmup can be intercepted later through the warmup events instead.
 */
public class PlayerBackTeleportEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final LocationType type;
    private final Location destination;
    private final double cost;
    private boolean cancelled;

    public PlayerBackTeleportEvent(@NotNull Player player, @NotNull LocationType type, @NotNull Location destination,
            double cost) {
        this.player = player;
        this.type = type;
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

    public @NotNull Player getPlayer() {
        return this.player;
    }

    /** Whether this is a return to the pre-teleport position or to the death position. */
    public @NotNull LocationType getType() {
        return this.type;
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