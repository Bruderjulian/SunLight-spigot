package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

public class LinkUsePermissionDialog extends AbstractLinkTextDialog {

    private static final String JSON_PERMISSION = "use_permission";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.UsePermission.Title")
            .text(title("Link", "Use Permission"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.UsePermission.Body")
            .dialogElement(400, "Enter the permission required to use the link.", "",
                    SOFT_YELLOW.wrap("→") + " Leave empty so anyone who can see it can use it.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.UsePermission.Input.Name")
            .text("Permission");

    public LinkUsePermissionDialog(@NotNull LinksModule module) {
        super(module, JSON_PERMISSION, TITLE, BODY, INPUT, 128);
    }

    @Override
    protected @NotNull String getCurrent(@NotNull Link link) {
        return link.getUsePermission();
    }

    @Override
    protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
        link.setUsePermission(input);
        return link.getUsePermission();
    }
}
