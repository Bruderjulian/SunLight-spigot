package su.nightexpress.sunlight.moduleImpl.friends.data;

import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.db.statement.condition.Operator;
import su.nightexpress.nightcore.db.statement.condition.Wheres;
import su.nightexpress.nightcore.db.table.Table;
import su.nightexpress.sunlight.data.DataHandler;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.moduleImpl.friends.model.BlockRecord;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static su.nightexpress.sunlight.moduleImpl.friends.data.FriendsColumns.*;

public class FriendsDataManager {

    private final DataHandler dataHandler;
    private final FriendsModule module;

    private Table tableFriends;
    private Table tableBlocks;

    public FriendsDataManager(DataHandler dataHandler, FriendsModule module) {
        this.dataHandler = dataHandler;
        this.module = module;
    }

    public void init(String tablePrefix) {
        this.tableFriends = Table.builder(tablePrefix + "_relations")
                .withColumn(ID, OWNER_ID, OWNER_NAME, FRIEND_ID, FRIEND_NAME, CREATE_DATE, FAVORITE)
                .build();

        this.tableBlocks = Table.builder(tablePrefix + "_blocks")
                .withColumn(BLOCK_ID, BLOCKER_ID, BLOCKER_NAME, BLOCKED_ID, BLOCKED_NAME, BLOCK_CREATE_DATE)
                .build();

        this.dataHandler.createTable(this.tableFriends);
        this.dataHandler.createTable(this.tableBlocks);

        this.dataHandler.addTableSync(this.tableFriends, this::syncFriend);
        this.dataHandler.addTableSync(this.tableBlocks, this::syncBlock);
    }

    private void syncFriend(ResultSet resultSet) {
        try {
            FriendRecord record = FriendsQueries.FRIEND_LOADER.map(resultSet);
            if (record == null) {
                return;
            }
            this.module.getRepository().upsert(record);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void syncBlock(ResultSet resultSet) {
        try {
            BlockRecord block = FriendsQueries.BLOCK_LOADER.map(resultSet);
            if (block == null) {
                return;
            }
            this.module.getRepository().upsertBlock(block);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    // Friendships

    public List<FriendRecord> getFriends() {
        return this.dataHandler.selectAny(this.tableFriends, FriendsQueries.SELECT_FRIEND);
    }

    public List<FriendRecord> getFriends(UUID ownerId) {
        return this.dataHandler.selectWhere(this.tableFriends, FriendsQueries.SELECT_FRIEND,
                Wheres.whereUUID(OWNER_ID, o -> ownerId));
    }

    /**
     * Re-reads one row and reports whether it is still there.
     * <p>
     * This is the cross-server half of the friendship guard: another server in the same sync window
     * may already have made them friends, in which case this server's own row would be a duplicate.
     */
    public boolean existsInDatabase(UUID ownerId, UUID friendId) {
        return this.dataHandler.selectFirst(this.tableFriends, FriendsQueries.SELECT_FRIEND,
                Wheres.whereUUID(OWNER_ID, o -> ownerId)
                        .and(FRIEND_ID, Operator.EQUALS, o -> friendId.toString())).isPresent();
    }

    public void insertFriend(FriendRecord record) {
        this.dataHandler.insert(this.tableFriends, FriendsQueries.INSERT_FRIEND, record);
    }

    public void updateFriend(FriendRecord friend) {
        this.dataHandler.update(this.tableFriends, FriendsQueries.UPDATE_FRIEND, friend,
                Wheres.whereUUID(OWNER_ID, FriendRecord::getOwnerId)
                        .and(FRIEND_ID, Operator.EQUALS, record -> friend.getFriendId().toString()));
    }

    public void deleteFriend(FriendRecord record) {
        this.dataHandler.delete(this.tableFriends,
                Wheres.whereUUID(OWNER_ID, o -> record.getOwnerId())
                        .and(FRIEND_ID, Operator.EQUALS, o -> record.getFriendId().toString()));
    }

    public void deleteFriends(UUID ownerId) {
        this.dataHandler.delete(this.tableFriends, Wheres.whereUUID(OWNER_ID, o -> ownerId));
    }

    public void deleteFriends(String ownerName) {
        this.dataHandler.delete(this.tableFriends,
                Wheres.where(OWNER_NAME, Operator.EQUALS_IGNORE_CASE, o -> ownerName));
    }

    // Blocks

    public List<BlockRecord> getBlocks() {
        return this.dataHandler.selectAny(this.tableBlocks, FriendsQueries.SELECT_BLOCK);
    }

    public List<BlockRecord> getBlocks(UUID blockerId) {
        return this.dataHandler.selectWhere(this.tableBlocks, FriendsQueries.SELECT_BLOCK,
                Wheres.whereUUID(BLOCKER_ID, o -> blockerId));
    }

    public boolean existsBlockInDatabase(UUID blockerId, UUID blockedId) {
        return this.dataHandler.selectFirst(this.tableBlocks, FriendsQueries.SELECT_BLOCK,
                Wheres.whereUUID(BLOCKER_ID, o -> blockerId)
                        .and(BLOCKED_ID, Operator.EQUALS, o -> blockedId.toString())).isPresent();
    }

    public void insertBlock(BlockRecord block) {
        this.dataHandler.insert(this.tableBlocks, FriendsQueries.INSERT_BLOCK, block);
    }

    public void deleteBlock(BlockRecord block) {
        this.dataHandler.delete(this.tableBlocks,
                Wheres.whereUUID(BLOCKER_ID, o -> block.getBlockerId())
                        .and(BLOCKED_ID, Operator.EQUALS, o -> block.getBlockedId().toString()));
    }

    public void deleteBlocks(UUID blockerId) {
        this.dataHandler.delete(this.tableBlocks, Wheres.whereUUID(BLOCKER_ID, o -> blockerId));
    }

    /**
     * Drops rows naming a player who no longer has a user record, so a deleted account does not
     * leave a friendship that nothing can ever resolve again.
     * <p>
     * Off by default, because the users table is not the only record of a player: a row may name
     * somebody whose user record is written by another server in the same sync window. Turning this
     * on is only safe on a single-server setup.
     */
    public void purgeOrphans() {
        for (FriendRecord record : this.getFriends()) {
            if (this.isOrphan(record.getOwnerId()) || this.isOrphan(record.getFriendId())) {
                this.deleteFriend(record);
            }
        }

        for (BlockRecord block : this.getBlocks()) {
            if (this.isOrphan(block.getBlockerId()) || this.isOrphan(block.getBlockedId())) {
                this.deleteBlock(block);
            }
        }
    }

    private boolean isOrphan(@NotNull UUID playerId) {
        return !this.module.userManager().getDataAccessor().isExists(playerId);
    }
}