package su.nightexpress.sunlight.moduleImpl.socials.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_RED;

public class SocialsLang implements LangContainer {

    public static final MessageLocale DISCORD_NOT_LINKED = LangEntry.builder("Socials.Discord.NotLinked")
            .chatMessage(
                SOFT_RED.wrap("Discord integration is disabled (DiscordSRV not found)."));
}
