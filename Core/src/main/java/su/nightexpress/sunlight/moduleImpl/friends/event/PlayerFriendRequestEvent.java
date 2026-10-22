package su.nightexpress.sunlight.moduleImpl.friends.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.user.UserInfo;

/**
 * Fired after a request has been queued, so a plugin can react to it without having to queue one.
 */
public class PlayerFriendRequestEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UserInfo sender;
    private final UserInfo target;
    private final long expireDate;

    public PlayerFriendRequestEvent(@NotNull UserInfo sender, @NotNull UserInfo target, long expireDate) {
        this.sender = sender;
        this.target = target;
        this.expireDate = expireDate;
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

    /** Epoch millis after which the request stops being actionable. */
    public long getExpireDate() {
        return this.expireDate;
    }
}