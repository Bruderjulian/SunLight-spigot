package su.nightexpress.sunlight.moduleImpl.nametags.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.BooleanLocale;
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

    public static final TextLocale COMMAND_PROFILE_DESC = LangEntry.builder("Command.Nametags.Profile.Desc")
        .text("Select a nametag profile.");

    public static final TextLocale COMMAND_RANK_DESC = LangEntry.builder("Command.Nametags.Rank.Desc")
        .text("Enable or disable the rank part of your nametag.");

    public static final TextLocale COMMAND_TAG_TOGGLE_DESC = LangEntry.builder("Command.Nametags.TagToggle.Desc")
        .text("Enable or disable the selected tag on your nametag.");

    public static final TextLocale COMMAND_SET_DESC = LangEntry.builder("Command.Nametags.Set.Desc")
        .text("Set or clear a player's tag.");

    public static final TextLocale COMMAND_GRANT_DESC = LangEntry.builder("Command.Nametags.Grant.Desc")
        .text("Grant or revoke a tag for a player.");

    public static final TextLocale COMMAND_ADMIN_DESC = LangEntry.builder("Command.Nametags.Admin.Desc")
        .text("Open the nametag tag editor.");

    public static final TextLocale COMMAND_RELOAD_DESC = LangEntry.builder("Command.Nametags.Reload.Desc")
        .text("Reload the nametags module.");

    public static final MessageLocale NAMETAG_RANK_CHANGED = LangEntry.builder("Command.Nametags.Rank.Changed")
        .chatMessage(GREEN.wrap("Rank part of the nametag is now ") + GENERIC_STATE + GREEN.wrap("."));

    public static final MessageLocale NAMETAG_TAG_CHANGED = LangEntry.builder("Command.Nametags.TagToggle.Changed")
        .chatMessage(GREEN.wrap("Selected tag is now ") + GENERIC_STATE + GREEN.wrap("."));

    public static final MessageLocale COMMAND_TAG_SELECTED = LangEntry.builder("Command.Nametags.Tag.Selected")
        .chatMessage(GREEN.wrap("Tag ") + GENERIC_NAME + GREEN.wrap(" has been selected."));

    public static final MessageLocale COMMAND_TAG_CLEARED = LangEntry.builder("Command.Nametags.Tag.Cleared")
        .chatMessage(GREEN.wrap("Tag has been removed."));

    public static final MessageLocale COMMAND_PROFILE_SELECTED = LangEntry.builder("Command.Nametags.Profile.Selected")
        .chatMessage(GREEN.wrap("Profile ") + GENERIC_NAME + GREEN.wrap(" has been selected."));

    public static final MessageLocale COMMAND_TAG_SET_NOTIFY = LangEntry.builder("Command.Nametags.Tag.SetNotify")
        .chatMessage(GREEN.wrap("Your tag was set to ") + GENERIC_NAME + GREEN.wrap("."));

    public static final MessageLocale COMMAND_TAG_CLEAR_NOTIFY = LangEntry.builder("Command.Nametags.Tag.ClearNotify")
        .chatMessage(GREEN.wrap("Your tag was removed."));

    public static final MessageLocale COMMAND_SET_DONE = LangEntry.builder("Command.Nametags.Set.Done")
        .chatMessage(GREEN.wrap("Tag for ") + PLAYER_NAME + GREEN.wrap(" is now ") + GENERIC_NAME + GREEN.wrap("."));

    public static final MessageLocale COMMAND_SET_CLEAR = LangEntry.builder("Command.Nametags.Set.Clear")
        .chatMessage(GREEN.wrap("Tag for ") + PLAYER_NAME + GREEN.wrap(" was removed."));

    public static final MessageLocale COMMAND_GRANT_DONE = LangEntry.builder("Command.Nametags.Grant.Done")
        .chatMessage(GREEN.wrap("Granted ") + GENERIC_NAME + GREEN.wrap(" to ") + PLAYER_NAME + GREEN.wrap("."));

    public static final MessageLocale COMMAND_GRANT_REVOKED = LangEntry.builder("Command.Nametags.Grant.Revoked")
        .chatMessage(GREEN.wrap("Revoked ") + GENERIC_NAME + GREEN.wrap(" from ") + PLAYER_NAME + GREEN.wrap("."));

    public static final MessageLocale COMMAND_TAG_ERROR_INVALID = LangEntry.builder("Command.Nametags.Tag.Error.Invalid")
        .chatMessage(SOFT_RED.wrap("Tag ") + GENERIC_NAME + SOFT_RED.wrap(" not found."));

    public static final MessageLocale COMMAND_TAG_ERROR_NO_ACCESS = LangEntry
        .builder("Command.Nametags.Tag.Error.NoAccess")
        .chatMessage(SOFT_RED.wrap("You don't have access to the ") + GENERIC_NAME + SOFT_RED.wrap(" tag."));

    public static final MessageLocale COMMAND_TAG_ERROR_CANCELLED = LangEntry
        .builder("Command.Nametags.Tag.Error.Cancelled")
        .chatMessage(SOFT_RED.wrap("Another plugin cancelled the tag change."));

    public static final MessageLocale COMMAND_TAG_ERROR_NO_EQUITY = LangEntry
        .builder("Command.Nametags.Tag.Error.NoEquity")
        .chatMessage(SOFT_RED.wrap("You can't afford the ") + GENERIC_NAME + SOFT_RED.wrap(" tag (")
            + GENERIC_AMOUNT + SOFT_RED.wrap(")."));

    public static final MessageLocale COMMAND_TAG_ERROR_EXTERNAL = LangEntry
        .builder("Command.Nametags.Tag.Error.External")
        .chatMessage(SOFT_RED.wrap("This tag is handled by another plugin and cannot be bought here."));

    public static final MessageLocale COMMAND_TAG_ERROR_ECONOMY = LangEntry
        .builder("Command.Nametags.Tag.Error.Economy")
        .chatMessage(SOFT_RED.wrap("No economy plugin is available to process this purchase."));

    public static final MessageLocale COMMAND_TAG_BOUGHT = LangEntry.builder("Command.Nametags.Tag.Bought")
        .chatMessage(GREEN.wrap("You purchased ") + GENERIC_NAME + GREEN.wrap("."));

    public static final MessageLocale COMMAND_TAG_SUBSCRIBED = LangEntry.builder("Command.Nametags.Tag.Subscribed")
        .chatMessage(GREEN.wrap("Your subscription for ") + GENERIC_NAME + GREEN.wrap(" is now active."));

    public static final MessageLocale COMMAND_TAG_RENEWED = LangEntry.builder("Command.Nametags.Tag.Renewed")
        .chatMessage(GREEN.wrap("Your subscription for ") + GENERIC_NAME + GREEN.wrap(" was extended."));

    public static final MessageLocale COMMAND_TAG_EXPIRED = LangEntry.builder("Command.Nametags.Tag.Expired")
        .chatMessage(SOFT_RED.wrap("Your subscription for ") + GENERIC_NAME + SOFT_RED.wrap(" has expired."));

    public static final TextLocale COMMAND_TAG_OWNED = LangEntry.builder("Command.Nametags.Tag.Owned")
        .text("Owned");

    public static final TextLocale COMMAND_TAG_PRICE = LangEntry.builder("Command.Nametags.Tag.Price")
        .text("Price");

    public static final TextLocale COMMAND_TAG_PERIOD = LangEntry.builder("Command.Nametags.Tag.Period")
        .text("Period");

    public static final TextLocale COMMAND_TAG_RANK = LangEntry.builder("Command.Nametags.Tag.Rank")
        .text("Rank");

    public static final BooleanLocale COMMAND_TAG_ENABLED = LangEntry.builder("Command.Nametags.Tag.Enabled")
        .bool(GREEN.wrap("Enabled"), SOFT_RED.wrap("Disabled"));

    public static final BooleanLocale COMMAND_TAG_ACCESS_FREE = LangEntry.builder("Command.Nametags.Tag.Access.Free")
        .bool(GREEN.wrap("Free"), SOFT_RED.wrap("Locked"));

    public static final BooleanLocale COMMAND_TAG_ACCESS_PERMISSION = LangEntry
        .builder("Command.Nametags.Tag.Access.Permission")
        .bool(YELLOW.wrap("Permission"), SOFT_RED.wrap("Locked"));

    public static final BooleanLocale COMMAND_TAG_ACCESS_PURCHASE = LangEntry
        .builder("Command.Nametags.Tag.Access.Purchase")
        .bool(GOLD.wrap("Purchase"), SOFT_RED.wrap("Locked"));

    public static final BooleanLocale COMMAND_TAG_ACCESS_SUBSCRIPTION = LangEntry
        .builder("Command.Nametags.Tag.Access.Subscription")
        .bool(LIGHT_PURPLE.wrap("Subscription"), SOFT_RED.wrap("Locked"));

    public static final BooleanLocale COMMAND_TAG_ACCESS_OWNED = LangEntry.builder("Command.Nametags.Tag.Access.Owned")
        .bool(GREEN.wrap("Owned"), SOFT_RED.wrap("Not owned"));

    public static final MessageLocale COMMAND_PROFILE_ERROR_INVALID = LangEntry
        .builder("Command.Nametags.Profile.Error.Invalid")
        .chatMessage(SOFT_RED.wrap("Profile ") + GENERIC_NAME + SOFT_RED.wrap(" not found."));

    public static final MessageLocale COMMAND_RELOAD_DONE = LangEntry.builder("Command.Nametags.Reload.Done")
        .chatMessage(GREEN.wrap("Nametags module reloaded."));

    public static final MessageLocale ERROR_MENU_UNAVAILABLE = LangEntry.builder("Nametags.Error.MenuUnavailable")
        .chatMessage(SOFT_RED.wrap("Tags menu is not available."));
}
