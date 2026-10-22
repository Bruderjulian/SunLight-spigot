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
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteDraft;

public class VoteWizardQuestionDialog extends Dialog<VoteDraft> {

    private static final String JSON_VALUE = "value";

    private static final TextLocale TITLE = LangEntry.builder("Voting.Dialog.Wizard.Question.Title")
            .text("Vote Wizard — Question");

    private static final DialogElementLocale BODY = LangEntry.builder("Voting.Dialog.Wizard.Question.Body")
            .dialogElement(400, "Enter the question players will vote on.");

    private static final TextLocale INPUT = LangEntry.builder("Voting.Dialog.Wizard.Question.Input")
            .text("Question");

    @Override
    public @NotNull WrappedDialog create(@NotNull Player player, @NotNull VoteDraft draft) {
        return Dialogs.builder()
                .base(DialogBases.builder(TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(DialogInputs.text(JSON_VALUE, INPUT).maxLength(128).initial(draft.getQuestion()).build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null) {
                        return;
                    }
                    final String value = nbtHolder.getText(JSON_VALUE, "").trim();
                    if (!value.isBlank()) {
                        draft.setQuestion(value);
                    }
                    viewer.callback();
                })
                .build();
    }
}
