package su.nightexpress.sunlight.module.homes.data;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.db.statement.condition.Operator;
import su.nightexpress.nightcore.db.statement.condition.Wheres;
import su.nightexpress.nightcore.db.statement.template.SelectStatement;
import su.nightexpress.nightcore.db.table.Table;
import su.nightexpress.sunlight.data.DataHandler;
import su.nightexpress.sunlight.data.UserColumns;
import su.nightexpress.sunlight.module.homes.HomesModule;
import su.nightexpress.sunlight.module.homes.impl.Home;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class HomeDataManager {

    // private final HomesModule module;
    private final DataHandler dataHandler;

    private Table tableHomes;

    public HomeDataManager(HomesModule module, DataHandler dataHandler) {
        // this.module = module;
        this.dataHandler = dataHandler;
    }

    public void init() {
        this.tableHomes = Table.builder(this.dataHandler.getTablePrefix() + "_homes")
                .withColumn(UserColumns.ID)
                .withColumn(HomeColumns.ID)
                .withColumn(HomeColumns.OWNER_ID)
                .withColumn(HomeColumns.OWNER_NAME)
                .withColumn(HomeColumns.NAME)
                .withColumn(HomeColumns.ICON_ID)
                .withColumn(HomeColumns.POSITION)
                .withColumn(HomeColumns.WORLD)
                .withColumn(HomeColumns.TYPE)
                .withColumn(HomeColumns.INVITED_PLAYERS)
                .withColumn(HomeColumns.FAVORITE)
                .build();

        this.dataHandler.createTable(this.tableHomes);
        this.dataHandler.dropColumn(this.tableHomes, "isRespawnPoint");

        if (this.dataHandler.hasColumn(this.tableHomes, HomeColumns.LOCATION)) {
            var oldHomes = this.dataHandler.selectAny(this.tableHomes,
                    SelectStatement.builder(HomesQueries.OLD_HOME_ROW_MAPPER).build());
            this.saveHomes(oldHomes);
            this.dataHandler.dropColumn(this.tableHomes, HomeColumns.LOCATION.getName());
        }
    }

    public List<Home> getHomes() {
        return this.dataHandler.selectAny(this.tableHomes,
                SelectStatement.builder(HomesQueries.HOME_ROW_MAPPER).build());
    }

    public List<Home> getHomes(UUID id) {
        return this.dataHandler.selectWhere(this.tableHomes,
                SelectStatement.builder(HomesQueries.HOME_ROW_MAPPER).build(),
                Wheres.where(HomeColumns.OWNER_ID, Operator.EQUALS, o -> id));
    }

    public void addHome(Home home) {
        this.dataHandler.insert(this.tableHomes, HomesQueries.HOME_INSERT, home);
    }

    public void saveHomes(Collection<Home> homes) {
        this.dataHandler.update(this.tableHomes, HomesQueries.HOME_UPDATE, homes,
                Wheres.where(HomeColumns.ID, Operator.EQUALS, Home::getId).and(HomeColumns.OWNER_ID, Operator.EQUALS,
                        home -> home.getOwner().id().toString()));
    }

    public void deleteHome(Home home) {
        this.deleteHome(home.getOwner().id(), home.getId());
    }

    public void deleteHomes(UUID userId) {
        this.dataHandler.delete(this.tableHomes, Wheres.whereUUID(HomeColumns.OWNER_ID, o -> userId));
    }

    public void deleteHomes(String userName) {
        this.dataHandler.delete(this.tableHomes, Wheres.where(HomeColumns.OWNER_NAME, Operator.EQUALS, o -> userName));
    }

    public void deleteHome(UUID ownerId, String homeId) {
        this.dataHandler.delete(this.tableHomes, Wheres.where(HomeColumns.ID, Operator.EQUALS, o -> homeId)
                .and(HomeColumns.OWNER_ID, Operator.EQUALS, o -> ownerId.toString()));
    }
}
