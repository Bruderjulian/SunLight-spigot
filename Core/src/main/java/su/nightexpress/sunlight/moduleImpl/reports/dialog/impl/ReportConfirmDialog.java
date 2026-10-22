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
import su.nightexpress.nightcore.ui.dialog.build.DialogTypes;
import su.nightexpress.nightcore.ui.dialog.wrap.Dialog;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportCategory;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportDraft;

/**
 * The last step. Nothing is written until this screen is confirmed, and it calls
 * {@code ReportsModule#submit} — the same entry point the command form uses — so every guard in
 * the module still applies.
 */
public class ReportConfirmDialog extends Dialog<ReportDraft> {

    private final ReportsModule module;

    public ReportConfirmDialog(ReportsModule module) {
        this.module = module;
    }

    @Override
    public WrappedDialog create(Player player, ReportDraft draft) {
        ReportCategory category = this.module.getCategory(draft.categoryId());
        String categoryName = category == null ? draft.categoryId() : category.getDisplay();

        DialogElementLocale body = LangEntry.builder("Reports.Dialog.Confirm.Body").dialogElement(400,
                "Report " + draft.targetName() + " for " + categoryName + "?",
                "",
                "\"" + draft.details() + "\"",
                "",
                "Staff will be told about this. Check the text before confirming.");

        return Dialogs.builder()
                .base(DialogBases.builder(ReportsLang.DIALOG_CONFIRM_TITLE)
                        .body(DialogBodies.plainMessage(body))
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    this.module.submit(viewer.getPlayer(), draft.targetName(), draft.categoryId(), draft.details());
                    viewer.close();
                })
                .build();
    }
}
