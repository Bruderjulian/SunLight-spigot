package su.nightexpress.sunlight.moduleImpl.reports;

import su.nightexpress.nightcore.util.placeholder.TypedPlaceholder;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;

public class ReportsPlaceholders extends SLPlaceholders {

    public static final String REPORT_ID = "%report_id%";
    public static final String REPORT_DATE = "%date%";
    public static final String REPORT_STATUS = "%report_status%";
    public static final String REPORT_CATEGORY = "%report_category%";
    public static final String REPORT_REASON = "%report_reason%";
    public static final String REPORT_REPORTER = "%report_reporter%";
    public static final String REPORT_REPORTER_UUID = "%report_reporter_uuid%";
    public static final String REPORT_TARGET = "%report_target%";
    public static final String REPORT_TARGET_UUID = "%report_target_uuid%";
    public static final String REPORT_DATE_CREATED = "%report_date_created%";
    public static final String REPORT_DATE_UPDATED = "%report_date_updated%";
    public static final String REPORT_AGE = "%report_age%";
    public static final String REPORT_STAFF = "%report_staff%";
    public static final String REPORT_REWARDED = "%report_rewarded%";
    public static final String REPORT_NOTES = "%report_notes%";

    /** Staff identity, used by the audit strings written into the note trail. */
    public static final String SYSTEM_STAFF = "%staff%";
    public static final String SYSTEM_TYPE = "%type%";

    public static final TypedPlaceholder<Report> REPORT = TypedPlaceholder
            .builder(Report.class)
            .with(REPORT_ID, report -> report.getId().toString())
            .with(REPORT_DATE, report -> TimeFormats.formatDateTime(report.getCreateDate()))
            .with(REPORT_STATUS, report -> ReportsLang.STATUS.getLocalized(report.getStatus()))
            .with(REPORT_CATEGORY, Report::getCategoryDisplay)
            .with(REPORT_REASON, Report::getDetails)
            .with(REPORT_REPORTER, Report::getReporterName)
            .with(REPORT_REPORTER_UUID, report -> report.getReporterId().toString())
            .with(REPORT_TARGET, Report::getTargetName)
            .with(REPORT_TARGET_UUID, report -> report.getTargetId() == null ? ""
                    : report.getTargetId().toString())
            .with(REPORT_DATE_CREATED, report -> TimeFormats.formatDateTime(report.getCreateDate()))
            .with(REPORT_DATE_UPDATED, report -> TimeFormats.formatDateTime(report.getUpdateDate()))
            .with(REPORT_AGE, report -> TimeFormats.formatSince(report.getCreateDate(), TimeFormatType.LITERAL))
            .with(REPORT_STAFF, report -> report.getStaffName() == null ? "" : report.getStaffName())
            .with(REPORT_REWARDED, report -> report.isRewarded() ? ReportsLang.STATE_REWARDED.text()
                    : ReportsLang.STATE_NOT_REWARDED.text())
            .with(REPORT_NOTES, report -> String.valueOf(report.getNoteCount()))
            .build();

    private ReportsPlaceholders() {
    }
}
