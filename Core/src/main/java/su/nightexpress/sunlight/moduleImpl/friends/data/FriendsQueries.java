package su.nightexpress.sunlight.moduleImpl.friends.data;

import su.nightexpress.nightcore.db.statement.RowMapper;
import su.nightexpress.nightcore.db.statement.template.InsertStatement;
import su.nightexpress.nightcore.db.statement.template.SelectStatement;
import su.nightexpress.nightcore.db.statement.template.UpdateStatement;
import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.moduleImpl.friends.model.BlockRecord;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRecord;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static su.nightexpress.sunlight.moduleImpl.friends.data.FriendsColumns.*;

public class FriendsQueries {

    /**
     * Rows are read defensively: a row whose uuids do not parse is dropped rather than aborting
     * the load, because one unreadable row must not cost a player every other friendship they have.
     */
    public static final RowMapper<FriendRecord> FRIEND_LOADER = resultSet -> {
        try {
            UUID ownerId = readUuid(resultSet, OWNER_ID.getName());
            UUID friendId = readUuid(resultSet, FRIEND_ID.getName());
            String ownerName = resultSet.getString(OWNER_NAME.getName());
            String friendName = resultSet.getString(FRIEND_NAME.getName());

            if (ownerId == null || friendId == null || ownerName == null || friendName == null) {
                return null;
            }
            if (ownerId.equals(friendId)) {
                // A self-friendship is meaningless and would inflate every count that reads this row.
                return null;
            }

            return new FriendRecord(new UserInfo(ownerId, ownerName), new UserInfo(friendId, friendName),
                    resultSet.getLong(CREATE_DATE.getName()),
                    resultSet.getBoolean(FAVORITE.getName()));
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    public static final RowMapper<BlockRecord> BLOCK_LOADER = resultSet -> {
        try {
            UUID blockerId = readUuid(resultSet, BLOCKER_ID.getName());
            UUID blockedId = readUuid(resultSet, BLOCKED_ID.getName());
            String blockerName = resultSet.getString(BLOCKER_NAME.getName());
            String blockedName = resultSet.getString(BLOCKED_NAME.getName());

            if (blockerId == null || blockedId == null || blockerName == null || blockedName == null) {
                return null;
            }
            if (blockerId.equals(blockedId)) {
                return null;
            }

            return new BlockRecord(new UserInfo(blockerId, blockerName), new UserInfo(blockedId, blockedName),
                    resultSet.getLong(BLOCK_CREATE_DATE.getName()));
        } catch (SQLException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    public static final SelectStatement<FriendRecord> SELECT_FRIEND = SelectStatement
            .builder(FRIEND_LOADER)
            .build();

    public static final SelectStatement<BlockRecord> SELECT_BLOCK = SelectStatement
            .builder(BLOCK_LOADER)
            .build();

    public static final InsertStatement<FriendRecord> INSERT_FRIEND = InsertStatement
            .builder(FriendRecord.class)
            .setUUID(OWNER_ID, FriendRecord::getOwnerId)
            .setString(OWNER_NAME, record -> record.getOwner().name())
            .setUUID(FRIEND_ID, FriendRecord::getFriendId)
            .setString(FRIEND_NAME, record -> record.getFriend().name())
            .setLong(CREATE_DATE, FriendRecord::getCreateDate)
            .setBoolean(FAVORITE, FriendRecord::isFavorite)
            .build();

    public static final UpdateStatement<FriendRecord> UPDATE_FRIEND = UpdateStatement
            .builder(FriendRecord.class)
            .setString(OWNER_NAME, record -> record.getOwner().name())
            .setString(FRIEND_NAME, record -> record.getFriend().name())
            .setLong(CREATE_DATE, FriendRecord::getCreateDate)
            .setBoolean(FAVORITE, FriendRecord::isFavorite)
            .build();

    public static final InsertStatement<BlockRecord> INSERT_BLOCK = InsertStatement
            .builder(BlockRecord.class)
            .setUUID(BLOCKER_ID, BlockRecord::getBlockerId)
            .setString(BLOCKER_NAME, record -> record.getBlocker().name())
            .setUUID(BLOCKED_ID, BlockRecord::getBlockedId)
            .setString(BLOCKED_NAME, record -> record.getBlocked().name())
            .setLong(BLOCK_CREATE_DATE, BlockRecord::getCreateDate)
            .build();

    /**
     * Reads a uuid column that may be null, which is what the nullable-column reads look like
     * across SQLite and MySQL alike.
     */
    private static UUID readUuid(ResultSet resultSet, String column) throws SQLException {
        String raw = resultSet.getString(column);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private FriendsQueries() {
    }
}