package su.nightexpress.sunlight.moduleImpl.friends.model;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.user.SunUser;

import java.util.UUID;

/**
 * A friendship that a player has blocked one side of.
 * <p>
 * Deliberately not merged into the friendship table: a block must survive the friendship being
 * deleted, and it has no favourite flag, create date per side, or symmetry to model.
 */
@NullMarked
public class BlockRecord {

    private final UserInfo blocker;
    private final UserInfo blocked;
    private final long createDate;

    public BlockRecord(UserInfo blocker, UserInfo blocked, long createDate) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createDate = createDate;
    }

    public static BlockRecord create(UserInfo blocker, UserInfo blocked) {
        return new BlockRecord(blocker, blocked, System.currentTimeMillis());
    }

    public UserInfo getBlocker() {
        return this.blocker;
    }

    public UserInfo getBlocked() {
        return this.blocked;
    }

    public UUID getBlockerId() {
        return this.blocker.id();
    }

    public UUID getBlockedId() {
        return this.blocked.id();
    }

    public long getCreateDate() {
        return this.createDate;
    }

    /**
     * Whether this block stands between those two players in either direction. Blocking is not
     * symmetric in storage but it is in effect: if either has blocked the other, neither may send
     * a request.
     */
    public boolean isBetween(@NotNull UUID playerId, @NotNull UUID otherId) {
        return this.involves(playerId) && this.involves(otherId);
    }

    public boolean involves(@NotNull UUID playerId) {
        return this.blocker.id().equals(playerId) || this.blocked.id().equals(playerId);
    }

    public boolean isBlocking(@NotNull UUID playerId) {
        return this.blocker.id().equals(playerId);
    }

    /** Whether the blocked player is online right now. */
    public boolean isBlockedOnline() {
        return Bukkit.getPlayer(this.blocked.id()) != null;
    }

    @Nullable
    public Player getBlockedPlayer() {
        return Bukkit.getPlayer(this.blocked.id());
    }
}