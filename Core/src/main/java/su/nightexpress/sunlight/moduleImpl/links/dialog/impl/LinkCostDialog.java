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

public class LinkCostDialog extends AbstractLinkTextDialog {

    private static final String JSON_COST = "cost";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Cost.Title")
            .text(title("Link", "Cost"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Cost.Body")
            .dialogElement(400, "Enter the Vault cost per use.", "",
                    SOFT_YELLOW.wrap("→") + " Use 0 for free.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Cost.Input.Value")
            .text("Cost");

    public LinkCostDialog(@NotNull LinksModule module) {
        super(module, JSON_COST, TITLE, BODY, INPUT, 32);
    }

    @Override
    protected @NotNull String getCurrent(@NotNull Link link) {
        return String.valueOf(link.getCost());
    }

    @Override
    protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
        return Numbers.parseDouble(input.trim())
                .filter(value -> value >= 0D)
                .map(cost -> {
                    link.setCost(cost);
                    return String.valueOf(cost);
                })
                .orElse(null);
    }
}
