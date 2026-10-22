package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.Numbers;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

public class LinkPriorityDialog extends AbstractLinkTextDialog {

    private static final String JSON_PRIORITY = "priority";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Priority.Title")
            .text(title("Link", "Priority"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Priority.Body")
            .dialogElement(400, "Enter the priority of the link.", "",
                    SOFT_YELLOW.wrap("→") + " Links with a greater priority are listed first.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Priority.Input.Value")
            .text("Priority");

    public LinkPriorityDialog(@NotNull LinksModule module) {
        super(module, JSON_PRIORITY, TITLE, BODY, INPUT, 11);
    }

    @Override
    protected @NotNull String getCurrent(@NotNull Link link) {
        return String.valueOf(link.getPriority());
    }

    @Override
    protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
        return Numbers.parseInteger(input.trim())
                .map(priority -> {
                    link.setPriority(priority);
                    return input;
                })
                .orElse(null);
    }
}
