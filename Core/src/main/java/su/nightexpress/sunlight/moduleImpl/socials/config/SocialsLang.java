package su.nightexpress.sunlight.moduleImpl.socials.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class SocialsLang implements LangContainer {

    public static final TextLocale COMMAND_SOCIALS_DESC = LangEntry.builder("Command.Socials.Root.Desc")
        .text("Show server links.");

    public static final TextLocale COMMAND_LINK_DESC = LangEntry.builder("Command.Socials.Link.Desc")
        .text("Show a server link.");

    public static final MessageLocale LINKS_HEADER = LangEntry.builder("Socials.Links.Header")
        .chatMessage(
            DARK_GRAY.wrap("[" + GOLD.wrap("Socials") + "]"));

    public static final MessageLocale LINK_LINE = LangEntry.builder("Socials.Link.Line")
        .chatMessage(
            GRAY.wrap("▸ ") + GENERIC_NAME);

    public static final MessageLocale LINKS_EMPTY = LangEntry.builder("Socials.Links.Empty")
        .chatMessage(
            SOFT_RED.wrap("No links available."));

    public static final MessageLocale ERROR_NO_PERMISSION = LangEntry.builder("Socials.Error.NoPermission")
        .chatMessage(
            SOFT_RED.wrap("You don't have permission to view this link."));

    public static final MessageLocale DISCORD_NOT_LINKED = LangEntry.builder("Socials.Discord.NotLinked")
        .chatMessage(
            SOFT_RED.wrap("Discord integration is disabled (DiscordSRV not found)."));
}
