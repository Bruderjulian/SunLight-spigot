package su.nightexpress.sunlight.moduleImpl.nick.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class NickLang implements LangContainer {

    public static final TextLocale COMMAND_NICK_ROOT_DESC = LangEntry.builder("Command.Nick.Root.Desc")
        .text("Nick commands.");

    public static final TextLocale COMMAND_NICK_CLEAR_DESC = LangEntry.builder("Command.Nick.Clear.Desc")
        .text("Remove custom name.");

    public static final TextLocale COMMAND_NICK_SET_DESC = LangEntry.builder("Command.Nick.Set.Desc")
        .text("Set player's custom name.");

    public static final TextLocale COMMAND_NICK_CHANGE_DESC = LangEntry.builder("Command.Nick.Change.Desc")
        .text("Set custom name.");

    public static final MessageLocale COMMAND_NICK_CLEAR_TARGET = LangEntry.builder("Command.Nick.Clear.Target")
        .chatMessage(
            GRAY.wrap("Removed " + SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + "'s custom name."));

    public static final MessageLocale COMMAND_NICK_CLEAR_NOTIFY = LangEntry.builder("Command.Nick.Clear.Notify")
        .chatMessage(
            GRAY.wrap("You custom name has been removed."));

    public static final MessageLocale COMMAND_NICK_SET_TARGET = LangEntry.builder("Command.Nick.Set.Target")
        .chatMessage(
            GRAY.wrap("Set " + SOFT_YELLOW.wrap(PLAYER_NAME) + "'s custom name to " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_NICK_SET_NOTIFY = LangEntry.builder("Command.Nick.Set.Notify")
        .chatMessage(
            GRAY.wrap("You got a new custom name: " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_NICK_CHANGE_DONE = LangEntry.builder("Command.Nick.Change.Done")
        .chatMessage(
            GRAY.wrap("You changed your custom name to " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_NICK_CHANGE_ERROR_BAD_WORDS = LangEntry.builder("Command.Nick.Error.BadWords")
        .chatMessage(
            SOFT_RED.wrap("This name is not allowed."));

    public static final MessageLocale COMMAND_NICK_CHANGE_ERROR_REGEX = LangEntry.builder("Command.Nick.Error.Regex")
        .chatMessage(
            SOFT_RED.wrap("Name contains forbidden characters."));

    public static final MessageLocale COMMAND_NICK_CHANGE_ERROR_TOO_LONG = LangEntry.builder("Command.Nick.Error.TooLong")
        .chatMessage(
            GRAY.wrap("Name can't be no longer than " + SOFT_RED.wrap(GENERIC_AMOUNT) + " characters."));

    public static final MessageLocale COMMAND_NICK_CHANGE_ERROR_TOO_SHORT = LangEntry.builder("Command.Nick.Error.TooShort")
        .chatMessage(
            SOFT_RED.wrap("Name can't be shorted than " + SOFT_RED.wrap(GENERIC_AMOUNT) + " characters."));
}
