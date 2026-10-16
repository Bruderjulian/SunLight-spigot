package su.nightexpress.sunlight.moduleImpl.friends.model;

import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.api.provider.dto.FriendRequestHandle;
import su.nightexpress.sunlight.utils.TimeUtil;

import java.util.UUID;

/**
 * A pending friend request.
 * <p>
 * Lives in memory only, like the PTP request queue: a request nobody answered before the timeout
 * is not worth a database row, and writing one per attempt would hand the database exactly the
 * flooding problem the request limits exist to prevent.
 */
public class FriendRequest {

    private final UserInfo sender;
    private final UserInfo target;
    private final long expireDate;

    public FriendRequest(UserInfo sender, UserInfo target, long expireDate) {
        this.sender = sender;
        this.target = target;
        this.expireDate = expireDate;
    }

    public static FriendRequest create(UserInfo sender, UserInfo target, int timeoutSeconds) {
        return new FriendRequest(sender, target, TimeUtil.createFutureTimestamp(timeoutSeconds));
    }

    public UserInfo getSender() {
        return this.sender;
    }

    public UserInfo getTarget() {
        return this.target;
    }

    public UUID getSenderId() {
        return this.sender.id();
    }

    public UUID getTargetId() {
        return this.target.id();
    }

    public String getSenderName() {
        return this.sender.name();
    }

    public String getTargetName() {
        return this.target.name();
    }

    public long getExpireDate() {
        return this.expireDate;
    }

    public boolean isExpired() {
        return this.expireDate >= 0L && System.currentTimeMillis() > this.expireDate;
    }

    /** Whether this request runs from that sender to that target. */
    public boolean isFrom(UUID senderId, UUID targetId) {
        return this.getSenderId().equals(senderId) && this.getTargetId().equals(targetId);
    }

    /** Whether that player is the one who sent it. */
    public boolean isSentBy(UUID playerId) {
        return this.getSenderId().equals(playerId);
    }

    /** Whether that player is the one who receives it. */
    public boolean isSentTo(UUID playerId) {
        return this.getTargetId().equals(playerId);
    }

    /** Whether this request is the same pair as that one, ignoring direction. */
    public boolean isBetween(UUID playerId, UUID otherId) {
        return (this.getSenderId().equals(playerId) && this.getTargetId().equals(otherId))
                || (this.getSenderId().equals(otherId) && this.getTargetId().equals(playerId));
    }

    /** Whether that name matches the sender. */
    public boolean isSender(String name) {
        return name != null && this.sender.isUser(name);
    }

    /** Whether that name matches the receiver. */
    public boolean isTarget(String name) {
        return name != null && this.target.isUser(name);
    }

    public FriendRequestHandle toHandle() {
        return new FriendRequestHandle(this.getSenderId(), this.getSenderName(), this.getTargetId(),
                this.getTargetName(), this.getExpireDate());
    }
}