package su.nightexpress.sunlight.moduleImpl.reports.dialog.impl;

import org.bukkit.entity.Player;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
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
import su.nightexpress.sunlight.moduleImpl.reports.ReportsConfig;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;

public class ReportNoteDialog extends Dialog<Report> {

    private static final String JSON_NOTE = "note";

    private static final DialogElementLocale BODY = LangEntry.builder("Reports.Dialog.Note.Body").dialogElement(400,
            "Notes are permanent and are visible to other staff.",
            "",
            "They cannot be edited or removed once saved.");

    private final ReportsModule module;

    public ReportNoteDialog(ReportsModule module) {
        this.module = module;
    }

    @Override
    public WrappedDialog create(Player player, Report report) {
        return Dialogs.builder()
                .base(DialogBases.builder(ReportsLang.DIALOG_NOTE_TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(DialogInputs.text(JSON_NOTE, ReportsLang.DIALOG_NOTE_INPUT)
                                .maxLength(ReportsConfig.NOTES_MAX_LENGTH.get())
                                .build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null)
                        return;

                    // NightCore dialogs have no declarative validator, so the emptiness and length
                    // checks live in addNote and must not write anything when they reject.
                    this.module.addNote(report, viewer.getPlayer(), nbtHolder.getText(JSON_NOTE, ""));
                    viewer.callback();
                })
                .build();
    }
}
