package su.nightexpress.sunlight.moduleImpl.friends.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.user.UserInfo;

/**
 * Fired before a friend request is queued, so a plugin can refuse it.
 * <p>
 * Fired before any limit check has been made public, so a handler that wants to replace the rules
 * can do so; a handler that wants to merely observe the outcome should listen to
 * {@link PlayerFriendRequestEvent} instead.
 */
public class PreFriendRequestEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UserInfo sender;
    private final UserInfo target;
    private boolean cancelled;

    public PreFriendRequestEvent(@NotNull UserInfo sender, @NotNull UserInfo target) {
        this.sender = sender;
        this.target = target;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull UserInfo getSender() {
        return this.sender;
    }

    public @NotNull UserInfo getTarget() {
        return this.target;
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