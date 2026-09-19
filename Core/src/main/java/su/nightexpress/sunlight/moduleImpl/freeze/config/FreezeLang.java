package su.nightexpress.sunlight.moduleImpl.freeze.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;

import static su.nightexpress.nightcore.util.text.tag.Tags.LIGHT_GRAY;
import static su.nightexpress.nightcore.util.text.tag.Tags.LIGHT_YELLOW;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_STATE;
import static su.nightexpress.sunlight.SLPlaceholders.PLAYER_DISPLAY_NAME;

public class FreezeLang implements LangContainer {

    public static final TextLocale COMMAND_FREEZE_DESC = LangEntry.builder("Command.Freeze.Desc")
        .text("Freeze players.");

    public static final MessageLocale COMMAND_FREEZE_NOTIFY = LangEntry.builder("Command.Freeze.Notify").chatMessage(
        LIGHT_GRAY.wrap("You have been " + LIGHT_YELLOW.wrap(GENERIC_STATE) + "."));

    public static final MessageLocale COMMAND_FREEZE_TARGET = LangEntry.builder("Command.Freeze.Target").chatMessage(
        LIGHT_GRAY.wrap(LIGHT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + " has been " + LIGHT_YELLOW.wrap(GENERIC_STATE) + "."));

    public static final MessageLocale ERROR_IMMUNE = LangEntry.builder("Command.Freeze.Error.Immune").chatMessage(
        LIGHT_GRAY.wrap(LIGHT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + " can not be frozen."));

    public static final MessageLocale ERROR_ACTION_BLOCKED = LangEntry.builder("Freeze.Error.ActionBlocked").chatMessage(
        LIGHT_GRAY.wrap("You are frozen and can not do this!"));

    public static final MessageLocale JOIN_FROZEN = LangEntry.builder("Freeze.Join").chatMessage(
        LIGHT_GRAY.wrap("You are still " + LIGHT_YELLOW.wrap("frozen") + ". Please wait for a staff member."));
}
