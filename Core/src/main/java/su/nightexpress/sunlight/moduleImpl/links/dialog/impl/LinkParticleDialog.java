package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

public class LinkParticleDialog extends AbstractLinkTextDialog {

    private static final String JSON_PARTICLE = "particle";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Particle.Title")
            .text(title("Link", "Particle"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Particle.Body")
            .dialogElement(400, "Enter the particle spawned on activation.", "",
                    SOFT_YELLOW.wrap("→") + " Bukkit particle name. Leave empty for none.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Particle.Input.Value")
            .text("Particle");

    public LinkParticleDialog(@NotNull LinksModule module) {
        super(module, JSON_PARTICLE, TITLE, BODY, INPUT, 64);
    }

    @Override
    protected @NotNull String getCurrent(@NotNull Link link) {
        return link.getParticle();
    }

    @Override
    protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
        String value = input.trim();
        if (!LinksModule.isValidParticle(value)) {
            return null;
        }
        link.setParticle(value);
        return link.getParticle();
    }
}
