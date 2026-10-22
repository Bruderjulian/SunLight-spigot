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

public class VoteWizardOptionDialog extends Dialog<VoteDraft> {

    private static final String JSON_KEY = "key";
    private static final String JSON_DISPLAY = "display";

    private static final TextLocale TITLE = LangEntry.builder("Voting.Dialog.Wizard.Option.Title")
            .text("Vote Wizard — Add Choice");

    private static final DialogElementLocale BODY = LangEntry.builder("Voting.Dialog.Wizard.Option.Body")
            .dialogElement(400,
                    "Add one choice to the vote.",
                    "",
                    "Key is the id players type; display is what they see.");

    private static final TextLocale INPUT_KEY = LangEntry.builder("Voting.Dialog.Wizard.Option.Input.Key")
            .text("Choice Key");

    private static final TextLocale INPUT_DISPLAY = LangEntry.builder("Voting.Dialog.Wizard.Option.Input.Display")
            .text("Display Name");

    @Override
    public @NotNull WrappedDialog create(@NotNull Player player, @NotNull VoteDraft draft) {
        return Dialogs.builder()
                .base(DialogBases.builder(TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(
                                DialogInputs.text(JSON_KEY, INPUT_KEY).maxLength(32).build(),
                                DialogInputs.text(JSON_DISPLAY, INPUT_DISPLAY).maxLength(64).build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null) {
                        return;
                    }
                    final String key = nbtHolder.getText(JSON_KEY, "").trim();
                    final String display = nbtHolder.getText(JSON_DISPLAY, "").trim();
                    if (!key.isBlank()) {
                        draft.addOption(key, display.isBlank() ? key : display);
                    }
                    viewer.callback();
                })
                .build();
    }
}
