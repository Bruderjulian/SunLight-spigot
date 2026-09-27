package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

public class LinkRewardDialog extends AbstractLinkTextDialog {

    private static final String JSON_REWARD = "reward";

    private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Reward.Title")
            .text(title("Link", "First-Click Reward"));

    private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Reward.Body")
            .dialogElement(400, "Enter the command run once per player on their first click.", "",
                    SOFT_YELLOW.wrap("→") + " Leave empty for no reward.");

    private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Reward.Input.Command")
            .text("Command");

    public LinkRewardDialog(@NotNull LinksModule module) {
        super(module, JSON_REWARD, TITLE, BODY, INPUT, 400);
    }

    @Override
    protected @NotNull String getCurrent(@NotNull Link link) {
        return String.join("\n", link.getFirstRewardCommands());
    }

    @Override
    protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
        link.setFirstRewardCommand(input);
        return String.join("\n", link.getFirstRewardCommands());
    }
}
