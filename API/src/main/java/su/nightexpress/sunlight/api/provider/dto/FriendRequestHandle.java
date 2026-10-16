package su.nightexpress.sunlight.api.provider.dto;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A pending friend request as seen from one side of it.
 * <p>
 * Requests live in memory only, so they do not survive a reload and {@link #isExpired()} is the
 * authority on whether one is still actionable.
 */
public record FriendRequestHandle(@NotNull UUID senderId,
                                  @NotNull String senderName,
                                  @NotNull UUID targetId,
                                  @NotNull String targetName,
                                  long expireDate) {

    public boolean isExpired() {
        return System.currentTimeMillis() > this.expireDate;
    }
}