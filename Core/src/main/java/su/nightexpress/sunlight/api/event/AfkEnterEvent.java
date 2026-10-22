package su.nightexpress.sunlight.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a player has actually gone AFK.
 * <p>
 * This is the informational counterpart to the cancellable {@link AfkChangeEvent} and is only fired
 * when that event was not cancelled, so by the time a listener runs the player is already AFK.
 * Fired even for silent transitions (server reload, going AFK in an exempt world, module unload).
 */
public class AfkEnterEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final int idleTime;

    /**
     * @param player   the player who went AFK
     * @param idleTime how long they had been idle, in seconds, before going AFK
     */
    public AfkEnterEvent(@NotNull Player player, int idleTime) {
        this.player = player;
        this.idleTime = idleTime;
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

    /** Idle time in seconds that triggered this transition. */
    public int getIdleTime() {
        return this.idleTime;
    }
}