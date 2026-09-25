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

    public static final TextLocale COMMAND_PROFILES_DESC = LangEntry.builder("Command.Nametags.Profiles.Desc")
        .text("Open the profile selection menu.");

    public static final TextLocale COMMAND_RANK_DESC = LangEntry.builder("Command.Nametags.Rank.Desc")
        .text("Enable or disable the rank part of your nametag.");

    public static final TextLocale COMMAND_TAG_TOGGLE_DESC = LangEntry.builder("Command.Nametags.TagToggle.Desc")
        .text("Enable or disable the selected tag on your nametag.");

    public static final TextLocale COMMAND_TEAM_TOGGLE_DESC = LangEntry.builder("Command.Nametags.TeamToggle.Desc")
        .text("Enable or disable your team prefix on your nametag.");

    public static final TextLocale COMMAND_HIDE_DESC = LangEntry.builder("Command.Nametags.Hide.Desc")
        .text("Show only your name, with no prefix, suffix or colour.");

    public static final TextLocale COMMAND_SHOW_DESC = LangEntry.builder("Command.Nametags.Show.Desc")
        .text("Restore your full nametag.");

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

    public static final MessageLocale NAMETAG_TEAM_CHANGED = LangEntry.builder("Command.Nametags.TeamToggle.Changed")
        .chatMessage(GREEN.wrap("Team prefix is now ") + GENERIC_STATE + GREEN.wrap("."));

    public static final MessageLocale NAMETAG_HIDE_CHANGED = LangEntry.builder("Command.Nametags.Hide.Changed")
        .chatMessage(GREEN.wrap("Nametag hidden, only your name is shown."));

    public static final MessageLocale NAMETAG_SHOW_DONE = LangEntry.builder("Command.Nametags.Show.Done")
        .chatMessage(GREEN.wrap("Your full nametag is back."));

    public static final MessageLocale COMMAND_TAG_SELECTED = LangEntry.builder("Command.Nametags.Tag.Selected")
        .chatMessage(GREEN.wrap("Tag ") + GENERIC_NAME + GREEN.wrap(" has been selected."));

    public static final MessageLocale COMMAND_TAG_CLEARED = LangEntry.builder("Command.Nametags.Tag.Cleared")
        .chatMessage(GREEN.wrap("Tag has been removed."));

    public static final MessageLocale COMMAND_PROFILE_SELECTED = LangEntry.builder("Command.Nametags.Profile.Selected")
        .chatMessage(GREEN.wrap("Profile ") + GENERIC_NAME + GREEN.wrap(" has been selected."));

    public static final MessageLocale COMMAND_PROFILE_CLEARED = LangEntry.builder("Command.Nametags.Profile.Cleared")
        .chatMessage(GREEN.wrap("Profile has been removed."));

    public static final MessageLocale COMMAND_PROFILE_ERROR_LOCKED = LangEntry
        .builder("Command.Nametags.Profile.Error.Locked")
        .chatMessage(SOFT_RED.wrap("The ") + GENERIC_NAME + SOFT_RED.wrap(" profile is not available to you."));

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

    // -----------------------------------------------------
    // Menu titles
    // -----------------------------------------------------

    public static final TextLocale MENU_TAGS_TITLE = LangEntry.builder("Nametags.Menu.Tags.Title")
        .text(DARK_GRAY.wrap("Tags"));

    public static final TextLocale MENU_ADMIN_TITLE = LangEntry.builder("Nametags.Menu.Admin.Title")
        .text(DARK_PURPLE.wrap("Nametags Admin"));

    public static final TextLocale MENU_ADMIN_TAGS_TITLE = LangEntry.builder("Nametags.Menu.AdminTags.Title")
        .text(DARK_PURPLE.wrap("Nametags Admin"));

    public static final TextLocale MENU_ADMIN_RANKS_TITLE = LangEntry.builder("Nametags.Menu.AdminRanks.Title")
        .text(DARK_PURPLE.wrap("Nametag Ranks"));

    public static final TextLocale MENU_ADMIN_PROFILES_TITLE = LangEntry.builder("Nametags.Menu.AdminProfiles.Title")
        .text(DARK_PURPLE.wrap("Nametag Profiles"));

    public static final TextLocale MENU_PROFILES_TITLE = LangEntry.builder("Nametags.Menu.Profiles.Title")
        .text(DARK_GRAY.wrap("Nametag Profiles"));

    public static final TextLocale MENU_ADMIN_TAG_TITLE = LangEntry.builder("Nametags.Menu.AdminTag.Title")
        .text(DARK_PURPLE.wrap("Edit Tag"));

    public static final TextLocale MENU_ADMIN_RANK_TITLE = LangEntry.builder("Nametags.Menu.AdminRank.Title")
        .text(DARK_PURPLE.wrap("Edit Rank"));

    public static final TextLocale MENU_ADMIN_PROFILE_TITLE = LangEntry.builder("Nametags.Menu.AdminProfile.Title")
        .text(DARK_PURPLE.wrap("Edit Profile"));

    // -----------------------------------------------------
    // Shared menu fragments
    // -----------------------------------------------------

    public static final TextLocale MENU_CURRENT = LangEntry.builder("Nametags.Menu.Current").text("Current");
    public static final TextLocale MENU_VALUE_NONE = LangEntry.builder("Nametags.Menu.ValueNone").text("none");
    public static final TextLocale MENU_VALUE_DOUBLE = LangEntry.builder("Nametags.Menu.ValueDouble").text("double");
    public static final TextLocale MENU_CLICK_EDIT = LangEntry.builder("Nametags.Menu.ClickEdit")
        .text("Click to edit");
    public static final TextLocale MENU_CLICK_CYCLE = LangEntry.builder("Nametags.Menu.ClickCycle")
        .text("Click to cycle");
    public static final TextLocale MENU_CLICK_TOGGLE = LangEntry.builder("Nametags.Menu.ClickToggle")
        .text("Click to toggle");
    public static final TextLocale MENU_CLICK_LEFT = LangEntry.builder("Nametags.Menu.ClickLeft").text("Left click");
    public static final TextLocale MENU_CLICK_RIGHT = LangEntry.builder("Nametags.Menu.ClickRight").text("Right click");
    public static final TextLocale MENU_CLICK_SHIFT_RIGHT = LangEntry.builder("Nametags.Menu.ClickShiftRight")
        .text("Shift + right");
    public static final TextLocale MENU_CLOSE = LangEntry.builder("Nametags.Menu.Close").text("Close");
    public static final TextLocale MENU_CLICK_CLOSE = LangEntry.builder("Nametags.Menu.ClickClose")
        .text("Click to close.");
    public static final TextLocale MENU_RIGHT_CLICK_DELETE = LangEntry.builder("Nametags.Menu.RightClickDelete")
        .text("Right click to delete");
    public static final TextLocale MENU_SELECTED = LangEntry.builder("Nametags.Menu.Selected").text("✔ Selected");
    public static final TextLocale MENU_CLICK_SELECT = LangEntry.builder("Nametags.Menu.ClickSelect")
        .text("Click to select");
    public static final TextLocale MENU_CLICK_BUY = LangEntry.builder("Nametags.Menu.ClickBuy")
        .text("Click to purchase");
    public static final TextLocale MENU_CLICK_SUBSCRIBE = LangEntry.builder("Nametags.Menu.ClickSubscribe")
        .text("Click to subscribe");
    public static final TextLocale MENU_NO_ACCESS = LangEntry.builder("Nametags.Menu.NoAccess")
        .text("You do not have access.");
    public static final TextLocale MENU_MANAGED_ELSEWHERE = LangEntry.builder("Nametags.Menu.ManagedElsewhere")
        .text("Managed by another plugin.");
    public static final TextLocale MENU_HINT_COMMA_SEPARATED = LangEntry.builder("Nametags.Menu.HintCommaSeparated")
        .text("Comma separated, e.g. 'a, b, c'.");
    public static final TextLocale MENU_RENEWS = LangEntry.builder("Nametags.Menu.Renews").text("Renews");
    public static final TextLocale MENU_EXPIRES_NEVER = LangEntry.builder("Nametags.Menu.ExpiresNever").text("never");
    public static final TextLocale MENU_LOCKED = LangEntry.builder("Nametags.Menu.Locked").text("Locked");
    public static final TextLocale MENU_FIELD_ACCESS = LangEntry.builder("Nametags.Menu.Field.Access").text("Access");
    public static final TextLocale MENU_CLICK_RENEW = LangEntry.builder("Nametags.Menu.ClickRenew")
        .text("Click to renew");
    public static final TextLocale MENU_GET_FROM_SHOP = LangEntry.builder("Nametags.Menu.GetFromShop")
        .text("Get it from the shop.");

    // -----------------------------------------------------
    // Tag editor fields
    // -----------------------------------------------------

    public static final TextLocale MENU_FIELD_DISPLAY = LangEntry.builder("Nametags.Menu.Field.Display")
        .text("Display name");
    public static final TextLocale MENU_FIELD_ID = LangEntry.builder("Nametags.Menu.Field.Id").text("Id");
    public static final TextLocale MENU_FIELD_DESCRIPTION = LangEntry.builder("Nametags.Menu.Field.Description")
        .text("Description");
    public static final TextLocale MENU_FIELD_PREFIX = LangEntry.builder("Nametags.Menu.Field.Prefix")
        .text("Prefix");
    public static final TextLocale MENU_FIELD_SUFFIX = LangEntry.builder("Nametags.Menu.Field.Suffix")
        .text("Suffix");
    public static final TextLocale MENU_FIELD_COLOR = LangEntry.builder("Nametags.Menu.Field.Color")
        .text("Name colour");
    public static final TextLocale MENU_FIELD_ICON = LangEntry.builder("Nametags.Menu.Field.Icon")
        .text("GUI icon material");
    public static final TextLocale MENU_FIELD_PERMISSION = LangEntry.builder("Nametags.Menu.Field.Permission")
        .text("Extra permission");
    public static final TextLocale MENU_FIELD_ACCESS_MODE = LangEntry.builder("Nametags.Menu.Field.AccessMode")
        .text("Access mode");
    public static final TextLocale MENU_FIELD_PRICE = LangEntry.builder("Nametags.Menu.Field.Price").text("Price");
    public static final TextLocale MENU_FIELD_PERIOD = LangEntry.builder("Nametags.Menu.Field.Period")
        .text("Billing period");
    public static final TextLocale MENU_FIELD_PRICE_MODE = LangEntry.builder("Nametags.Menu.Field.PriceMode")
        .text("Price mode");
    public static final TextLocale MENU_FIELD_GLOW = LangEntry.builder("Nametags.Menu.Field.Glow")
        .text("Glow colour");
    public static final TextLocale MENU_FIELD_GLOW_HINT = LangEntry.builder("Nametags.Menu.Field.GlowHint")
        .text("Allow the glow module to colour this tag:");
    public static final TextLocale MENU_FIELD_SORT = LangEntry.builder("Nametags.Menu.Field.Sort")
        .text("Sort priority");
    public static final TextLocale MENU_SORT_HINT = LangEntry.builder("Nametags.Menu.SortHint")
        .text("Higher sorts first in the player menu");
    public static final TextLocale MENU_SUBSCRIPTIONS_ONLY = LangEntry.builder("Nametags.Menu.SubscriptionsOnly")
        .text("Only used by subscriptions");
    public static final TextLocale MENU_PRICE_MODE_INTERNAL = LangEntry.builder("Nametags.Menu.PriceMode.Internal")
        .text("INTERNAL: SunLight handles the payment");
    public static final TextLocale MENU_PRICE_MODE_EXTERNAL = LangEntry.builder("Nametags.Menu.PriceMode.External")
        .text("EXTERNAL: another plugin owns the grant");

    // -----------------------------------------------------
    // Rank editor fields
    // -----------------------------------------------------

    public static final TextLocale MENU_FIELD_GROUPS = LangEntry.builder("Nametags.Menu.Field.Groups")
        .text("Matching groups");
    public static final TextLocale MENU_FIELD_DEFAULT = LangEntry.builder("Nametags.Menu.Field.Default")
        .text("Default rank");
    public static final TextLocale MENU_DEFAULT_HINT = LangEntry.builder("Nametags.Menu.DefaultHint")
        .text("Used only when the player matches no other rank");
    public static final TextLocale MENU_GROUPS_HINT = LangEntry.builder("Nametags.Menu.GroupsHint")
        .text("Group names, or '*' for everyone");

    // -----------------------------------------------------
    // Profile editor fields
    // -----------------------------------------------------

    public static final TextLocale MENU_FIELD_WORLDS = LangEntry.builder("Nametags.Menu.Field.Worlds")
        .text("Worlds");
    public static final TextLocale MENU_FIELD_WORLD_LABEL = LangEntry.builder("Nametags.Menu.Field.WorldLabel")
        .text("Applies to");
    public static final TextLocale MENU_FIELD_PRIORITY = LangEntry.builder("Nametags.Menu.Field.Priority")
        .text("Priority");
    public static final TextLocale MENU_FIELD_RANK = LangEntry.builder("Nametags.Menu.Field.Rank").text("Forced rank");
    public static final TextLocale MENU_FIELD_TAG = LangEntry.builder("Nametags.Menu.Field.Tag").text("Forced tag");
    public static final TextLocale MENU_FIELD_INHERIT = LangEntry.builder("Nametags.Menu.Field.Inherit")
        .text("inherit");
    public static final TextLocale MENU_WORLDS_HINT = LangEntry.builder("Nametags.Menu.WorldsHint")
        .text("Empty means every world");
    public static final TextLocale MENU_PRIORITY_HINT = LangEntry.builder("Nametags.Menu.PriorityHint")
        .text("Higher wins when several profiles match");

    // -----------------------------------------------------
    // Admin hub
    // -----------------------------------------------------

    public static final TextLocale MENU_HUB_TAGS = LangEntry.builder("Nametags.Menu.Hub.Tags").text("Tags");
    public static final TextLocale MENU_HUB_TAGS_HINT = LangEntry.builder("Nametags.Menu.Hub.TagsHint")
        .text("Edit selectable tags");
    public static final TextLocale MENU_HUB_RANKS = LangEntry.builder("Nametags.Menu.Hub.Ranks").text("Ranks");
    public static final TextLocale MENU_HUB_RANKS_HINT = LangEntry.builder("Nametags.Menu.Hub.RanksHint")
        .text("Edit group based ranks");
    public static final TextLocale MENU_HUB_PROFILES = LangEntry.builder("Nametags.Menu.Hub.Profiles").text("Profiles");
    public static final TextLocale MENU_HUB_PROFILES_HINT = LangEntry.builder("Nametags.Menu.Hub.ProfilesHint")
        .text("Edit named bundles of defaults");
    public static final TextLocale MENU_CREATE_TAG = LangEntry.builder("Nametags.Menu.CreateTag").text("Create tag");
    public static final TextLocale MENU_CREATE_TAG_HINT = LangEntry.builder("Nametags.Menu.CreateTagHint")
        .text("Click to add a new tag.");
    public static final TextLocale MENU_CREATE_RANK = LangEntry.builder("Nametags.Menu.CreateRank").text("Create rank");
    public static final TextLocale MENU_CREATE_RANK_HINT = LangEntry.builder("Nametags.Menu.CreateRankHint")
        .text("Click to add a new rank.");
    public static final TextLocale MENU_CREATE_PROFILE = LangEntry.builder("Nametags.Menu.CreateProfile")
        .text("Create profile");
    public static final TextLocale MENU_CREATE_PROFILE_HINT = LangEntry.builder("Nametags.Menu.CreateProfileHint")
        .text("Click to add a new profile.");
    public static final TextLocale MENU_DELETE_REFERENCED = LangEntry.builder("Nametags.Menu.DeleteReferenced")
        .text("Profiles pointing here fall back to normal resolution.");
}
