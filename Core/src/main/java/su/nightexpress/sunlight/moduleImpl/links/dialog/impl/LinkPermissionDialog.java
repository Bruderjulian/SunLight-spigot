package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

public class LinkPermissionDialog extends AbstractLinkTextDialog {

    private static final String JSON_PERMISSION = "permission";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Permission.Title")
            .text(title("Link", "Permission"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Permission.Body")
            .dialogElement(400, "Enter the permission required to open the link.", "",
                    SOFT_YELLOW.wrap("→") + " Leave empty to require no extra permission.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Permission.Input.Name")
            .text("Permission");

    public LinkPermissionDialog(@NotNull LinksModule module) {
        super(module, JSON_PERMISSION, TITLE, BODY, INPUT, 128);
    }

    @Override
    protected @NotNull String getCurrent(@NotNull Link link) {
        return link.getPermission();
    }

    @Override
    protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
        link.setPermission(input);
        return link.getPermission();
    }
}
