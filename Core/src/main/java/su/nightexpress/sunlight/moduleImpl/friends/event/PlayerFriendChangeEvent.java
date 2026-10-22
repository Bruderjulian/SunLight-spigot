package su.nightexpress.sunlight.moduleImpl.friends.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRecord;

/**
 * Fired after a friendship row has been added or removed.
 * <p>
 * Fires once per row, so a mutual friendship fires it twice with the owner and friend swapped.
 */
public class PlayerFriendChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final FriendRecord record;
    private final boolean addition;

    public PlayerFriendChangeEvent(@NotNull FriendRecord record, boolean addition) {
        this.record = record;
        this.addition = addition;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull FriendRecord getRecord() {
        return this.record;
    }

    public @NotNull UserInfo getOwner() {
        return this.record.getOwner();
    }

    public @NotNull UserInfo getFriend() {
        return this.record.getFriend();
    }

    /** True for an addition, false for a removal. */
    public boolean isAddition() {
        return this.addition;
    }
}