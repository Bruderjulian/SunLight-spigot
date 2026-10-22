package su.nightexpress.sunlight.moduleImpl.friends.model;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.api.provider.dto.FriendHandle;

import java.util.UUID;

/**
 * Builds the immutable API snapshot of a {@link FriendRecord}.
 * <p>
 * The last-seen stamp comes from the caller, not from the friendship row, so it stays correct
 * while a player is online and survives a rename.
 */
public final class FriendSnapshots {

    private FriendSnapshots() {
    }

    public static FriendHandle of(FriendRecord record, long lastSeen) {
        return new FriendHandle(
                record.getOwnerId(),
                record.getOwner().name(),
                record.getFriendId(),
                record.getFriend().name(),
                record.getCreateDate(),
                record.isFavorite(),
                Bukkit.getPlayer(record.getFriendId()) != null,
                normalize(lastSeen)
        );
    }

    public static FriendHandle ofBlocked(BlockRecord block, long lastSeen) {
        return new FriendHandle(
                block.getBlockerId(),
                block.getBlocker().name(),
                block.getBlockedId(),
                block.getBlocked().name(),
                block.getCreateDate(),
                false,
                Bukkit.getPlayer(block.getBlockedId()) != null,
                normalize(lastSeen)
        );
    }

    /** Zero means "never seen", which the API contract allows as a null-ish value. */
    private static Long normalize(long lastSeen) {
        return lastSeen <= 0L ? null : lastSeen;
    }

    /** The side of the friendship that belongs to that player. */
    @NotNull
    public static UserInfo viewOf(@NotNull FriendRecord record, @NotNull UUID playerId) {
        return record.getOwner().id().equals(playerId) ? record.getOwner() : record.getFriend();
    }
}