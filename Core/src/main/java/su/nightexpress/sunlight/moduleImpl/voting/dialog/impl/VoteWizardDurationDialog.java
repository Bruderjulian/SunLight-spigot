package su.nightexpress.sunlight.moduleImpl.voting.dialog.impl;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
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
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteDraft;
import su.nightexpress.sunlight.utils.TimeUtil;

public class VoteWizardDurationDialog extends Dialog<VoteDraft> {

    private static final String JSON_VALUE = "value";

    private static final TextLocale TITLE = LangEntry.builder("Voting.Dialog.Wizard.Duration.Title")
            .text("Vote Wizard — Duration");

    private static final DialogElementLocale BODY = LangEntry.builder("Voting.Dialog.Wizard.Duration.Body")
            .dialogElement(400,
                    "How long should the vote stay open?",
                    "",
                    "Examples: 30s, 5m, 1h, 7d.");

    private static final TextLocale INPUT = LangEntry.builder("Voting.Dialog.Wizard.Duration.Input")
            .text("Duration");

    private final VotingModule module;

    public VoteWizardDurationDialog(final VotingModule module) {
        this.module = module;
    }

    @Override
    public @NotNull WrappedDialog create(@NotNull Player player, @NotNull VoteDraft draft) {
        return Dialogs.builder()
                .base(DialogBases.builder(TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(DialogInputs.text(JSON_VALUE, INPUT).maxLength(16)
                                .initial(draft.getDurationSeconds() + "s").build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null) {
                        return;
                    }
                    final String raw = nbtHolder.getText(JSON_VALUE, "").trim();
                    final long seconds = TimeUtil.parseDurationSeconds(raw);
                    if (seconds > 0L) {
                        draft.setDurationSeconds((int) Math.min(seconds, Integer.MAX_VALUE));
                    }
                    viewer.callback();
                })
                .build();
    }
}
