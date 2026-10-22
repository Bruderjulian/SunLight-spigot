package su.nightexpress.sunlight.moduleImpl.reports.dialog;

import su.nightexpress.nightcore.ui.dialog.wrap.DialogKey;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportDraft;

public class ReportsDialogKeys {

    private ReportsDialogKeys() {
    }

    /** Picks the category for a new report. Bound to the target name the reporter already chose. */
    public static final DialogKey<String> REPORT_PICK_CATEGORY = new DialogKey<>("report_pick_category");

    /** Writes the explanation. Bound to the target and category collected so far. */
    public static final DialogKey<ReportDraft> REPORT_PICK_DETAILS = new DialogKey<>("report_pick_details");

    /** Final review and confirm. The only screen that actually writes anything. */
    public static final DialogKey<ReportDraft> REPORT_CONFIRM = new DialogKey<>("report_confirm");

    public static final DialogKey<Report> REPORT_NOTE = new DialogKey<>("report_note");

    public static final DialogKey<Report> REPORT_OUTCOME = new DialogKey<>("report_outcome");
}
