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

public class LinkCooldownDialog extends AbstractLinkTextDialog {

    private static final String JSON_COOLDOWN = "cooldown";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Cooldown.Title")
            .text(title("Link", "Cooldown"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Cooldown.Body")
            .dialogElement(400, "Enter the per-player cooldown in seconds.", "",
                    SOFT_YELLOW.wrap("→") + " Use 0 for no cooldown.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Cooldown.Input.Value")
            .text("Seconds");

    public LinkCooldownDialog(@NotNull LinksModule module) {
        super(module, JSON_COOLDOWN, TITLE, BODY, INPUT, 11);
    }

    @Override
    protected @NotNull String getCurrent(@NotNull Link link) {
        return String.valueOf(link.getCooldown());
    }

    @Override
    protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
        return Numbers.parseInteger(input.trim())
                .filter(value -> value >= 0)
                .map(cooldown -> {
                    link.setCooldown(cooldown);
                    return String.valueOf(cooldown);
                })
                .orElse(null);
    }
}
