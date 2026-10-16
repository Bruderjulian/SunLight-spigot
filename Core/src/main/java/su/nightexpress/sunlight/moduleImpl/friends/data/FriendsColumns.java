package su.nightexpress.sunlight.moduleImpl.friends.data;

import su.nightexpress.nightcore.db.column.Column;

import java.util.UUID;

/** Columns of the friendship and block tables. */
public class FriendsColumns {

    // Friendships: one row per owner, so a mutual friendship is two rows.

    public static final Column<Integer> ID          = Column.intType("id").autoIncrement().primaryKey().build();
    public static final Column<UUID>    OWNER_ID    = Column.uuidType("ownerId").build();
    public static final Column<String>  OWNER_NAME  = Column.stringType("ownerName", 24).build();
    public static final Column<UUID>    FRIEND_ID   = Column.uuidType("friendId").build();
    public static final Column<String>  FRIEND_NAME = Column.stringType("friendName", 24).build();
    public static final Column<Long>    CREATE_DATE = Column.longType("createDate").defaultValue(0L).build();
    public static final Column<Boolean> FAVORITE    = Column.booleanType("isFavorite").defaultValue(false).build();

    // Blocks: one row per blocker, and a block is never mirrored.

    public static final Column<Integer> BLOCK_ID          = Column.intType("id").autoIncrement().primaryKey().build();
    public static final Column<UUID>    BLOCKER_ID        = Column.uuidType("blockerId").build();
    public static final Column<String>  BLOCKER_NAME      = Column.stringType("blockerName", 24).build();
    public static final Column<UUID>    BLOCKED_ID        = Column.uuidType("blockedId").build();
    public static final Column<String>  BLOCKED_NAME      = Column.stringType("blockedName", 24).build();
    public static final Column<Long>    BLOCK_CREATE_DATE = Column.longType("createDate").defaultValue(0L).build();

    private FriendsColumns() {
    }
}