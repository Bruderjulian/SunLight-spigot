package su.nightexpress.sunlight.api.event;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired when a home is created.
 */
public class HomeCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID ownerId;
    private final String homeId;
    private final Location location;
    private boolean cancelled;

    public HomeCreateEvent(@NotNull UUID ownerId, @NotNull String homeId, @NotNull Location location) {
        this.ownerId = ownerId;
        this.homeId = homeId;
        this.location = location.clone();
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull UUID getOwnerId() {
        return this.ownerId;
    }

    public @NotNull String getHomeId() {
        return this.homeId;
    }

    public @NotNull Location getLocation() {
        return this.location.clone();
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
