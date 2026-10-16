package su.nightexpress.sunlight.moduleImpl.friends.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.user.UserInfo;

/**
 * Fired after a block has been added or lifted.
 */
public class PlayerFriendBlockEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UserInfo blocker;
    private final UserInfo blocked;
    private final boolean blocking;

    public PlayerFriendBlockEvent(@NotNull UserInfo blocker, @NotNull UserInfo blocked, boolean blocking) {
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
}