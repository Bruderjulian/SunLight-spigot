package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

public class LinkUrlDialog extends AbstractLinkTextDialog {

    private static final String JSON_URL = "url";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Url.Title").text(title("Link", "URL"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Url.Body")
            .dialogElement(400, "Enter the URL of the link.", "",
                    SOFT_YELLOW.wrap("→") + " Leave empty for a command-only link.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Url.Input.Url").text("URL");

    public LinkUrlDialog(@NotNull LinksModule module) {
        super(module, JSON_URL, TITLE, BODY, INPUT, 512);
    }

    @Override
    protected @NotNull String getCurrent(@NotNull Link link) {
        return link.getUrl();
    }

    @Override
    protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
        link.setUrl(input);
        return link.getUrl();
    }
}
