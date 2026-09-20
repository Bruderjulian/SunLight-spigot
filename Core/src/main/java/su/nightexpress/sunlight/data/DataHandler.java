package su.nightexpress.sunlight.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import su.nightexpress.nightcore.db.AbstractDatabaseManager;
import su.nightexpress.nightcore.db.column.Column;
import su.nightexpress.nightcore.db.statement.condition.Operator;
import su.nightexpress.nightcore.db.statement.condition.Wheres;
import su.nightexpress.nightcore.db.statement.template.InsertStatement;
import su.nightexpress.nightcore.db.statement.template.SelectStatement;
import su.nightexpress.nightcore.db.statement.template.UpdateStatement;
import su.nightexpress.nightcore.db.table.Table;
import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.nightcore.user.data.UserDataSchema;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandKey;
import su.nightexpress.sunlight.data.serialize.CommandKeySerializer;
import su.nightexpress.sunlight.data.serialize.UserInfoSerializer;
import su.nightexpress.sunlight.user.SunUser;

import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DataHandler extends AbstractDatabaseManager<SunLightPlugin> implements UserDataSchema<SunUser> {

    public static final Gson GSON = new GsonBuilder()
            .enableComplexMapKeySerialization() // To trigger adapters for custom Map keys (used by commandCooldowns).
            .registerTypeAdapter(UserInfo.class, new UserInfoSerializer())
            .registerTypeAdapter(CommandKey.class, new CommandKeySerializer())
            .create();

    private final Table usersTable;

    public DataHandler(SunLightPlugin plugin) {
        super(plugin);

        this.usersTable = Table.builder(this.getTablePrefix() + "_users")
                .withColumn(UserColumns.ID)
                .withColumn(UserColumns.UUID)
                .withColumn(UserColumns.NAME)
                .withColumn(UserColumns.DATE_CREATED)
                .withColumn(UserColumns.LAST_ONLINE)
                .withColumn(UserColumns.INET_ADDRESS)
                .withColumn(UserColumns.COMMAND_COOLDOWNS)
                .withColumn(UserColumns.PROPERTIES)
                .build();
    }

    @Override
    public void onSynchronize() {
        this.synchronizer.syncAll();
    }

    @Override
    protected void onInitialize() {
        this.createTable(this.usersTable);

        this.dropColumn(this.usersTable, "ignoredUsers", "cooldowns", "settings");
    }

    @Override
    protected void onClose() {

    }

    @Override
    public void onPurge() {

    }

    @Override

    public Table getUsersTable() {
        return this.usersTable;
    }

    @Override

    public Column<UUID> getUserIdColumn() {
        return UserColumns.UUID;
    }

    @Override

    public Column<String> getUserNameColumn() {
        return UserColumns.NAME;
    }

    @Override

    public SelectStatement<SunUser> getUserSelectStatement() {
        return DataQueries.SELECT_USER;
    }

    @Override

    public UpdateStatement<SunUser> getUserUpdateStatement() {
        return DataQueries.UPDATE_USER;
    }

    @Override

    public UpdateStatement<SunUser> getUserTinyUpdateStatement() {
        return DataQueries.UPDATE_USER_TINY;
    }

    @Override

    public InsertStatement<SunUser> getUserInsertStatement() {
        return DataQueries.INSERT_USER;
    }

    public Optional<UserInfo> loadProfile(String playerName) {
        return this.selectFirst(this.usersTable, DataQueries.SELECT_PROFILE,
                Wheres.where(UserColumns.NAME, Operator.EQUALS_IGNORE_CASE, o -> playerName));
    }

    public Optional<InetAddress> loadInetAddress(UUID playerId) {
        return this.selectFirst(this.usersTable, DataQueries.SELECT_INET,
                Wheres.whereUUID(UserColumns.UUID, o -> playerId));
    }

    public List<UserInfo> getProfilesByInet(InetAddress address) {
        String host = address.getHostAddress();

        return this.selectWhere(this.usersTable, DataQueries.SELECT_PROFILE,
                Wheres.where(UserColumns.INET_ADDRESS, Operator.EQUALS, o -> host));
    }
}
