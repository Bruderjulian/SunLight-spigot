package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
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

/**
 * Shared shape of every single-text field editor in the link settings menu. Subclasses only decide what
 * the current value is and how a submitted value is applied; persisting it is handled here.
 */
public abstract class AbstractLinkTextDialog extends Dialog<Link> {

    private final LinksModule        module;
    private final String             jsonKey;
    private final TextLocale         title;
    private final DialogElementLocale body;
    private final TextLocale         input;
    private final int                maxLength;

    protected AbstractLinkTextDialog(@NotNull LinksModule module, @NotNull String jsonKey, @NotNull TextLocale title,
                                     @NotNull DialogElementLocale body, @NotNull TextLocale input, int maxLength) {
        this.module = module;
        this.jsonKey = jsonKey;
        this.title = title;
        this.body = body;
        this.input = input;
        this.maxLength = maxLength;
    }

    @Override
    public @NotNull WrappedDialog create(@NotNull Player player, @NotNull Link link) {
        return Dialogs.builder()
                .base(DialogBases.builder(this.title)
                        .body(DialogBodies.plainMessage(this.body))
                        .inputs(DialogInputs.text(this.jsonKey, this.input)
                                .initial(this.getCurrent(link))
                                .maxLength(this.maxLength)
                                .build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null) {
                        return;
                    }

                    if (this.apply(link, nbtHolder.getText(this.jsonKey, "")) == null) {
                        // Rejected: leave the link untouched so the admin can retry.
                        return;
                    }

                    this.module.saveLink(link);
                    viewer.callback();
                })
                .build();
    }

    protected @NotNull String getCurrent(@NotNull Link link) {
        return "";
    }

    /**
     * Applies the submitted value to the link.
     *
     * @return The stored value, or {@code null} when the input was rejected and nothing was changed.
     */
    protected abstract @Nullable String apply(@NotNull Link link, @NotNull String input);
}
