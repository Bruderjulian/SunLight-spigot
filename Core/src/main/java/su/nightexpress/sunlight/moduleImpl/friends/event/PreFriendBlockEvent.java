package su.nightexpress.sunlight.moduleImpl.friends.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.user.UserInfo;

/**
 * Fired before a player blocks or unblocks somebody.
 * <p>
 * Blocking also drops the friendship in both directions, so cancelling this leaves the friendship
 * intact rather than half of it.
 */
public class PreFriendBlockEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UserInfo blocker;
    private final UserInfo blocked;
    private final boolean blocking;
    private boolean cancelled;

    public PreFriendBlockEvent(@NotNull UserInfo blocker, @NotNull UserInfo blocked, boolean blocking) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.blocking = blocking;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull UserInfo getBlocker() {
        return this.blocker;
    }

    public @NotNull UserInfo getBlocked() {
        return this.blocked;
    }

    /** True for a new block, false for a lifted one. */
    public boolean isBlocking() {
        return this.blocking;
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