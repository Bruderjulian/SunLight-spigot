package su.nightexpress.sunlight.moduleImpl.afk.core;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.BooleanLocale;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class AfkLang implements LangContainer {

    public static final TextLocale COMMAND_AFK_OFF_DESC    = LangEntry.builder("Afk.Command.Afk.Off.Desc").text("Exit AFK mode.");
    public static final TextLocale COMMAND_AFK_ON_DESC     = LangEntry.builder("Afk.Command.Afk.On.Desc").text("Enter AFK mode.");
    public static final TextLocale COMMAND_AFK_TOGGLE_DESC = LangEntry.builder("Afk.Command.Afk.Toggle.Desc").text("Toggle AFK mode.");
    public static final TextLocale COMMAND_AFK_LIST_DESC   = LangEntry.builder("Afk.Command.Afk.List.Desc").text("List all AFK players.");

    public static final BooleanLocale PLACEHOLDER_MODE = LangEntry.builder("Afk.Placeholder.Mode").bool(
        DARK_GRAY.wrap(" [AFK]"),
        ""
    );

    public static final MessageLocale AFK_TOGGLE_FEEDBACK = LangEntry.builder("Afk.Command.Afk.Done.Others").chatMessage(
        GRAY.wrap("You have set " + ORANGE.wrap(PLAYER_DISPLAY_NAME) + "'s AFK mode on " + WHITE.wrap(GENERIC_STATE) + "."));

    public static final MessageLocale AFK_ENTER_BROADCAST = LangEntry.builder("Afk.Mode.Enter").chatMessage(
        GRAY.wrap(WHITE.wrap(PLAYER_DISPLAY_NAME) + " is AFK now."));

    public static final MessageLocale AFK_EXIT_BROADCAST = LangEntry.builder("Afk.Mode.Exit").chatMessage(
        GRAY.wrap(WHITE.wrap(PLAYER_DISPLAY_NAME) + " is back. (was AFK for " + ORANGE.wrap(GENERIC_TIME) + ")"));

    public static final MessageLocale AFK_LIST_EMPTY = LangEntry.builder("Afk.Command.Afk.List.Empty").chatMessage(
        GRAY.wrap("There are no AFK players."));

    public static final MessageLocale AFK_LIST = LangEntry.builder("Afk.Command.Afk.List").chatMessage(
        DARK_GRAY.wrap("[" + ORANGE.wrap("AFK") + "]") + " " + GRAY.wrap("Online AFK players: ") + WHITE.wrap(GENERIC_LIST));

    public static final MessageLocale KICK_WARNING = LangEntry.builder("Afk.Kick.Warning").chatMessage(
        SOFT_RED.wrap("You're AFK for too long! You will be kicked in " + WHITE.wrap(GENERIC_TIME) + "."));

    public static final MessageLocale KICK_REJOIN = LangEntry.builder("Afk.Kick.Rejoin").chatMessage(
        SOFT_RED.wrap("You have just been kicked for idling, please wait before joining back."));

    public static final MessageLocale ERROR_ACTION_BLOCKED = LangEntry.builder("Afk.Error.ActionBlocked").chatMessage(
        SOFT_RED.wrap("You cannot do this while being AFK!"));
}