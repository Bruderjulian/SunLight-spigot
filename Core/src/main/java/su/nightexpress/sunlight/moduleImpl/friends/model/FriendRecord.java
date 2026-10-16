package su.nightexpress.sunlight.moduleImpl.friends.model;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolvable;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsPlaceholders;

import java.util.UUID;

/**
 * The module's own friendship row.
 * <p>
 * Named to avoid clashing with the API's {@code FriendHandle}: this one is mutable and carries the
 * repository bookkeeping, while the API type is an immutable snapshot handed to other plugins.
 * <p>
 * A mutual friendship is two of these rows, one owned by each side, because the favourite flag is
 * per-owner: two friends of each other may have pinned each other differently.
 */
@NullMarked
public class FriendRecord implements PlaceholderResolvable {

    private final UserInfo owner;
    private final UserInfo friend;

    private long createDate;
    private boolean favorite;

    public FriendRecord(UserInfo owner, UserInfo friend, long createDate, boolean favorite) {
        this.owner = owner;
        this.friend = friend;
        this.createDate = createDate;
        this.favorite = favorite;
    }

    public static FriendRecord create(UserInfo owner, UserInfo friend) {
        return new FriendRecord(owner, friend, System.currentTimeMillis(), false);
    }

    @Override
    public PlaceholderResolver placeholders() {
        return FriendsPlaceholders.FRIEND.resolver(this);
    }

    public UserInfo getOwner() {
        return this.owner;
    }

    public UserInfo getFriend() {
        return this.friend;
    }

    public UUID getOwnerId() {
        return this.owner.id();
    }

    public UUID getFriendId() {
        return this.friend.id();
    }

    public long getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(long createDate) {
        this.createDate = createDate;
    }

    public boolean isFavorite() {
        return this.favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isOwner(Player player) {
        return this.owner.isUser(player);
    }

    public boolean isOwner(UUID playerId) {
        return this.owner.id().equals(playerId);
    }

    public boolean isMember(UUID playerId) {
        return this.owner.id().equals(playerId) || this.friend.id().equals(playerId);
    }

    /** The other party, or {@code null} when that player is neither of the two. */
    @Nullable
    public UserInfo getOther(UUID playerId) {
        if (this.owner.id().equals(playerId)) {
            return this.friend;
        }
        if (this.friend.id().equals(playerId)) {
            return this.owner;
        }
        return null;
    }
}