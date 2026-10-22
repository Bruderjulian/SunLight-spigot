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
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportDraft;

/**
 * Step three: describe what happened. Validation lives in the module, which is the same code the
 * command form runs through, so the two can never disagree about what a valid report is.
 */
public class ReportPickDetailsDialog extends Dialog<ReportDraft> {

    private static final String JSON_DETAILS = "details";

    private static final DialogElementLocale BODY = LangEntry.builder("Reports.Dialog.Details.Body")
            .dialogElement(400,
                    "Describe what this player did.",
                    "",
                    "Be specific: what happened, where, and anything that proves it.",
                    "Be at least " + ReportsConfig.LIMIT_DETAILS_MIN.get() + " characters long.");

    private final ReportsModule module;

    public ReportPickDetailsDialog(ReportsModule module) {
        this.module = module;
    }

    @Override
    public WrappedDialog create(Player player, ReportDraft draft) {
        return Dialogs.builder()
                .base(DialogBases.builder(ReportsLang.DIALOG_DETAILS_TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(DialogInputs.text(JSON_DETAILS, ReportsLang.DIALOG_DETAILS_INPUT)
                                .maxLength(ReportsConfig.LIMIT_DETAILS_MAX.get())
                                .initial(draft.details())
                                .build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null)
                        return;

                    String details = nbtHolder.getText(JSON_DETAILS, draft.details());
                    this.module.openConfirmDialog(viewer.getPlayer(), draft.withDetails(details),
                            viewer::callback);
                })
                .build();
    }
}
