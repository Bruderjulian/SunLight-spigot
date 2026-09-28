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

    public static final TextLocale COMMAND_GLOW_COLOR_DESC = LangEntry.builder("Command.Glow.Color.Desc")
            .text("Set glow color.");

    public static final TextLocale COMMAND_GLOW_PHASE_DESC = LangEntry.builder("Command.Glow.Phase.Desc")
            .text("Build a phased glow: each phase is a color with its own duration.");

    public static final TextLocale COMMAND_GLOW_PRESET_DESC = LangEntry.builder("Command.Glow.Preset.Desc")
            .text("Save, load, delete and list your phased glow presets.");

    public static final TextLocale COMMAND_GLOW_ON_DESC = LangEntry.builder("Command.Glow.On.Desc")
            .text("Enable your glow.");

    public static final TextLocale COMMAND_GLOW_OFF_DESC = LangEntry.builder("Command.Glow.Off.Desc")
            .text("Disable your glow.");

    public static final TextLocale MENU_TITLE = LangEntry.builder("Glow.Menu.Title")
            .text("Glow Colors");

    public static final MessageLocale COMMAND_GLOW_SET_DONE = LangEntry.builder("Command.Glow.Set.Done")
            .chatMessage(
                    GRAY.wrap("Your glow color is now " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_GLOW_SET_TARGET = LangEntry.builder("Command.Glow.Set.Target")
            .chatMessage(
                    GRAY.wrap("Set " + SOFT_YELLOW.wrap(PLAYER_NAME) + "'s glow color to "
                            + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

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

    public static final MessageLocale COMMAND_GLOW_ON_DONE = LangEntry.builder("Command.Glow.On.Done")
            .chatMessage(
                    GRAY.wrap("Your glow is now " + SOFT_YELLOW.wrap("enabled") + "."));

    public static final MessageLocale COMMAND_GLOW_ON_TARGET = LangEntry.builder("Command.Glow.On.Target")
            .chatMessage(
                    GRAY.wrap("Enabled " + SOFT_YELLOW.wrap(PLAYER_NAME) + "'s glow."));

    public static final MessageLocale COMMAND_GLOW_ON_NOTIFY = LangEntry.builder("Command.Glow.On.Notify")
            .chatMessage(
                    GRAY.wrap("Your glow was enabled."));

    public static final MessageLocale COMMAND_GLOW_OFF_DONE = LangEntry.builder("Command.Glow.Off.Done")
            .chatMessage(
                    GRAY.wrap("Your glow is now " + SOFT_YELLOW.wrap("disabled") + "."));

    public static final MessageLocale COMMAND_GLOW_OFF_TARGET = LangEntry.builder("Command.Glow.Off.Target")
            .chatMessage(
                    GRAY.wrap("Disabled " + SOFT_YELLOW.wrap(PLAYER_NAME) + "'s glow."));

    public static final MessageLocale COMMAND_GLOW_OFF_NOTIFY = LangEntry.builder("Command.Glow.Off.Notify")
            .chatMessage(
                    GRAY.wrap("Your glow was disabled."));

    public static final MessageLocale COMMAND_GLOW_ERROR_NO_SELECTION = LangEntry
            .builder("Command.Glow.Error.NoSelection")
            .chatMessage(
                    SOFT_RED.wrap(
                            "You have no glow color selected. Pick one with " + SOFT_YELLOW.wrap("/glow color <color>")
                                    + " or in the " + SOFT_YELLOW.wrap("/glow") + " menu."));

    public static final MessageLocale COMMAND_GLOW_ERROR_BAD_COLOR = LangEntry.builder("Command.Glow.Error.BadColor")
            .chatMessage(
                    SOFT_RED.wrap("Unknown color: " + SOFT_YELLOW.wrap(GENERIC_NAME) + ". Use "
                            + SOFT_YELLOW.wrap("/glow list") + " to see all colors."));

    public static final MessageLocale COMMAND_GLOW_ERROR_BAD_DURATION = LangEntry
            .builder("Command.Glow.Error.BadDuration")
            .chatMessage(
                    SOFT_RED.wrap("Duration must be between " + SOFT_YELLOW.wrap("1") + " and "
                            + SOFT_YELLOW.wrap("1200") + " ticks (20 ticks = 1 second)."));

    public static final MessageLocale COMMAND_GLOW_ERROR_BAD_INDEX = LangEntry.builder("Command.Glow.Error.BadIndex")
            .chatMessage(
                    SOFT_RED.wrap("Unknown phase: " + SOFT_YELLOW.wrap(GENERIC_VALUE) + ". Use "
                            + SOFT_YELLOW.wrap("/glow phase list") + " to see indices."));

    public static final MessageLocale COMMAND_GLOW_ERROR_NO_PHASES = LangEntry.builder("Command.Glow.Error.NoPhases")
            .chatMessage(
                    SOFT_RED.wrap("You have no phased glow yet. Use "
                            + SOFT_YELLOW.wrap("/glow phase create <color> <duration> [...]") + " first."));

    public static final MessageLocale COMMAND_GLOW_ERROR_TOO_MANY_PHASES = LangEntry
            .builder("Command.Glow.Error.TooManyPhases")
            .chatMessage(
                    SOFT_RED.wrap("Too many phases. Maximum is " + SOFT_YELLOW.wrap(GENERIC_VALUE) + "."));

    public static final MessageLocale COMMAND_GLOW_ERROR_PHASE_USAGE = LangEntry
            .builder("Command.Glow.Error.PhaseUsage")
            .chatMessage(
                    SOFT_RED.wrap(
                            "Usage: " + SOFT_YELLOW.wrap("/glow phase <create|add|remove|duration|list|clear>") + "."));

    public static final MessageLocale COMMAND_GLOW_ERROR_CREATE_USAGE = LangEntry
            .builder("Command.Glow.Error.CreateUsage")
            .chatMessage(
                    SOFT_RED.wrap("Usage: "
                            + SOFT_YELLOW.wrap("/glow phase create <color1> <duration1> [<color2> <duration2> ...]")
                            + ". Example: " + SOFT_YELLOW.wrap("/glow phase create red 40 gold 20") + "."));

    public static final MessageLocale COMMAND_GLOW_PHASE_CREATED = LangEntry.builder("Command.Glow.Phase.Created")
            .chatMessage(
                    GRAY.wrap("Your phased glow is now " + SOFT_YELLOW.wrap(GENERIC_VALUE) + "."));

    public static final MessageLocale COMMAND_GLOW_PHASE_ADDED = LangEntry.builder("Command.Glow.Phase.Added")
            .chatMessage(
                    GRAY.wrap("Added phase " + SOFT_YELLOW.wrap(GENERIC_NAME) + " for "
                            + SOFT_YELLOW.wrap(GENERIC_VALUE) + " ticks."));

    public static final MessageLocale COMMAND_GLOW_PHASE_REMOVED = LangEntry.builder("Command.Glow.Phase.Removed")
            .chatMessage(
                    GRAY.wrap("Removed phase " + SOFT_YELLOW.wrap(GENERIC_VALUE) + "."));

    public static final MessageLocale COMMAND_GLOW_PHASE_DURATION = LangEntry.builder("Command.Glow.Phase.Duration")
            .chatMessage(
                    GRAY.wrap("Phase " + SOFT_YELLOW.wrap(GENERIC_NAME) + " duration is now "
                            + SOFT_YELLOW.wrap(GENERIC_VALUE) + " ticks."));

    public static final MessageLocale COMMAND_GLOW_PHASE_LIST_TITLE = LangEntry.builder("Command.Glow.Phase.List.Title")
            .chatMessage(
                    GRAY.wrap("Your glow phases:"));

    public static final MessageLocale COMMAND_GLOW_PHASE_LIST_ENTRY = LangEntry.builder("Command.Glow.Phase.List.Entry")
            .chatMessage(
                    SOFT_YELLOW.wrap(GENERIC_VALUE) + GRAY.wrap(". " + GENERIC_NAME));

    public static final MessageLocale COMMAND_GLOW_PRESET_SAVED = LangEntry.builder("Command.Glow.Preset.Saved")
            .chatMessage(
                    GRAY.wrap("Saved glow preset " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_GLOW_PRESET_LOADED = LangEntry.builder("Command.Glow.Preset.Loaded")
            .chatMessage(
                    GRAY.wrap("Loaded glow preset " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_GLOW_PRESET_DELETED = LangEntry.builder("Command.Glow.Preset.Deleted")
            .chatMessage(
                    GRAY.wrap("Deleted glow preset " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_GLOW_PRESET_LIST_TITLE = LangEntry
            .builder("Command.Glow.Preset.List.Title")
            .chatMessage(
                    GRAY.wrap("Your glow presets:"));

    public static final MessageLocale COMMAND_GLOW_PRESET_LIST_ENTRY = LangEntry
            .builder("Command.Glow.Preset.List.Entry")
            .chatMessage(
                    SOFT_YELLOW.wrap("●") + GRAY.wrap(" " + GENERIC_NAME + " (" + GENERIC_VALUE + ")"));

    public static final MessageLocale COMMAND_GLOW_PRESET_LIST_EMPTY = LangEntry
            .builder("Command.Glow.Preset.List.Empty")
            .chatMessage(
                    GRAY.wrap("You have no saved presets. Use " + SOFT_YELLOW.wrap("/glow preset save <name>") + "."));

    public static final MessageLocale COMMAND_GLOW_ERROR_PRESET_EXISTS = LangEntry
            .builder("Command.Glow.Error.PresetExists")
            .chatMessage(
                    SOFT_RED.wrap("Preset " + SOFT_YELLOW.wrap(GENERIC_NAME)
                            + " already exists. Delete it first or pick another name."));

    public static final MessageLocale COMMAND_GLOW_ERROR_PRESET_UNKNOWN = LangEntry
            .builder("Command.Glow.Error.PresetUnknown")
            .chatMessage(
                    SOFT_RED.wrap("Unknown preset: " + SOFT_YELLOW.wrap(GENERIC_NAME) + ". Use "
                            + SOFT_YELLOW.wrap("/glow preset list") + "."));

    public static final MessageLocale COMMAND_GLOW_ERROR_PRESET_NAME = LangEntry
            .builder("Command.Glow.Error.PresetName")
            .chatMessage(
                    SOFT_RED.wrap("Invalid preset name. Use " + SOFT_YELLOW.wrap("a-z, 0-9, _ or -") + ", up to "
                            + SOFT_YELLOW.wrap("24") + " characters."));

    public static final MessageLocale COMMAND_GLOW_ERROR_PRESET_LIMIT = LangEntry
            .builder("Command.Glow.Error.PresetLimit")
            .chatMessage(
                    SOFT_RED.wrap("Preset limit reached (" + SOFT_YELLOW.wrap(GENERIC_VALUE) + "). Delete one first."));

    public static final MessageLocale COMMAND_GLOW_ERROR_PRESET_USAGE = LangEntry
            .builder("Command.Glow.Error.PresetUsage")
            .chatMessage(
                    SOFT_RED.wrap("Usage: " + SOFT_YELLOW.wrap("/glow preset <save|load|delete|list> [name]") + "."));

    public static final MessageLocale COMMAND_GLOW_LIST_OPTIONS = LangEntry.builder("Command.Glow.List.Options")
            .chatMessage(
                    GRAY.wrap("Options: " + SOFT_YELLOW.wrap("/glow") + " menu, "
                            + SOFT_YELLOW.wrap("/glow color <color>") + ", "
                            + SOFT_YELLOW.wrap("/glow phase create <color> <duration> [...]") + ", "
                            + SOFT_YELLOW.wrap("/glow preset <save|load|delete|list>") + ", "
                            + SOFT_YELLOW.wrap("/glow on") + ", " + SOFT_YELLOW.wrap("/glow off") + "."));

    public static final MessageLocale COMMAND_GLOW_MENU_OPENED = LangEntry.builder("Command.Glow.Menu.Opened")
            .chatMessage(
                    GRAY.wrap("Opening glow menu..."));

    public static final MessageLocale COMMAND_GLOW_MENU_SELECTED = LangEntry.builder("Command.Glow.Menu.Selected")
            .chatMessage(
                    GRAY.wrap("Your glow color is now " + SOFT_YELLOW.wrap(GENERIC_NAME) + "."));

    public static final MessageLocale COMMAND_GLOW_ERROR_MENU = LangEntry.builder("Command.Glow.Error.Menu")
            .chatMessage(
                    SOFT_RED.wrap("Glow menu is unavailable."));
}
