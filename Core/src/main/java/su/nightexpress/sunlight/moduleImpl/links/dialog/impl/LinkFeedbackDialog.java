package su.nightexpress.sunlight.moduleImpl.links.dialog.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;

public class LinkFeedbackDialog {

    private LinkFeedbackDialog() {
    }

    public static class Sound extends AbstractLinkTextDialog {

        private static final String JSON_SOUND = "sound";

        private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Sound.Title")
                .text(title("Link", "Sound"));

        private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Sound.Body")
                .dialogElement(400, "Enter the sound played on activation.", "",
                        SOFT_YELLOW.wrap("→") + " Format: SOUND;volume;pitch. Leave empty for silent.");

        private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Sound.Input.Value")
                .text("Sound");

        public Sound(@NotNull LinksModule module) {
            super(module, JSON_SOUND, TITLE, BODY, INPUT, 128);
        }

        @Override
        protected @NotNull String getCurrent(@NotNull Link link) {
            return link.getSound();
        }

        @Override
        protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
            String value = input.trim();
            if (!value.isBlank() && !LinksModule.isValidSound(value)) {
                return null;
            }
            link.setSound(value);
            return link.getSound();
        }
    }

    public static class Actionbar extends AbstractLinkTextDialog {

        private static final String JSON_ACTIONBAR = "actionbar";

        private static final TextLocale TITLE = LangEntry.builder("Links.Dialog.Actionbar.Title")
                .text(title("Link", "Actionbar"));

        private static final DialogElementLocale BODY = LangEntry.builder("Links.Dialog.Actionbar.Body")
                .dialogElement(400, "Enter the actionbar message shown on activation.", "",
                        SOFT_YELLOW.wrap("→") + " Leave empty to disable.");

        private static final TextLocale INPUT = LangEntry.builder("Links.Dialog.Actionbar.Input.Value")
                .text("Text");

        public Actionbar(@NotNull LinksModule module) {
            super(module, JSON_ACTIONBAR, TITLE, BODY, INPUT, 400);
        }

        @Override
        protected @NotNull String getCurrent(@NotNull Link link) {
            return link.getActionbar();
        }

        @Override
        protected @Nullable String apply(@NotNull Link link, @NotNull String input) {
            link.setActionbar(input);
            return link.getActionbar();
        }
    }
}
