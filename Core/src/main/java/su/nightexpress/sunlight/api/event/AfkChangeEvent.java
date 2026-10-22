package su.nightexpress.sunlight.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a player's AFK state changes, while it is still unapplied.
 * <p>
 * Cancelling aborts the transition entirely: the tracker keeps its previous state and none of the
 * enter/leave side effects run — no commands, no sounds, no broadcast, no AFK session bookkeeping.
 * Use this to veto a player being put to sleep, or to veto a wake-up being accepted (for example
 * while a minigame round is still running).
 * <p>
 * Follow this with {@link AfkEnterEvent} or {@link AfkLeaveEvent}, which are only fired once the
 * change has actually been applied.
 */
public class AfkChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean afk;
    private boolean cancelled;

    /**
     * @param player the player whose AFK state is about to change
     * @param afk   the state being switched <i>to</i> — {@code true} to go AFK, {@code false} to wake up
     */
    public AfkChangeEvent(@NotNull Player player, boolean afk) {
        this.player = player;
        this.afk = afk;
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

    /** The AFK state being switched to, not the player's current one. */
    public boolean isAfk() {
        return this.afk;
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