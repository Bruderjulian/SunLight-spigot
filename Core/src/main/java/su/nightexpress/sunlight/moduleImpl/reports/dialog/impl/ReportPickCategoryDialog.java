package su.nightexpress.sunlight.moduleImpl.reports.dialog.impl;

import org.bukkit.entity.Player;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.bridge.dialog.wrap.input.single.WrappedSingleOptionEntry;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
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
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportCategory;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportDraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Step two of the guided flow: choose a category. The dialog is bound to the already-chosen
 * target name, so a report cannot reach this screen without a target.
 */
public class ReportPickCategoryDialog extends Dialog<String> {

    private static final String JSON_CATEGORY = "category";

    private static final DialogElementLocale BODY = LangEntry.builder("Reports.Dialog.Category.Body")
            .dialogElement(400,
                    "Why are you reporting this player?",
                    "",
                    "Pick the closest match. You will be asked to describe what happened next.");

    private final ReportsModule module;

    public ReportPickCategoryDialog(ReportsModule module) {
        this.module = module;
    }

    @Override
    public WrappedDialog create(Player player, String targetName) {
        List<WrappedSingleOptionEntry> entries = new ArrayList<>();
        for (String id : this.module.getVisibleCategoryIds(player)) {
            ReportCategory category = this.module.getCategory(id);
            if (category != null) {
                entries.add(new WrappedSingleOptionEntry(id, category.getDisplay(), false));
            }
        }

        if (entries.isEmpty()) {
            this.module.sendPrefixed(ReportsLang.ERROR_CATEGORY_LOCKED, player);
            return Dialogs.builder()
                    .base(DialogBases.builder(ReportsLang.DIALOG_CATEGORY_TITLE).build())
                    .type(DialogTypes.multiAction(DialogButtons.ok()).build())
                    .build();
        }

        return Dialogs.builder()
                .base(DialogBases.builder(ReportsLang.DIALOG_CATEGORY_TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(DialogInputs.singleOption(JSON_CATEGORY, ReportsLang.DIALOG_CATEGORY_INPUT, entries)
                                .build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null)
                        return;

                    String categoryId = nbtHolder.getText(JSON_CATEGORY, entries.get(0).id());
                    this.module.openDetailsDialog(viewer.getPlayer(),
                            new ReportDraft(targetName, categoryId, ""), viewer::callback);
                })
                .build();
    }
}
