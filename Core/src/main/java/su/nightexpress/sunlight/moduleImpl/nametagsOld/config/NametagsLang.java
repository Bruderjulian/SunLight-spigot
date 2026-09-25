package su.nightexpress.sunlight.moduleImpl.nametagsOld.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class NametagsLang implements LangContainer {

    public static final TextLocale COMMAND_NAMETAG_DESC = LangEntry.builder("Command.Nametags.Root.Desc")
        .text("Manage your nametag.");

    public static final TextLocale COMMAND_TAGS_DESC = LangEntry.builder("Command.Nametags.Tags.Desc")
        .text("Open the tag selection menu.");

    public static final TextLocale COMMAND_TAG_DESC = LangEntry.builder("Command.Nametags.Tag.Desc")
        .text("Select a tag.");

    public static final TextLocale COMMAND_RANK_DESC = LangEntry.builder("Command.Nametags.Rank.Desc")
        .text("Enable or disable the rank part of your nametag.");

    public static final TextLocale COMMAND_TAG_TOGGLE_DESC = LangEntry.builder("Command.Nametags.TagToggle.Desc")
        .text("Enable or disable the selected tag on your nametag.");

    public static final TextLocale COMMAND_SET_DESC = LangEntry.builder("Command.Nametags.Set.Desc")
        .text("Set or clear a player's tag.");

    public static final TextLocale COMMAND_RELOAD_DESC = LangEntry.builder("Command.Nametags.Reload.Desc")
        .text("Reload the nametags module.");

    public static final MessageLocale NAMETAG_RANK_CHANGED = LangEntry.builder("Command.Nametags.Rank.Changed")
        .chatMessage(
            GREEN.wrap("Rank part of the nametag is now ") + GENERIC_STATE + GREEN.wrap("."));

    public static final MessageLocale NAMETAG_TAG_CHANGED = LangEntry.builder("Command.Nametags.TagToggle.Changed")
        .chatMessage(
            GREEN.wrap("Selected tag is now ") + GENERIC_STATE + GREEN.wrap("."));

    public static final MessageLocale COMMAND_TAG_SELECTED = LangEntry.builder("Command.Nametags.Tag.Selected")
        .chatMessage(
            GREEN.wrap("Tag ") + GENERIC_NAME + GREEN.wrap(" has been selected."));

    public static final MessageLocale COMMAND_TAG_CLEARED = LangEntry.builder("Command.Nametags.Tag.Cleared")
        .chatMessage(
            GREEN.wrap("Tag has been removed."));

    public static final MessageLocale COMMAND_TAG_SET_NOTIFY = LangEntry.builder("Command.Nametags.Tag.SetNotify")
        .chatMessage(
            GREEN.wrap("Your tag was set to ") + GENERIC_NAME + GREEN.wrap("."));

    public static final MessageLocale COMMAND_TAG_CLEAR_NOTIFY = LangEntry.builder("Command.Nametags.Tag.ClearNotify")
        .chatMessage(
            GREEN.wrap("Your tag was removed."));

    public static final MessageLocale COMMAND_SET_DONE = LangEntry.builder("Command.Nametags.Set.Done")
        .chatMessage(
            GREEN.wrap("Tag for ") + PLAYER_NAME + GREEN.wrap(" is now ") + GENERIC_NAME + GREEN.wrap("."));

    public static final MessageLocale COMMAND_SET_CLEAR = LangEntry.builder("Command.Nametags.Set.Clear")
        .chatMessage(
            GREEN.wrap("Tag for ") + PLAYER_NAME + GREEN.wrap(" was removed."));

    public static final MessageLocale COMMAND_TAG_ERROR_INVALID = LangEntry.builder("Command.Nametags.Tag.Error.Invalid")
        .chatMessage(
            SOFT_RED.wrap("Tag ") + GENERIC_NAME + SOFT_RED.wrap(" not found."));

    public static final MessageLocale COMMAND_TAG_ERROR_NO_ACCESS = LangEntry
        .builder("Command.Nametags.Tag.Error.NoAccess")
        .chatMessage(
            SOFT_RED.wrap("You don't have access to the ") + GENERIC_NAME + SOFT_RED.wrap(" tag."));

    public static final MessageLocale COMMAND_TAG_ERROR_NO_EQUITY = LangEntry
        .builder("Command.Nametags.Tag.Error.NoEquity")
        .chatMessage(
            SOFT_RED.wrap("You can't afford the ") + GENERIC_NAME + SOFT_RED.wrap(" tag (") + GENERIC_AMOUNT
                + SOFT_RED.wrap(")."));

    public static final MessageLocale COMMAND_RELOAD_DONE = LangEntry.builder("Command.Nametags.Reload.Done")
        .chatMessage(
            GREEN.wrap("Nametags module reloaded."));

    public static final MessageLocale ERROR_MENU_UNAVAILABLE = LangEntry.builder("Nametags.Error.MenuUnavailable")
        .chatMessage(
            SOFT_RED.wrap("Tags menu is not available."));
}