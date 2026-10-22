package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.bridge.dialog.wrap.input.text.WrappedMultilineOptions;
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
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import java.util.Arrays;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Standalone dialog (not {@link AbstractLinkTextDialog}) because the reward is a command <i>list</i>:
 * one command per line. A single-line input would collapse the list on every edit.
 */
public class LinkRewardDialog extends Dialog<Link> {

    private static final String JSON_REWARD = "reward";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Reward.Title")
            .text(title("Link", "First-Click Reward"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Reward.Body")
            .dialogElement(400, "Enter the commands run once per player on their first click.", "",
                    SOFT_YELLOW.wrap("→") + " One command per line. Leave empty for no reward.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Reward.Input.Command")
            .text("Commands");

    private final LinksModule module;

    public LinkRewardDialog(@NotNull LinksModule module) {
        this.module = module;
    }

    @Override
    public @NotNull WrappedDialog create(@NotNull Player player, @NotNull Link link) {
        return Dialogs.builder()
                .base(DialogBases.builder(TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(DialogInputs.text(JSON_REWARD, INPUT)
                                .initial(String.join("\n", link.getFirstRewardCommands()))
                                .maxLength(400)
                                .width(200)
                                .multiline(new WrappedMultilineOptions(5, 50))
                                .build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null) {
                        return;
                    }

                    String input = nbtHolder.getText(JSON_REWARD).orElse(null);
                    if (input == null) {
                        return;
                    }

                    link.setFirstRewardCommands(Arrays.asList(input.split("\n")));
                    this.module.saveLink(link);
                    viewer.callback();
                })
                .build();
    }
}
