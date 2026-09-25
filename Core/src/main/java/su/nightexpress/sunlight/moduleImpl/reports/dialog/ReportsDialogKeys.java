package su.nightexpress.sunlight.moduleImpl.reports.dialog;

import su.nightexpress.nightcore.ui.dialog.wrap.DialogKey;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;

public class ReportsDialogKeys {

    private ReportsDialogKeys() {
    }

    public static final DialogKey<Report> REPORT_NOTE = new DialogKey<>("report_note");
    public static final DialogKey<Report> REPORT_OUTCOME = new DialogKey<>("report_outcome");
}
