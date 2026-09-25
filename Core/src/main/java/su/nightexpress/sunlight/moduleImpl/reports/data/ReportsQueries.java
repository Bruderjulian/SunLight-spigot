package su.nightexpress.sunlight.moduleImpl.reports.data;

import su.nightexpress.nightcore.db.statement.RowMapper;
import su.nightexpress.nightcore.db.statement.template.InsertStatement;
import su.nightexpress.nightcore.db.statement.template.SelectStatement;
import su.nightexpress.nightcore.db.statement.template.UpdateStatement;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportNote;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportStatus;
import su.nightexpress.sunlight.utils.Utils;

import java.sql.SQLException;
import java.util.UUID;

import static su.nightexpress.sunlight.moduleImpl.reports.data.ReportsDataManager.*;

public class ReportsQueries {

    public static final RowMapper<Report> REPORT_LOADER = resultSet -> {
        try {
            UUID id = readUuid(resultSet, COLUMN_REPORT_ID.getName());
            if (id == null)
                return null;

            UUID reporterId = readUuid(resultSet, COLUMN_REPORTER_ID.getName());
            if (reporterId == null)
                return null;

            String targetName = resultSet.getString(COLUMN_TARGET_NAME.getName());
            if (targetName == null)
                return null;

            ReportStatus status = Utils.enumValueOf(resultSet.getString(COLUMN_STATUS.getName()),
                    ReportStatus.class);
            if (status == null)
                return null;

            return new Report(
                    id,
                    reporterId,
                    resultSet.getString(COLUMN_REPORTER_NAME.getName()),
                    readUuid(resultSet, COLUMN_TARGET_ID.getName()),
                    targetName,
                    resultSet.getString(COLUMN_CATEGORY.getName()),
                    resultSet.getString(COLUMN_DETAILS.getName()),
                    status,
                    readUuid(resultSet, COLUMN_STAFF_ID.getName()),
                    resultSet.getString(COLUMN_STAFF_NAME.getName()),
                    resultSet.getLong(COLUMN_CREATE_DATE.getName()),
                    resultSet.getLong(COLUMN_UPDATE_DATE.getName()),
                    resultSet.getBoolean(COLUMN_REWARDED.getName()),
                    resultSet.getString(COLUMN_LAST_WORLD.getName()),
                    resultSet.getDouble(COLUMN_LAST_X.getName()),
                    resultSet.getDouble(COLUMN_LAST_Y.getName()),
                    resultSet.getDouble(COLUMN_LAST_Z.getName()),
                    resultSet.getInt(COLUMN_NOTE_COUNT.getName())
            );
        } catch (SQLException | IllegalArgumentException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    public static final RowMapper<ReportNote> NOTE_LOADER = resultSet -> {
        try {
            UUID id = readUuid(resultSet, COLUMN_NOTE_ID.getName());
            UUID reportId = readUuid(resultSet, COLUMN_NOTE_REPORT_ID.getName());
            if (id == null || reportId == null)
                return null;

            String authorName = resultSet.getString(COLUMN_NOTE_AUTHOR_NAME.getName());

            return new ReportNote(
                    id,
                    reportId,
                    readUuid(resultSet, COLUMN_NOTE_AUTHOR_ID.getName()),
                    authorName == null ? "CONSOLE" : authorName,
                    resultSet.getString(COLUMN_NOTE_TEXT.getName()),
                    resultSet.getLong(COLUMN_NOTE_DATE.getName())
            );
        } catch (SQLException | IllegalArgumentException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    public static final SelectStatement<Report> SELECT_REPORT = SelectStatement.builder(REPORT_LOADER).build();
    public static final SelectStatement<ReportNote> SELECT_NOTE = SelectStatement.builder(NOTE_LOADER).build();

    public static final InsertStatement<Report> INSERT_REPORT = InsertStatement.builder(Report.class)
            .setUUID(COLUMN_REPORT_ID, Report::getId)
            .setUUID(COLUMN_REPORTER_ID, Report::getReporterId)
            .setString(COLUMN_REPORTER_NAME, Report::getReporterName)
            .setUUID(COLUMN_TARGET_ID, Report::getTargetId)
            .setString(COLUMN_TARGET_NAME, Report::getTargetName)
            .setString(COLUMN_CATEGORY, Report::getCategoryId)
            .setString(COLUMN_DETAILS, Report::getDetails)
            .setString(COLUMN_STATUS, report -> report.getStatus().name())
            .setUUID(COLUMN_STAFF_ID, Report::getStaffId)
            .setString(COLUMN_STAFF_NAME, Report::getStaffName)
            .setLong(COLUMN_CREATE_DATE, Report::getCreateDate)
            .setLong(COLUMN_UPDATE_DATE, Report::getUpdateDate)
            .setBoolean(COLUMN_REWARDED, Report::isRewarded)
            .setString(COLUMN_LAST_WORLD, Report::getLastWorld)
            .setDouble(COLUMN_LAST_X, Report::getLastX)
            .setDouble(COLUMN_LAST_Y, Report::getLastY)
            .setDouble(COLUMN_LAST_Z, Report::getLastZ)
            .setInt(COLUMN_NOTE_COUNT, Report::getNoteCount)
            .build();

    public static final UpdateStatement<Report> UPDATE_REPORT = UpdateStatement.builder(Report.class)
            .setString(COLUMN_REPORTER_NAME, Report::getReporterName)
            .setString(COLUMN_DETAILS, Report::getDetails)
            .setString(COLUMN_STATUS, report -> report.getStatus().name())
            .setUUID(COLUMN_STAFF_ID, Report::getStaffId)
            .setString(COLUMN_STAFF_NAME, Report::getStaffName)
            .setLong(COLUMN_UPDATE_DATE, Report::getUpdateDate)
            .setBoolean(COLUMN_REWARDED, Report::isRewarded)
            .setString(COLUMN_LAST_WORLD, Report::getLastWorld)
            .setDouble(COLUMN_LAST_X, Report::getLastX)
            .setDouble(COLUMN_LAST_Y, Report::getLastY)
            .setDouble(COLUMN_LAST_Z, Report::getLastZ)
            .setInt(COLUMN_NOTE_COUNT, Report::getNoteCount)
            .build();

    public static final InsertStatement<ReportNote> INSERT_NOTE = InsertStatement.builder(ReportNote.class)
            .setUUID(COLUMN_NOTE_ID, ReportNote::getId)
            .setUUID(COLUMN_NOTE_REPORT_ID, ReportNote::getReportId)
            .setUUID(COLUMN_NOTE_AUTHOR_ID, ReportNote::getAuthorId)
            .setString(COLUMN_NOTE_AUTHOR_NAME, ReportNote::getAuthorName)
            .setString(COLUMN_NOTE_TEXT, ReportNote::getText)
            .setLong(COLUMN_NOTE_DATE, ReportNote::getDate)
            .build();

    private static UUID readUuid(java.sql.ResultSet resultSet, String column) throws SQLException {
        String raw = resultSet.getString(column);
        if (raw == null || raw.isBlank())
            return null;

        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private ReportsQueries() {
    }
}
