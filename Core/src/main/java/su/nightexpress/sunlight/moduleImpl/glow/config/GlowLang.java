package su.nightexpress.sunlight.moduleImpl.glow.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class GlowLang implements LangContainer {

    public static final TextLocale COMMAND_GLOW_ROOT_DESC = LangEntry.builder("Command.Glow.Root.Desc")
        .text("Glow commands.");

    public static final TextLocale COMMAND_GLOW_SET_DESC = LangEntry.builder("Command.Glow.Set.Desc")
        .text("Set glow color.");

    public static final TextLocale COMMAND_GLOW_CLEAR_DESC = LangEntry.builder("Command.Glow.Clear.Desc")
        .text("Remove glow color.");

    public static final TextLocale COMMAND_GLOW_LIST_DESC = LangEntry.builder("Command.Glow.List.Desc")
        .text("List available glow colors.");

    public static final MessageLocale COMMAND_GLOW_SET_DONE = LangEntry.builder("Command.Glow.Set.Done")
        .chatMessage(
            GRAY.wrap("Your glow color is now " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_GLOW_SET_TARGET = LangEntry.builder("Command.Glow.Set.Target")
        .chatMessage(
            GRAY.wrap("Set " + SOFT_YELLOW.wrap(PLAYER_NAME) + "'s glow color to " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_GLOW_SET_NOTIFY = LangEntry.builder("Command.Glow.Set.Notify")
        .chatMessage(
            GRAY.wrap("Your glow color was set to " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_GLOW_CLEAR_DONE = LangEntry.builder("Command.Glow.Clear.Done")
        .chatMessage(
            GRAY.wrap("Your glow color has been removed."));

    public static final MessageLocale COMMAND_GLOW_CLEAR_TARGET = LangEntry.builder("Command.Glow.Clear.Target")
        .chatMessage(
            GRAY.wrap("Removed " + SOFT_YELLOW.wrap(PLAYER_DISPLAY_NAME) + "'s glow color."));

    public static final MessageLocale COMMAND_GLOW_CLEAR_NOTIFY = LangEntry.builder("Command.Glow.Clear.Notify")
        .chatMessage(
            GRAY.wrap("Your glow color has been removed."));

    public static final MessageLocale COMMAND_GLOW_LIST_TITLE = LangEntry.builder("Command.Glow.List.Title")
        .chatMessage(
            GRAY.wrap("Available glow colors:"));

    public static final MessageLocale COMMAND_GLOW_LIST_ENTRY = LangEntry.builder("Command.Glow.List.Entry")
        .chatMessage(
            SOFT_YELLOW.wrap("●") + GRAY.wrap(" " + GENERIC_NAME + " (" + GENERIC_VALUE + ")"));

    public static final MessageLocale COMMAND_GLOW_ERROR_INVALID = LangEntry.builder("Command.Glow.Error.Invalid")
        .chatMessage(
            SOFT_RED.wrap("Unknown glow color: " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_GLOW_ERROR_NO_ACCESS = LangEntry.builder("Command.Glow.Error.NoAccess")
        .chatMessage(
            SOFT_RED.wrap("You don't have access to the " + SOFT_YELLOW.wrap(GENERIC_NAME) + " glow color."));
}
