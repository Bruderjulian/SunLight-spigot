package su.nightexpress.sunlight.api.event;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.backlocation.data.LocationType;

import java.util.UUID;

/**
 * Fired before a back or death location is written to a player's record, before the stored entry is
 * replaced.
 * <p>
 * Cancelling leaves the previous location untouched. This matters because the write happens
 * automatically — a player teleporting away overwrites their previous location, and dying overwrites
 * their death location — so without this hook a plugin has no way to stop that.
 * <p>
 * Locations are tracked per player id rather than per {@code Player}, so this fires for offline
 * players too and {@link #getPlayerId()} is always present.
 */
public class BackLocationSaveEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final Location location;
    private final LocationType type;
    private boolean cancelled;

    public BackLocationSaveEvent(@NotNull UUID playerId, @NotNull Location location, @NotNull LocationType type) {
        this.playerId = playerId;
        this.location = location;
        this.type = type;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull UUID getPlayerId() {
        return this.playerId;
    }

    /** The location about to be remembered. */
    public @NotNull Location getLocation() {
        return this.location;
    }

    /** Which slot is being written — the pre-teleport position or the death position. */
    public @NotNull LocationType getType() {
        return this.type;
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