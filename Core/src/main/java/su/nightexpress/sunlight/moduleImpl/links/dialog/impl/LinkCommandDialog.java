package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

public class LinkCommandDialog extends AbstractLinkTextDialog {

    private static final String JSON_COMMAND = "command";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Command.Title")
            .text(title("Link", "Command"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Command.Body")
            .dialogElement(400, "Enter the command executed when the link is opened.", "",
                    SOFT_YELLOW.wrap("→") + " Leave empty for a URL-only link.",
                    SOFT_YELLOW.wrap("→") + " Placeholders: " + SOFT_YELLOW.wrap("%player_name%") + ", "
                            + SOFT_YELLOW.wrap("%player_uuid%") + ", " + SOFT_YELLOW.wrap("%link_id%") + ", "
                            + SOFT_YELLOW.wrap("%link_name%") + ", " + SOFT_YELLOW.wrap("%link_url%") + ".");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Command.Input.Command")
            .text("Command");

    public LinkCommandDialog(@NotNull LinksModule module) {
        super(module, JSON_COMMAND, TITLE, BODY, INPUT, 400);
    }

    @Override
    protected @NotNull String getCurrent(@NotNull Link link) {
        return link.getCommand();
    }

    @Override
    protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
        link.setCommand(input);
        return link.getCommand();
    }
}
