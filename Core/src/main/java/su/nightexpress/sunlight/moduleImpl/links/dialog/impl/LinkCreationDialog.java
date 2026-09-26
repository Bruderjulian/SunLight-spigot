package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

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
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

public class LinkCreationDialog extends Dialog<LinksModule> {

    private static final String JSON_ID = "id";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Creation.Title").text("Link Creation");

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Creation.Body")
            .dialogElement(400, "Enter an identifier for the new link.", "",
                    "Allowed characters: " + SOFT_YELLOW.wrap("a-z") + ", " + SOFT_YELLOW.wrap("0-9") + " and "
                            + SOFT_YELLOW.wrap("underscore") + ".",
                    "The ID becomes the link's own command, so it cannot be changed later.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Creation.Input.Id")
            .text("Link Identifier");

    @Override
    public @NotNull WrappedDialog create(@NotNull Player player, @NotNull LinksModule module) {
        return Dialogs.builder()
                .base(DialogBases.builder(TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(DialogInputs.text(JSON_ID, INPUT).maxLength(32).build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null) {
                        return;
                    }

                    String id = nbtHolder.getText(JSON_ID, "").trim();
                    if (!module.createLinkAndReport(player, id)) {
                        return;
                    }

                    viewer.callback();
                })
                .build();
    }
}
