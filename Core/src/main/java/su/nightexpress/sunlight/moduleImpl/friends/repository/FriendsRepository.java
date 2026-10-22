package su.nightexpress.sunlight.moduleImpl.friends.repository;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.moduleImpl.friends.model.BlockRecord;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRecord;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory index of the friendship graph and the block list.
 * <p>
 * Both directions of a friendship get their own entry, so every read below is a single map lookup
 * rather than a scan: a mutual friendship is two {@link FriendRecord} rows, one owned by each side.
 * The block map is keyed by blocker only, because a block is never mirrored.
 */
public class FriendsRepository {

    private final Map<UUID, Set<FriendRecord>> friendsByOwner;
    private final Map<UUID, Set<FriendRecord>> friendsByFriend;
    private final Map<UUID, Set<BlockRecord>> blocksByBlocker;

    public FriendsRepository() {
        this.friendsByOwner = new ConcurrentHashMap<>();
        this.friendsByFriend = new ConcurrentHashMap<>();
        this.blocksByBlocker = new ConcurrentHashMap<>();
    }

    public void clear() {
        this.friendsByOwner.clear();
        this.friendsByFriend.clear();
        this.blocksByBlocker.clear();
    }

    // Friendships

    public synchronized void upsert(FriendRecord record) {
        this.removeRecord(record);
        this.add(record);
    }

    private synchronized void add(FriendRecord record) {
        this.friendsByOwner.computeIfAbsent(record.getOwnerId(), key -> ConcurrentHashMap.newKeySet())
                .add(record);
        this.friendsByFriend.computeIfAbsent(record.getFriendId(), key -> ConcurrentHashMap.newKeySet())
                .add(record);
    }

    private synchronized void removeRecord(FriendRecord record) {
        Set<FriendRecord> owned = this.friendsByOwner.get(record.getOwnerId());
        if (owned != null) {
            owned.removeIf(existing -> existing.getFriendId().equals(record.getFriendId()));
        }
        Set<FriendRecord> friended = this.friendsByFriend.get(record.getFriendId());
        if (friended != null) {
            friended.removeIf(existing -> existing.getOwnerId().equals(record.getOwnerId()));
        }
    }

    public synchronized void remove(FriendRecord record) {
        this.removeRecord(record);
    }

    /**
     * That player's own side of every friendship they are in.
     * <p>
     * Ordered online first, then alphabetically, because that is what a menu or a list message
     * wants and re-sorting at every call site would eventually be forgotten at one of them.
     */
    public List<FriendRecord> getFriends(UUID playerId) {
        Set<FriendRecord> set = this.friendsByOwner.get(playerId);
        if (set == null || set.isEmpty()) {
            return List.of();
        }

        List<FriendRecord> list = new ArrayList<>(set);
        list.sort(Comparator
                .comparing((FriendRecord record) -> !isOnline(record.getFriendId()))
                .thenComparing(record -> record.getFriend().name(), String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public List<FriendRecord> getOnlineFriends(UUID playerId) {
        return this.getFriends(playerId).stream().filter(record -> isOnline(record.getFriendId())).toList();
    }

    /** The row describing what that player thinks of the other one, or {@code null}. */
    @Nullable
    public FriendRecord getFriend(UUID playerId, UUID otherId) {
        Set<FriendRecord> set = this.friendsByOwner.get(playerId);
        if (set == null) {
            return null;
        }
        return set.stream().filter(record -> record.getFriendId().equals(otherId)).findFirst().orElse(null);
    }

    public boolean isFriend(UUID playerId, UUID otherId) {
        return this.getFriend(playerId, otherId) != null;
    }

    /** Both sides of a friendship, so a removal can take them together. */
    public List<FriendRecord> getBothSides(UUID playerId, UUID otherId) {
        List<FriendRecord> list = new ArrayList<>(2);

        FriendRecord owned = this.getFriend(playerId, otherId);
        if (owned != null) {
            list.add(owned);
        }
        FriendRecord reversed = this.getFriend(otherId, playerId);
        if (reversed != null) {
            list.add(reversed);
        }

        return list;
    }

    @Nullable
    public FriendRecord getFavorite(UUID playerId) {
        Set<FriendRecord> set = this.friendsByOwner.get(playerId);
        if (set == null) {
            return null;
        }
        return set.stream().filter(FriendRecord::isFavorite).findFirst().orElse(null);
    }

    /**
     * Pins one friend and unpins whichever was pinned before.
     * <p>
     * Returns the row that now holds the pin so the caller can persist both rows.
     */
    @Nullable
    public synchronized FriendRecord setFavorite(UUID playerId, UUID friendId, boolean favorite) {
        FriendRecord target = this.getFriend(playerId, friendId);
        if (target == null) {
            return null;
        }

        if (favorite) {
            for (FriendRecord record : this.getFriends(playerId)) {
                if (record.isFavorite() && !record.getFriendId().equals(friendId)) {
                    record.setFavorite(false);
                }
            }
        }

        target.setFavorite(favorite);
        return target;
    }

    /** Every friendship on the server, for the admin command and the stats screen. */
    public List<FriendRecord> getAll() {
        List<FriendRecord> list = new ArrayList<>();
        this.friendsByOwner.values().forEach(list::addAll);
        return list;
    }

    public int size() {
        return this.friendsByOwner.values().stream().mapToInt(Set::size).sum();
    }

    // Blocks

    public synchronized void upsertBlock(BlockRecord block) {
        Set<BlockRecord> set = this.blocksByBlocker.computeIfAbsent(block.getBlockerId(),
                key -> ConcurrentHashMap.newKeySet());
        set.removeIf(existing -> existing.getBlockedId().equals(block.getBlockedId()));
        set.add(block);
    }

    public synchronized void removeBlock(BlockRecord block) {
        Set<BlockRecord> set = this.blocksByBlocker.get(block.getBlockerId());
        if (set != null) {
            set.removeIf(existing -> existing.getBlockedId().equals(block.getBlockedId()));
        }
    }

    /** That player's blocks. */
    public List<BlockRecord> getBlocks(UUID blockerId) {
        Set<BlockRecord> set = this.blocksByBlocker.get(blockerId);
        if (set == null || set.isEmpty()) {
            return List.of();
        }

        List<BlockRecord> list = new ArrayList<>(set);
        list.sort(Comparator.comparing((BlockRecord block) -> !isOnline(block.getBlockedId()))
                .thenComparing(block -> block.getBlocked().name(), String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    /** Whether that player has blocked the other one. One direction only. */
    public boolean isBlocked(UUID blockerId, UUID blockedId) {
        Set<BlockRecord> set = this.blocksByBlocker.get(blockerId);
        if (set == null) {
            return false;
        }
        return set.stream().anyMatch(block -> block.getBlockedId().equals(blockedId));
    }

    /** Whether either side has blocked the other, which is what actually stops a request. */
    public boolean isBlockedEitherWay(UUID playerId, UUID otherId) {
        return this.isBlocked(playerId, otherId) || this.isBlocked(otherId, playerId);
    }

    public List<BlockRecord> getAllBlocks() {
        List<BlockRecord> list = new ArrayList<>();
        this.blocksByBlocker.values().forEach(list::addAll);
        return list;
    }

    // Helpers

    public static boolean isOnline(UUID playerId) {
        return Bukkit.getPlayer(playerId) != null;
    }

    @Nullable
    public static Player getPlayer(UUID playerId) {
        return Bukkit.getPlayer(playerId);
    }

    @NotNull
    public static UserInfo infoOf(@NotNull Player player) {
        return UserInfo.of(player);
    }
}