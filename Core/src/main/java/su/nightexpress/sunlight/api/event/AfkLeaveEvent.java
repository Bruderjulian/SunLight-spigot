package su.nightexpress.sunlight.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a player has actually woken up from AFK.
 * <p>
 * This is the informational counterpart to the cancellable {@link AfkChangeEvent} and is only fired
 * when that event was not cancelled, so by the time a listener runs the player is already awake.
 * The reported duration is the AFK session that just ended and has already been recorded into the
 * player's total AFK time and AFK count.
 */
public class AfkLeaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final long duration;

    /**
     * @param player   the player who woke up
     * @param duration how long they were AFK, in milliseconds; never negative
     */
    public AfkLeaveEvent(@NotNull Player player, long duration) {
        this.player = player;
        this.duration = Math.max(0L, duration);
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

    /** The length of the AFK session that just ended, in milliseconds. */
    public long getDuration() {
        return this.duration;
    }
}