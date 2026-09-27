package su.nightexpress.sunlight.moduleImpl.links.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.IconLocale;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.moduleImpl.links.LinksPlaceholders;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_AMOUNT;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_COOLDOWN;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_INPUT;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_NAME;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_NEW_VALUE;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_TIME;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_TOTAL;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_VALUE;

public class LinksLang implements LangContainer {

    // Commands

    public static final TextLocale COMMAND_LINKS_ROOT_DESC = LangEntry.builder("Command.Links.Root.Desc")
            .text("Show all server links.");

    public static final TextLocale COMMAND_LINKS_ALL_DESC = LangEntry.builder("Command.Links.All.Desc")
            .text("Show all server links.");

    public static final TextLocale COMMAND_LINKS_LINK_DESC = LangEntry.builder("Command.Links.Link.Desc")
            .text("Open a server link.");

    public static final TextLocale COMMAND_LINKS_GET_DESC = LangEntry.builder("Command.Links.Get.Desc")
            .text("Open a server link by ID.");

    public static final TextLocale COMMAND_LINKS_STATS_DESC = LangEntry.builder("Command.Links.Stats.Desc")
            .text("Show link click statistics.");

    public static final TextLocale COMMAND_ADMIN_ROOT_DESC = LangEntry.builder("Command.Links.Admin.Root.Desc")
            .text("Edit server links.");

    public static final TextLocale COMMAND_ADMIN_CREATE_DESC = LangEntry.builder("Command.Links.Admin.Create.Desc")
            .text("Create a new server link.");

    public static final TextLocale COMMAND_ADMIN_DELETE_DESC = LangEntry.builder("Command.Links.Admin.Delete.Desc")
            .text("Delete a server link.");

    public static final TextLocale COMMAND_ADMIN_EDIT_DESC = LangEntry.builder("Command.Links.Admin.Edit.Desc")
            .text("Open the link editor.");

    public static final TextLocale COMMAND_ADMIN_SET_URL_DESC = LangEntry.builder("Command.Links.Admin.SetUrl.Desc")
            .text("Set the URL of a link.");

    public static final TextLocale COMMAND_ADMIN_SET_NAME_DESC = LangEntry.builder("Command.Links.Admin.SetName.Desc")
            .text("Set the display name of a link.");

    public static final TextLocale COMMAND_ADMIN_SET_COMMAND_DESC = LangEntry
            .builder("Command.Links.Admin.SetCommand.Desc")
            .text("Set the command executed when a link is opened.");

    public static final TextLocale COMMAND_ADMIN_SET_EXECUTOR_DESC = LangEntry
            .builder("Command.Links.Admin.SetExecutor.Desc")
            .text("Set who executes the command of a link.");

    public static final TextLocale COMMAND_ADMIN_SET_PERMISSION_DESC = LangEntry
            .builder("Command.Links.Admin.SetPermission.Desc")
            .text("Set the permission required to see a link.");

    public static final TextLocale COMMAND_ADMIN_SET_USE_PERMISSION_DESC = LangEntry
            .builder("Command.Links.Admin.SetUsePermission.Desc")
            .text("Set the permission required to use a link.");

    public static final TextLocale COMMAND_ADMIN_SET_COOLDOWN_DESC = LangEntry
            .builder("Command.Links.Admin.SetCooldown.Desc")
            .text("Set the per-player cooldown of a link in seconds.");

    public static final TextLocale COMMAND_ADMIN_SET_COST_DESC = LangEntry
            .builder("Command.Links.Admin.SetCost.Desc")
            .text("Set the Vault cost of a link.");

    public static final TextLocale COMMAND_ADMIN_SET_REWARD_DESC = LangEntry
            .builder("Command.Links.Admin.SetReward.Desc")
            .text("Set the first-click reward command of a link.");

    public static final TextLocale COMMAND_ADMIN_SET_SOUND_DESC = LangEntry
            .builder("Command.Links.Admin.SetSound.Desc")
            .text("Set the click sound of a link.");

    public static final TextLocale COMMAND_ADMIN_SET_ACTIONBAR_DESC = LangEntry
            .builder("Command.Links.Admin.SetActionbar.Desc")
            .text("Set the actionbar message of a link.");

    public static final TextLocale COMMAND_ADMIN_SET_PARTICLE_DESC = LangEntry
            .builder("Command.Links.Admin.SetParticle.Desc")
            .text("Set the click particle of a link.");

    public static final TextLocale COMMAND_ADMIN_SET_ICON_DESC = LangEntry.builder("Command.Links.Admin.SetIcon.Desc")
            .text("Set the icon of a link.");

    public static final TextLocale COMMAND_ADMIN_SET_PRIORITY_DESC = LangEntry
            .builder("Command.Links.Admin.SetPriority.Desc")
            .text("Set the priority of a link.");

    public static final TextLocale COMMAND_ADMIN_TOGGLE_DESC = LangEntry.builder("Command.Links.Admin.Toggle.Desc")
            .text("Enable or disable a link.");

    public static final TextLocale COMMAND_ADMIN_RESET_CLICKS_DESC = LangEntry
            .builder("Command.Links.Admin.ResetClicks.Desc")
            .text("Reset the click counter of a link.");

    public static final MessageLocale COMMAND_SYNTAX_INVALID_LINK = LangEntry.builder("Command.Links.Syntax.InvalidLink")
            .chatMessage(GRAY.wrap(RED.wrap(GENERIC_INPUT) + " is not a valid link!"));

    public static final TextLocale COMMAND_ARGUMENT_LINK = LangEntry.builder("Command.Links.Argument.Link")
            .text("link");

    // Player-facing feedback

    public static final MessageLocale LIST_HEADER = LangEntry.builder("Links.List.Header")
            .chatMessage(DARK_GRAY.wrap("[") + GOLD.wrap("Links") + DARK_GRAY.wrap("]"));

    public static final MessageLocale LIST_EMPTY = LangEntry.builder("Links.List.Empty")
            .chatMessage(SOFT_RED.wrap("No links available."));

    public static final MessageLocale LINK_LINE = LangEntry.builder("Links.Link.Line")
            .chatMessage(GRAY.wrap("▸ ") + GENERIC_NAME);

    public static final MessageLocale LINK_COMMAND_ONLY = LangEntry.builder("Links.Link.CommandOnly")
            .chatMessage(SOFT_GREEN.wrap("▸ ") + GOLD.wrap("Link activated."));

    public static final MessageLocale ERROR_NO_PERMISSION = LangEntry.builder("Links.Error.NoPermission")
            .chatMessage(SOFT_RED.wrap("You don't have permission to open this link."));

    public static final MessageLocale ERROR_NO_USE_PERMISSION = LangEntry.builder("Links.Error.NoUsePermission")
            .chatMessage(SOFT_RED.wrap("You don't have permission to use this link."));

    public static final MessageLocale ERROR_COOLDOWN = LangEntry.builder("Links.Error.Cooldown")
            .chatMessage(SOFT_RED.wrap("Wait ") + WHITE.wrap(GENERIC_TIME) + SOFT_RED.wrap(" before using this link again."));

    public static final MessageLocale ERROR_COST = LangEntry.builder("Links.Error.Cost")
            .chatMessage(GRAY.wrap("You don't have enough funds. Required: " + WHITE.wrap(GENERIC_AMOUNT) + "."));

    public static final MessageLocale FIRST_REWARD_NOTIFY = LangEntry.builder("Links.FirstReward.Notify")
            .chatMessage(SOFT_GREEN.wrap("First-click reward claimed!"));

    public static final MessageLocale STATS_HEADER = LangEntry.builder("Links.Stats.Header")
            .chatMessage(DARK_GRAY.wrap("[") + GOLD.wrap("Link Stats") + DARK_GRAY.wrap("]"));

    public static final MessageLocale STATS_LINE = LangEntry.builder("Links.Stats.Line")
            .chatMessage(GRAY.wrap("▸ ") + GOLD.wrap(GENERIC_NAME) + GRAY.wrap(": ") + WHITE.wrap(GENERIC_VALUE)
                    + GRAY.wrap(" clicks, ") + WHITE.wrap(GENERIC_TOTAL) + GRAY.wrap(" unique"));

    public static final MessageLocale STATS_EMPTY = LangEntry.builder("Links.Stats.Empty")
            .chatMessage(SOFT_RED.wrap("No link statistics yet."));

    public static final MessageLocale ERROR_NOT_ACTIONABLE = LangEntry.builder("Links.Error.NotActionable")
            .chatMessage(SOFT_RED.wrap("This link has no URL and no command configured."));

    // Admin feedback

    public static final MessageLocale ADMIN_CREATE_FEEDBACK = LangEntry.builder("Links.Admin.Create.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" created."));

    public static final MessageLocale ADMIN_CREATE_ERROR_EXISTS = LangEntry.builder("Links.Admin.Create.ErrorExists")
            .chatMessage(SOFT_RED.wrap("A link with the ID '") + RED.wrap(GENERIC_VALUE) + SOFT_RED.wrap("' already exists."));

    public static final MessageLocale ADMIN_CREATE_ERROR_ID = LangEntry.builder("Links.Admin.Create.ErrorId")
            .chatMessage(SOFT_RED.wrap("'") + RED.wrap(GENERIC_VALUE) + SOFT_RED.wrap(
                    "' is not a valid link ID. Allowed characters: a-z, 0-9 and underscore, max 32 chars."));

    public static final MessageLocale ADMIN_DELETE_FEEDBACK = LangEntry.builder("Links.Admin.Delete.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" deleted."));

    public static final MessageLocale ADMIN_TOGGLE_FEEDBACK = LangEntry.builder("Links.Admin.Toggle.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" is now ")
                    + GENERIC_NEW_VALUE + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_URL_FEEDBACK = LangEntry.builder("Links.Admin.SetUrl.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" URL set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_NAME_FEEDBACK = LangEntry.builder("Links.Admin.SetName.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" renamed to ")
                    + GENERIC_NEW_VALUE + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_COMMAND_FEEDBACK = LangEntry.builder("Links.Admin.SetCommand.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" command set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_EXECUTOR_FEEDBACK = LangEntry
            .builder("Links.Admin.SetExecutor.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" executor set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_PERMISSION_FEEDBACK = LangEntry
            .builder("Links.Admin.SetPermission.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" view permission set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_USE_PERMISSION_FEEDBACK = LangEntry
            .builder("Links.Admin.SetUsePermission.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" use permission set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_COOLDOWN_FEEDBACK = LangEntry
            .builder("Links.Admin.SetCooldown.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" cooldown set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE + "s") + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_COST_FEEDBACK = LangEntry
            .builder("Links.Admin.SetCost.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" cost set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_REWARD_FEEDBACK = LangEntry
            .builder("Links.Admin.SetReward.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" first-click reward set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_SOUND_FEEDBACK = LangEntry
            .builder("Links.Admin.SetSound.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" sound set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_ACTIONBAR_FEEDBACK = LangEntry
            .builder("Links.Admin.SetActionbar.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" actionbar set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_PARTICLE_FEEDBACK = LangEntry
            .builder("Links.Admin.SetParticle.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" particle set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_INVALID_NUMBER = LangEntry.builder("Links.Admin.Set.InvalidNumber")
            .chatMessage(SOFT_RED.wrap("'") + RED.wrap(GENERIC_VALUE) + SOFT_RED.wrap("' is not a valid number."));

    public static final MessageLocale ADMIN_SET_INVALID_SOUND = LangEntry.builder("Links.Admin.Set.InvalidSound")
            .chatMessage(SOFT_RED.wrap("'") + RED.wrap(GENERIC_VALUE) + SOFT_RED.wrap("' is not a valid sound."));

    public static final MessageLocale ADMIN_SET_INVALID_PARTICLE = LangEntry.builder("Links.Admin.Set.InvalidParticle")
            .chatMessage(SOFT_RED.wrap("'") + RED.wrap(GENERIC_VALUE) + SOFT_RED.wrap("' is not a valid particle."));

    public static final MessageLocale ADMIN_SET_ICON_FEEDBACK = LangEntry.builder("Links.Admin.SetIcon.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" icon set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_SET_PRIORITY_FEEDBACK = LangEntry
            .builder("Links.Admin.SetPriority.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" priority set to ")
                    + AQUA.wrap(GENERIC_NEW_VALUE) + SOFT_GREEN.wrap("."));

    public static final MessageLocale ADMIN_RESET_CLICKS_FEEDBACK = LangEntry
            .builder("Links.Admin.ResetClicks.Feedback")
            .chatMessage(SOFT_GREEN.wrap("Link ") + GOLD.wrap(GENERIC_VALUE) + SOFT_GREEN.wrap(" click counter reset."));

    public static final MessageLocale ADMIN_RELOAD_REQUIRED = LangEntry.builder("Links.Admin.ReloadRequired")
            .chatMessage(SOFT_YELLOW.wrap("A restart or reload is required for a new link's command to become available."));

    public static final MessageLocale DIALOG_INPUT_INVALID = LangEntry.builder("Links.Dialog.Input.Invalid")
            .chatMessage(SOFT_RED.wrap("'") + RED.wrap(GENERIC_VALUE) + SOFT_RED.wrap(
                    "' is not a valid value. Nothing was changed."));

    // Menus

    public static final TextLocale MENU_TITLE_LIST = LangEntry.builder("Links.Menu.Title.List")
            .text("Server Links");

    public static final TextLocale MENU_TITLE_EDITOR = LangEntry.builder("Links.Menu.Title.Editor")
            .text("Link Editor");

    public static final TextLocale MENU_TITLE_SETTINGS = LangEntry.builder("Links.Menu.Title.Settings")
            .text("Link Settings");

    public static final TextLocale MENU_TITLE_ICONS = LangEntry.builder("Links.Menu.Title.Icons")
            .text("Select Icon");

    public static final IconLocale ICON_EDITOR_CREATE = LangEntry.iconBuilder("Links.Editor.Icon.Create")
            .accentColor(SOFT_GREEN)
            .name("Create Link")
            .appendInfo("Add a new server link.", "Opens a prompt for its ID.")
            .br()
            .appendClick("Click to create")
            .build();

    public static final IconLocale ICON_EDITOR_RETURN = LangEntry.iconBuilder("Links.Editor.Icon.Return")
            .accentColor(SOFT_GREEN)
            .name("Back to Links")
            .appendClick("Click to return")
            .build();

    public static final IconLocale ICON_SETTINGS_ENABLED = LangEntry.iconBuilder("Links.Editor.Icon.Enabled")
            .name("Enabled")
            .appendCurrent("Status", LinksPlaceholders.LINK_ENABLED)
            .appendInfo("Disabled links are hidden", "from every player-facing view.")
            .br()
            .appendClick("Click to toggle")
            .build();

    public static final IconLocale ICON_SETTINGS_NAME = LangEntry.iconBuilder("Links.Editor.Icon.DisplayName")
            .name("Display Name")
            .appendCurrent("Name", LinksPlaceholders.LINK_NAME)
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_ICON = LangEntry.iconBuilder("Links.Editor.Icon.Icon")
            .name("Icon")
            .appendInfo("The item shown in the link menu.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_URL = LangEntry.iconBuilder("Links.Editor.Icon.Url")
            .accentColor(AQUA)
            .name("URL")
            .appendCurrent("URL", LinksPlaceholders.LINK_URL)
            .appendInfo("Leave empty for a command-only link.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_COMMAND = LangEntry.iconBuilder("Links.Editor.Icon.Command")
            .accentColor(GOLD)
            .name("Command")
            .appendCurrent("Command", LinksPlaceholders.LINK_COMMAND)
            .appendInfo("Executed before the URL is sent.", "Placeholders: %player%, %player_uuid%,",
                    "%link_id%, %link_name%, %link_url%.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_EXECUTOR = LangEntry.iconBuilder("Links.Editor.Icon.Executor")
            .name("Command Executor")
            .appendCurrent("Executor", LinksPlaceholders.LINK_EXECUTOR)
            .appendInfo("Who runs the link's command.")
            .br()
            .appendClick("Click to toggle")
            .build();

    public static final IconLocale ICON_SETTINGS_PERMISSION = LangEntry.iconBuilder("Links.Editor.Icon.Permission")
            .accentColor(RED)
            .name("View Permission")
            .appendCurrent("Permission", LinksPlaceholders.LINK_PERMISSION)
            .appendInfo("Required to see the link.", "Checked in addition to the", "player-facing link permission.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_USE_PERMISSION = LangEntry.iconBuilder("Links.Editor.Icon.UsePermission")
            .accentColor(RED)
            .name("Use Permission")
            .appendCurrent("Permission", LinksPlaceholders.LINK_USE_PERMISSION)
            .appendInfo("Required to activate the link.", "Empty = anyone who can see it.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_COOLDOWN = LangEntry.iconBuilder("Links.Editor.Icon.Cooldown")
            .name("Cooldown")
            .appendCurrent("Seconds", LinksPlaceholders.LINK_COOLDOWN)
            .appendInfo("Per-player cooldown between uses.", "0 = no cooldown.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_COST = LangEntry.iconBuilder("Links.Editor.Icon.Cost")
            .accentColor(GOLD)
            .name("Cost")
            .appendCurrent("Cost", LinksPlaceholders.LINK_COST)
            .appendInfo("Vault cost per use.", "0 = free.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_REWARD = LangEntry.iconBuilder("Links.Editor.Icon.Reward")
            .accentColor(GREEN)
            .name("First-Click Reward")
            .appendInfo("Command run once per player", "on their first click.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_SOUND = LangEntry.iconBuilder("Links.Editor.Icon.Sound")
            .accentColor(AQUA)
            .name("Click Sound")
            .appendInfo("Sound played on activation.", "Empty = silent.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_ACTIONBAR = LangEntry.iconBuilder("Links.Editor.Icon.Actionbar")
            .name("Actionbar")
            .appendInfo("Message shown above the hotbar", "on activation.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_PARTICLE = LangEntry.iconBuilder("Links.Editor.Icon.Particle")
            .name("Particle")
            .appendInfo("Particle spawned on activation.", "Empty = none.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_PRIORITY = LangEntry.iconBuilder("Links.Editor.Icon.Priority")
            .name("Priority")
            .appendCurrent("Priority", LinksPlaceholders.LINK_PRIORITY)
            .appendInfo("Links with a greater priority", "are listed first.")
            .br()
            .appendClick("Click to change")
            .build();

    public static final IconLocale ICON_SETTINGS_CLICKS = LangEntry.iconBuilder("Links.Editor.Icon.Clicks")
            .accentColor(ORANGE)
            .name("Clicks")
            .appendCurrent("Clicks", LinksPlaceholders.LINK_CLICKS)
            .appendInfo("Times the link was opened", "by a player.")
            .br()
            .appendClick("Click to reset")
            .build();

    public static final IconLocale ICON_SETTINGS_DELETE = LangEntry.iconBuilder("Links.Editor.Icon.Delete")
            .accentColor(RED)
            .name("Delete Link")
            .appendInfo("Permanently delete this link.")
            .br()
            .appendClick("Click to delete")
            .build();
}
