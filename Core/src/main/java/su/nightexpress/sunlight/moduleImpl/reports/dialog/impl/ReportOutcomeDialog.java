package su.nightexpress.sunlight.moduleImpl.reports.dialog.impl;

import org.bukkit.entity.Player;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.bridge.dialog.wrap.input.single.WrappedSingleOptionEntry;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.DialogActions;
import su.nightexpress.nightcore.ui.dialog.build.DialogBases;
import su.nightexpress.nightcore.ui.dialog.build.DialogBodies;
import su.nightexpress.nightcore.ui.dialog.build.DialogButtons;
import su.nightexpress.nightcore.ui.dialog.build.DialogInputs;
import su.nightexpress.nightcore.ui.dialog.build.DialogTypes;
import su.nightexpress.nightcore.ui.dialog.wrap.Dialog;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportStatus;

import java.util.List;

public class ReportOutcomeDialog extends Dialog<Report> {

    private static final String JSON_OUTCOME = "outcome";
    private static final String JSON_NOTE = "note";

    private static final DialogElementLocale BODY = LangEntry.builder("Reports.Dialog.Outcome.Body")
            .dialogElement(400,
                    "Choose how this report ends.",
                    "",
                    "Resolved means the report was justified and the reporter may be rewarded.",
                    "Denied means it was not justified and no reward is paid.");

    private final ReportsModule module;

    public ReportOutcomeDialog(ReportsModule module) {
        this.module = module;
    }

    @Override
    public WrappedDialog create(Player player, Report report) {
        List<WrappedSingleOptionEntry> entries = List.of(
                new WrappedSingleOptionEntry(ReportStatus.RESOLVED.name(), ReportsLang.OUTCOME_RESOLVED.text(), true),
                new WrappedSingleOptionEntry(ReportStatus.DENIED.name(), ReportsLang.OUTCOME_DENIED.text(), false));

        return Dialogs.builder()
                .base(DialogBases.builder(ReportsLang.DIALOG_OUTCOME_TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(
                                DialogInputs.singleOption(JSON_OUTCOME, ReportsLang.DIALOG_OUTCOME_INPUT, entries)
                                        .build(),
                                DialogInputs.text(JSON_NOTE, ReportsLang.DIALOG_OUTCOME_NOTE_INPUT)
                                        .maxLength(200)
                                        .build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null)
                        return;

                    String raw = nbtHolder.getText(JSON_OUTCOME, ReportStatus.RESOLVED.name());
                    ReportStatus status = ReportStatus.fromString(raw);
                    if (status == null || !status.isTerminal()) {
                        status = ReportStatus.RESOLVED;
                    }

                    String note = nbtHolder.getText(JSON_NOTE, "");
                    this.module.conclude(report, status, viewer.getPlayer(), note.isBlank() ? null : note);
                    viewer.callback();
                })
                .build();
    }
}
