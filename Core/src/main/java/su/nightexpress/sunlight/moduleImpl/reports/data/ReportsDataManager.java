package su.nightexpress.sunlight.moduleImpl.reports.data;

import su.nightexpress.nightcore.db.column.Column;
import su.nightexpress.nightcore.db.statement.condition.Operator;
import su.nightexpress.nightcore.db.statement.condition.Wheres;
import su.nightexpress.nightcore.db.table.Table;
import su.nightexpress.sunlight.data.DataHandler;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportNote;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportStatus;
import su.nightexpress.sunlight.utils.TimeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReportsDataManager {

    public static final Column<UUID> COLUMN_REPORT_ID = Column.uuidType("reportId").primaryKey().build();
    public static final Column<UUID> COLUMN_REPORTER_ID = Column.uuidType("reporterId").build();
    public static final Column<String> COLUMN_REPORTER_NAME = Column.stringType("reporterName", 32).build();
    public static final Column<UUID> COLUMN_TARGET_ID = Column.uuidType("targetId").nullable().build();
    public static final Column<String> COLUMN_TARGET_NAME = Column.stringType("targetName", 32).build();
    public static final Column<String> COLUMN_CATEGORY = Column.stringType("category", 32).build();
    public static final Column<String> COLUMN_DETAILS = Column.mediumText("details").build();
    public static final Column<String> COLUMN_STATUS = Column.stringType("status", 16).build();
    public static final Column<UUID> COLUMN_STAFF_ID = Column.uuidType("staffId").nullable().build();
    public static final Column<String> COLUMN_STAFF_NAME = Column.stringType("staffName", 64).nullable().build();
    public static final Column<Long> COLUMN_CREATE_DATE = Column.longType("createDate").build();
    public static final Column<Long> COLUMN_UPDATE_DATE = Column.longType("updateDate").build();
    public static final Column<Boolean> COLUMN_REWARDED = Column.booleanType("rewarded").build();
    public static final Column<String> COLUMN_LAST_WORLD = Column.stringType("lastWorld", 64).nullable().build();
    public static final Column<Double> COLUMN_LAST_X = Column.doubleType("lastX").build();
    public static final Column<Double> COLUMN_LAST_Y = Column.doubleType("lastY").build();
    public static final Column<Double> COLUMN_LAST_Z = Column.doubleType("lastZ").build();
    public static final Column<Integer> COLUMN_NOTE_COUNT = Column.intType("noteCount").build();

    public static final Column<UUID> COLUMN_NOTE_ID = Column.uuidType("noteId").primaryKey().build();
    public static final Column<UUID> COLUMN_NOTE_REPORT_ID = Column.uuidType("reportId").build();
    public static final Column<UUID> COLUMN_NOTE_AUTHOR_ID = Column.uuidType("authorId").nullable().build();
    public static final Column<String> COLUMN_NOTE_AUTHOR_NAME = Column.stringType("authorName", 64).build();
    public static final Column<String> COLUMN_NOTE_TEXT = Column.mediumText("note").build();
    public static final Column<Long> COLUMN_NOTE_DATE = Column.longType("date").build();

    private final DataHandler dataHandler;
    private final ReportsModule module;

    private Table tableReports;
    private Table tableNotes;

    public ReportsDataManager(DataHandler dataHandler, ReportsModule module) {
        this.dataHandler = dataHandler;
        this.module = module;
    }

    public void init(String tablePrefix) {
        this.tableReports = Table.builder(tablePrefix + "_reports")
                .withColumn(
                        COLUMN_REPORT_ID,
                        COLUMN_REPORTER_ID,
                        COLUMN_REPORTER_NAME,
                        COLUMN_TARGET_ID,
                        COLUMN_TARGET_NAME,
                        COLUMN_CATEGORY,
                        COLUMN_DETAILS,
                        COLUMN_STATUS,
                        COLUMN_STAFF_ID,
                        COLUMN_STAFF_NAME,
                        COLUMN_CREATE_DATE,
                        COLUMN_UPDATE_DATE,
                        COLUMN_REWARDED,
                        COLUMN_LAST_WORLD,
                        COLUMN_LAST_X,
                        COLUMN_LAST_Y,
                        COLUMN_LAST_Z,
                        COLUMN_NOTE_COUNT)
                .build();

        this.tableNotes = Table.builder(tablePrefix + "_report_notes")
                .withColumn(
                        COLUMN_NOTE_ID,
                        COLUMN_NOTE_REPORT_ID,
                        COLUMN_NOTE_AUTHOR_ID,
                        COLUMN_NOTE_AUTHOR_NAME,
                        COLUMN_NOTE_TEXT,
                        COLUMN_NOTE_DATE)
                .build();

        this.dataHandler.createTable(this.tableReports);
        this.dataHandler.createTable(this.tableNotes);

        this.purgeOldEntries();

        this.dataHandler.addTableSync(this.tableReports, this::syncReport);
        this.dataHandler.addTableSync(this.tableNotes, this::syncNote);
    }

    private void syncReport(ResultSet resultSet) {
        try {
            Report report = ReportsQueries.REPORT_LOADER.map(resultSet);
            if (report == null)
                return;

            this.module.stampCategoryDisplay(report);
            this.module.getRepository().upsertReport(report);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    private void syncNote(ResultSet resultSet) {
        try {
            ReportNote note = ReportsQueries.NOTE_LOADER.map(resultSet);
            if (note == null)
                return;

            this.module.getRepository().addNote(note);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Deletes reports concluded longer ago than the global purge period, plus every note old enough
     * to belong to something already gone. Reports still OPEN or CLAIMED are never purged, so a
     * neglected queue cannot silently lose evidence.
     */
    public void purgeOldEntries() {
        LocalDateTime deadline = TimeUtil.getCurrentDateTime()
                .minusDays(this.dataHandler.getConfig().getPurgePeriod());
        long deadlineMs = TimeUtil.toEpochMillis(deadline);

        for (ReportStatus status : List.of(ReportStatus.RESOLVED, ReportStatus.DENIED)) {
            this.dataHandler.delete(this.tableReports, Wheres
                    .where(COLUMN_STATUS, Operator.EQUALS, o -> status.name())
                    .and(COLUMN_UPDATE_DATE, Operator.SMALLER, o -> deadlineMs));
        }

        this.dataHandler.delete(this.tableNotes, Wheres.where(COLUMN_NOTE_DATE, Operator.SMALLER, o -> deadlineMs));
    }

    public List<Report> getReports() {
        return this.dataHandler.selectAny(this.tableReports, ReportsQueries.SELECT_REPORT);
    }

    public List<ReportNote> getNotes() {
        return this.dataHandler.selectAny(this.tableNotes, ReportsQueries.SELECT_NOTE);
    }

    public List<ReportNote> getNotes(UUID reportId) {
        return this.dataHandler.selectWhere(this.tableNotes, ReportsQueries.SELECT_NOTE,
                Wheres.whereUUID(COLUMN_NOTE_REPORT_ID, o -> reportId));
    }

    /**
     * Re-reads one row and reports whether it is still unrewarded. This is the cross-server half of
     * the reward guard: another server in the same sync window may already have paid.
     */
    public boolean isUnrewardedInDatabase(UUID reportId) {
        return this.dataHandler.selectFirst(this.tableReports, ReportsQueries.SELECT_REPORT,
                Wheres.whereUUID(COLUMN_REPORT_ID, o -> reportId)
                        .and(COLUMN_REWARDED, Operator.EQUALS, o -> false)).isPresent();
    }

    public void insertReport(Report report) {
        this.dataHandler.insert(this.tableReports, ReportsQueries.INSERT_REPORT, report);
    }

    public void updateReport(Report report) {
        this.dataHandler.update(this.tableReports, ReportsQueries.UPDATE_REPORT, report,
                Wheres.whereUUID(COLUMN_REPORT_ID, Report::getId));
    }

    public void deleteReport(Report report) {
        this.dataHandler.delete(this.tableNotes, Wheres.whereUUID(COLUMN_NOTE_REPORT_ID, Report::getId));
        this.dataHandler.delete(this.tableReports, report, Wheres.whereUUID(COLUMN_REPORT_ID, Report::getId));
    }

    public void insertNote(ReportNote note) {
        this.dataHandler.insert(this.tableNotes, ReportsQueries.INSERT_NOTE, note);
    }
}
