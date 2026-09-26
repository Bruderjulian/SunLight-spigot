package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class LinkDisplayNameDialog extends AbstractLinkTextDialog {

    private static final String JSON_NAME = "name";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Name.Title").text(title("Link", "Name"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Name.Body")
            .dialogElement(400, "Enter the display name of the link.", "",
                    SOFT_YELLOW.wrap("→") + " MiniMessage tags are supported.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Name.Input.Name").text("Display Name");

    public LinkDisplayNameDialog(@NotNull LinksModule module) {
        super(module, JSON_NAME, TITLE, BODY, INPUT, 128);
    }

    @Override
    protected @NotNull String getCurrent(@NotNull Link link) {
        return link.getDisplay();
    }

    @Override
    protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
        if (input.isBlank()) {
            return null;
        }

        link.setDisplay(input);
        return link.getDisplay();
    }
}
