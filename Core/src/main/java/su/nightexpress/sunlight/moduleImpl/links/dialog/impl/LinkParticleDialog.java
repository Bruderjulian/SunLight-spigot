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
import su.nightexpress.nightcore.util.Numbers;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksLang;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

/**
 * Standalone dialog (not {@link AbstractLinkTextDialog}) because the particle has two fields:
 * the type and the amount. Both are validated before anything is stored.
 */
public class LinkParticleDialog extends Dialog<Link> {

    private static final String JSON_PARTICLE = "particle";
    private static final String JSON_COUNT    = "count";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Particle.Title")
            .text(title("Link", "Particle"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Particle.Body")
            .dialogElement(400, "Enter the particle spawned on activation.", "",
                    SOFT_YELLOW.wrap("→") + " Bukkit particle name. Leave empty for none.",
                    SOFT_YELLOW.wrap("→") + " Count 0-100. Advanced data (colours, materials)",
                    SOFT_YELLOW.wrap("→") + " can be edited in the config 'Particle' section.");

    private static final TextLocale INPUT_PARTICLE = LangEntry.builder("Links.Dialog.Particle.Input.Value")
            .text("Particle");

    private static final TextLocale INPUT_COUNT = LangEntry.builder("Links.Dialog.Particle.Input.Count")
            .text("Count");

    private final LinksModule module;

    public LinkParticleDialog(@NotNull LinksModule module) {
        this.module = module;
    }

    @Override
    public @NotNull WrappedDialog create(@NotNull Player player, @NotNull Link link) {
        return Dialogs.builder()
                .base(DialogBases.builder(TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(
                                DialogInputs.text(JSON_PARTICLE, INPUT_PARTICLE)
                                        .initial(link.getParticleName())
                                        .maxLength(64)
                                        .build(),
                                DialogInputs.text(JSON_COUNT, INPUT_COUNT)
                                        .initial(String.valueOf(link.getParticleCount()))
                                        .maxLength(3)
                                        .build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null) {
                        return;
                    }

                    String particle = nbtHolder.getText(JSON_PARTICLE, "").trim();
                    String countRaw = nbtHolder.getText(JSON_COUNT, "").trim();

                    int count = Numbers.parseInteger(countRaw).filter(value -> value >= 0 && value <= 100)
                            .orElse(-1);
                    if (!LinksModule.isValidParticle(particle) || count < 0) {
                        this.module.sendPrefixed(LinksLang.DIALOG_INPUT_INVALID, player,
                                replacer -> replacer.with(SLPlaceholders.GENERIC_VALUE,
                                        () -> particle + " x" + countRaw));
                        return;
                    }

                    link.setParticle(particle);
                    link.setParticleCount(count);
                    this.module.saveLink(link);
                    viewer.callback();
                })
                .build();
    }
}
