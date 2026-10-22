package su.nightexpress.sunlight.moduleImpl.friends.config;

import org.bukkit.Sound;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.IconLocale;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.config.Lang;

import static su.nightexpress.nightcore.util.placeholder.CommonPlaceholders.*;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.moduleImpl.friends.FriendsPlaceholders.*;

public class FriendsLang extends Lang {

    // Command descriptions

    public static final TextLocale COMMAND_ROOT_DESC          = LangEntry.builder("Friends.Command.Root.Desc").text("Friends commands.");
    public static final TextLocale COMMAND_ADD_DESC           = LangEntry.builder("Friends.Command.Add.Desc").text("Send a friend request.");
    public static final TextLocale COMMAND_REMOVE_DESC        = LangEntry.builder("Friends.Command.Remove.Desc").text("Remove a friend.");
    public static final TextLocale COMMAND_ACCEPT_DESC        = LangEntry.builder("Friends.Command.Accept.Desc").text("Accept a friend request.");
    public static final TextLocale COMMAND_DECLINE_DESC       = LangEntry.builder("Friends.Command.Decline.Desc").text("Decline a friend request.");
    public static final TextLocale COMMAND_CANCEL_DESC        = LangEntry.builder("Friends.Command.Cancel.Desc").text("Cancel a sent friend request.");
    public static final TextLocale COMMAND_LIST_DESC          = LangEntry.builder("Friends.Command.List.Desc").text("List of friends.");
    public static final TextLocale COMMAND_PENDING_DESC       = LangEntry.builder("Friends.Command.Pending.Desc").text("Pending friend requests.");
    public static final TextLocale COMMAND_BLOCKED_DESC       = LangEntry.builder("Friends.Command.Blocked.Desc").text("List of blocked players.");
    public static final TextLocale COMMAND_BLOCK_DESC         = LangEntry.builder("Friends.Command.Block.Desc").text("Block a player.");
    public static final TextLocale COMMAND_UNBLOCK_DESC       = LangEntry.builder("Friends.Command.Unblock.Desc").text("Unblock a player.");
    public static final TextLocale COMMAND_TOGGLE_DESC        = LangEntry.builder("Friends.Command.Toggle.Desc").text("Toggle a friends setting.");
    public static final TextLocale COMMAND_TOGGLE_ON_DESC     = LangEntry.builder("Friends.Command.Toggle.On.Desc").text("Enable a friends setting.");
    public static final TextLocale COMMAND_TOGGLE_OFF_DESC    = LangEntry.builder("Friends.Command.Toggle.Off.Desc").text("Disable a friends setting.");
    public static final TextLocale COMMAND_JUMP_DESC          = LangEntry.builder("Friends.Command.Jump.Desc").text("Teleport to a friend.");
    public static final TextLocale COMMAND_MESSAGE_DESC       = LangEntry.builder("Friends.Command.Message.Desc").text("Send a private message to a friend.");
    public static final TextLocale COMMAND_FAVORITE_DESC      = LangEntry.builder("Friends.Command.Favorite.Desc").text("Pin a friend as your favourite.");
    public static final TextLocale COMMAND_ADMIN_ROOT_DESC    = LangEntry.builder("Friends.Command.Admin.Root.Desc").text("Friends admin commands.");
    public static final TextLocale COMMAND_ADMIN_INFO_DESC    = LangEntry.builder("Friends.Command.Admin.Info.Desc").text("Show a player's friends.");
    public static final TextLocale COMMAND_ADMIN_ADD_DESC     = LangEntry.builder("Friends.Command.Admin.Add.Desc").text("Force a friendship.");
    public static final TextLocale COMMAND_ADMIN_REMOVE_DESC  = LangEntry.builder("Friends.Command.Admin.Remove.Desc").text("Remove a friendship.");
    public static final TextLocale COMMAND_ADMIN_UNBLOCK_DESC = LangEntry.builder("Friends.Command.Admin.Unblock.Desc").text("Lift a block in both directions.");

    // Data

    public static final MessageLocale ERROR_DATA_NOT_LOADED = LangEntry.builder("Friends.Error.DataNotLoaded").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("Please wait, the friendships are still loading...")
    );

    // Requests

    public static final MessageLocale REQUEST_SENT_FEEDBACK = LangEntry.builder("Friends.Request.Sent.Feedback").chatMessage(
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            GRAY.wrap("You have sent " + ORANGE.wrap(PLAYER_NAME) + " a friend request.")
    );

    public static final MessageLocale REQUEST_SENT_NOTIFY = LangEntry.builder("Friends.Request.Sent.Notify").titleMessage(
            YELLOW.wrap(BOLD.wrap("Friend Request!")),
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " wants to be your friend."),
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP
    );

    public static final MessageLocale REQUEST_ERROR_DISABLED = LangEntry.builder("Friends.Request.Error.Disabled").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " does not accept friend requests.")
    );

    public static final MessageLocale REQUEST_ERROR_ALREADY_FRIEND = LangEntry.builder("Friends.Request.Error.AlreadyFriend").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You are already friends with " + ORANGE.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale REQUEST_ERROR_SELF = LangEntry.builder("Friends.Request.Error.Self").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You can't add yourself as a friend!")
    );

    public static final MessageLocale REQUEST_ERROR_BLOCKED = LangEntry.builder("Friends.Request.Error.Blocked").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " is not accepting friend requests.")
    );

    public static final MessageLocale REQUEST_ERROR_PENDING = LangEntry.builder("Friends.Request.Error.Pending").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("There is already a pending request between you and " + ORANGE.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale REQUEST_ERROR_LIMIT = LangEntry.builder("Friends.Request.Error.Limit").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You have reached your friend limit of " + ORANGE.wrap(SLPlaceholders.GENERIC_MAX) + ".")
    );

    public static final MessageLocale REQUEST_ERROR_OTHER_LIMIT = LangEntry.builder("Friends.Request.Error.OtherLimit").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " has reached their own friend limit.")
    );

    public static final MessageLocale REQUEST_ERROR_COOLDOWN = LangEntry.builder("Friends.Request.Error.Cooldown").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You have to wait " + ORANGE.wrap(SLPlaceholders.GENERIC_TIME) + " before sending another request.")
    );

    public static final MessageLocale REQUEST_ERROR_DAILY_LIMIT = LangEntry.builder("Friends.Request.Error.DailyLimit").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You have sent your " + ORANGE.wrap(SLPlaceholders.GENERIC_MAX) + " friend requests for today.")
    );

    public static final MessageLocale REQUEST_ERROR_TARGET_DAILY_LIMIT = LangEntry.builder("Friends.Request.Error.TargetDailyLimit").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You have sent " + ORANGE.wrap(SLPlaceholders.GENERIC_MAX) + " requests to " + ORANGE.wrap(PLAYER_NAME)
                    + " today already.")
    );

    public static final MessageLocale REQUEST_ACCEPT_FEEDBACK = LangEntry.builder("Friends.Request.Accept.Feedback").titleMessage(
            GREEN.wrap(BOLD.wrap("Friend Added!")),
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " is now your friend."),
            Sound.ENTITY_PLAYER_LEVELUP
    );

    public static final MessageLocale REQUEST_ACCEPT_NOTIFY = LangEntry.builder("Friends.Request.Accept.Notify").titleMessage(
            GREEN.wrap(BOLD.wrap("Friend Added!")),
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " accepted your friend request."),
            Sound.ENTITY_PLAYER_LEVELUP
    );

    public static final MessageLocale REQUEST_DECLINE_FEEDBACK = LangEntry.builder("Friends.Request.Decline.Feedback").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You have declined the friend request from " + ORANGE.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale REQUEST_DECLINE_NOTIFY = LangEntry.builder("Friends.Request.Decline.Notify").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " declined your friend request.")
    );

    public static final MessageLocale REQUEST_CANCEL_FEEDBACK = LangEntry.builder("Friends.Request.Cancel.Feedback").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You have cancelled your friend request to " + ORANGE.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale REQUEST_CANCEL_NOTIFY = LangEntry.builder("Friends.Request.Cancel.Notify").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " cancelled their friend request.")
    );

    public static final MessageLocale REQUEST_NOTHING_PENDING = LangEntry.builder("Friends.Request.NothingPending").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("There is no pending friend request from " + ORANGE.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale REQUEST_NOTHING_SENT = LangEntry.builder("Friends.Request.NothingSent").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You have no pending friend request to " + ORANGE.wrap(PLAYER_NAME) + ".")
    );

    // Friends

    public static final MessageLocale REMOVE_FEEDBACK = LangEntry.builder("Friends.Remove.Feedback").titleMessage(
            RED.wrap(BOLD.wrap("Friend Removed!")),
            GRAY.wrap("You have removed " + RED.wrap(PLAYER_NAME) + " from your friends."),
            Sound.ENTITY_VILLAGER_NO
    );

    public static final MessageLocale REMOVE_NOTIFY = LangEntry.builder("Friends.Remove.Notify").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " removed you from their friends.")
    );

    public static final MessageLocale REMOVE_ERROR_NOT_FRIEND = LangEntry.builder("Friends.Remove.Error.NotFriend").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " is not your friend.")
    );

    public static final MessageLocale ERROR_NO_FRIENDS = LangEntry.builder("Friends.Error.NoFriends").chatMessage(
            GRAY.wrap("You have no friends yet.")
    );

    public static final MessageLocale ERROR_NO_PENDING = LangEntry.builder("Friends.Error.NoPending").chatMessage(
            GRAY.wrap("You have no pending friend requests.")
    );

    public static final MessageLocale ERROR_NO_BLOCKED = LangEntry.builder("Friends.Error.NoBlocked").chatMessage(
            GRAY.wrap("You have not blocked anybody.")
    );

    public static final MessageLocale FAVORITE_SET = LangEntry.builder("Friends.Favorite.Set").chatMessage(
            Sound.BLOCK_NOTE_BLOCK_PLING,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " is now your favourite friend.")
    );

    public static final MessageLocale FAVORITE_UNSET = LangEntry.builder("Friends.Favorite.Unset").chatMessage(
            Sound.BLOCK_NOTE_BLOCK_PLING,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " is no longer your favourite friend.")
    );

    // Blocks

    public static final MessageLocale BLOCK_DONE_FEEDBACK = LangEntry.builder("Friends.Block.Done.Feedback").chatMessage(
            Sound.BLOCK_ANVIL_LAND,
            GRAY.wrap("You have blocked " + RED.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale BLOCK_DONE_NOTIFY = LangEntry.builder("Friends.Block.Done.Notify").chatMessage(
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " has removed you as a friend.")
    );

    public static final MessageLocale BLOCK_ERROR_ALREADY = LangEntry.builder("Friends.Block.Error.Already").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You have already blocked " + ORANGE.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale BLOCK_ERROR_SELF = LangEntry.builder("Friends.Block.Error.Self").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You can't block yourself!")
    );

    public static final MessageLocale BLOCK_ERROR_LIMIT = LangEntry.builder("Friends.Block.Error.Limit").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You have reached your block limit of " + ORANGE.wrap(SLPlaceholders.GENERIC_MAX) + ".")
    );

    public static final MessageLocale UNBLOCK_DONE = LangEntry.builder("Friends.Unblock.Done").chatMessage(
            Sound.BLOCK_NOTE_BLOCK_PLING,
            GRAY.wrap("You have unblocked " + GREEN.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale UNBLOCK_ERROR_NOT_BLOCKED = LangEntry.builder("Friends.Unblock.Error.NotBlocked").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " is not blocked.")
    );

    // Jump

    public static final MessageLocale JUMP_SUCCESS = LangEntry.builder("Friends.Jump.Success").titleMessage(
            ORANGE.wrap(BOLD.wrap("Teleport")),
            GRAY.wrap("You have teleported to " + ORANGE.wrap(PLAYER_NAME) + "."),
            Sound.ENTITY_ENDERMAN_TELEPORT
    );

    public static final MessageLocale JUMP_ERROR_NOT_FRIEND = LangEntry.builder("Friends.Jump.Error.NotFriend").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " is not your friend.")
    );

    public static final MessageLocale JUMP_ERROR_OFFLINE = LangEntry.builder("Friends.Jump.Error.Offline").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " is not online right now.")
    );

    public static final MessageLocale JUMP_ERROR_DISABLED = LangEntry.builder("Friends.Jump.Error.Disabled").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " does not allow friends to teleport to them.")
    );

    public static final MessageLocale JUMP_ERROR_WORLD = LangEntry.builder("Friends.Jump.Error.World").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You can't teleport to friends in this world.")
    );

    public static final MessageLocale MESSAGE_PROMPT = LangEntry.builder("Friends.Message.Prompt").chatMessage(
            GRAY.wrap("Use " + GREEN.wrap("/friend message " + PLAYER_NAME + " <message>") + " to write to "
                    + ORANGE.wrap(PLAYER_NAME) + ".")
    );

    // Toggles

    public static final MessageLocale TOGGLE_NOTIFY = LangEntry.builder("Friends.Toggle.Notify").chatMessage(
            GRAY.wrap("You have set " + GRAY.wrap(SLPlaceholders.GENERIC_NAME) + " to "
                    + WHITE.wrap(SLPlaceholders.GENERIC_STATE) + ".")
    );

    public static final MessageLocale TOGGLE_FEEDBACK = LangEntry.builder("Friends.Toggle.Feedback").chatMessage(
            GRAY.wrap("You have set " + ORANGE.wrap(PLAYER_NAME) + "'s " + GRAY.wrap(SLPlaceholders.GENERIC_NAME) + " to "
                    + WHITE.wrap(SLPlaceholders.GENERIC_STATE) + ".")
    );

    public static final MessageLocale TOGGLE_ERROR_UNKNOWN = LangEntry.builder("Friends.Toggle.Error.Unknown").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap(SOFT_RED.wrap(GENERIC_INPUT) + " is not a friends setting. Use: "
                    + GRAY.wrap(SLPlaceholders.GENERIC_LIST) + ".")
    );

    // Login alerts

    public static final MessageLocale NOTIFY_LOGIN = LangEntry.builder("Friends.Notify.Login").chatMessage(
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " just joined the server.")
    );

    public static final MessageLocale NOTIFY_QUIT = LangEntry.builder("Friends.Notify.Quit").chatMessage(
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " left the server.")
    );

    public static final MessageLocale NOTIFY_ADMIN_ADD = LangEntry.builder("Friends.Notify.Admin.Add").chatMessage(
            GRAY.wrap(PLAYER_NAME + " and " + SLPlaceholders.GENERIC_TARGET + " are now friends.")
    );

    // Admin

    public static final MessageLocale ADMIN_INFO_FEEDBACK = LangEntry.builder("Friends.Admin.Info.Feedback").chatMessage(
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " has " + WHITE.wrap(FRIEND_LIST) + " friends ("
                    + WHITE.wrap(GENERIC_AMOUNT) + "/" + WHITE.wrap(SLPlaceholders.GENERIC_MAX) + ").")
    );

    public static final MessageLocale ADMIN_ADD_FEEDBACK = LangEntry.builder("Friends.Admin.Add.Feedback").chatMessage(
            Sound.BLOCK_NOTE_BLOCK_PLING,
            GRAY.wrap(ORANGE.wrap(PLAYER_NAME) + " and " + ORANGE.wrap(SLPlaceholders.GENERIC_TARGET) + " are now friends.")
    );

    public static final MessageLocale ADMIN_ADD_NOTIFY = LangEntry.builder("Friends.Admin.Add.Notify").chatMessage(
            GRAY.wrap("A staff member has made you friends with " + ORANGE.wrap(PLAYER_NAME) + ".")
    );

    public static final MessageLocale ADMIN_ADD_ERROR_ALREADY = LangEntry.builder("Friends.Admin.Add.Error.Already").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("They are already friends.")
    );

    public static final MessageLocale ADMIN_REMOVE_FEEDBACK = LangEntry.builder("Friends.Admin.Remove.Feedback").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("You have removed the friendship between " + ORANGE.wrap(PLAYER_NAME) + " and "
                    + ORANGE.wrap(SLPlaceholders.GENERIC_TARGET) + ".")
    );

    public static final MessageLocale ADMIN_REMOVE_ERROR_NOT_FRIEND = LangEntry.builder("Friends.Admin.Remove.Error.NotFriend").chatMessage(
            Sound.ENTITY_VILLAGER_NO,
            GRAY.wrap("They are not friends.")
    );

    public static final MessageLocale ADMIN_UNBLOCK_FEEDBACK = LangEntry.builder("Friends.Admin.Unblock.Feedback").chatMessage(
            Sound.BLOCK_NOTE_BLOCK_PLING,
            GRAY.wrap("Blocks between " + ORANGE.wrap(PLAYER_NAME) + " and " + ORANGE.wrap(SLPlaceholders.GENERIC_TARGET)
                    + " have been lifted.")
    );

    // Menus

    public static final TextLocale MENU_TITLE = LangEntry.builder("Friends.Menu.Title").text(BLACK.wrap("Friends"));
    public static final TextLocale MENU_PENDING_TITLE = LangEntry.builder("Friends.Menu.PendingTitle").text(BLACK.wrap("Friend Requests"));
    public static final TextLocale MENU_SETTINGS_TITLE = LangEntry.builder("Friends.Menu.SettingsTitle").text(BLACK.wrap("Friends Settings"));

    public static final IconLocale ICON_FRIEND_ONLINE = LangEntry.iconBuilder("Friends.UI.FriendOnline")
            .rawName(GREEN.wrap(FRIEND_NAME))
            .rawLore(GRAY.wrap(GREEN.wrap("✔") + " Online now"))
            .br()
            .appendClick("Click to open", GREEN)
            .build();

    public static final IconLocale ICON_FRIEND_OFFLINE = LangEntry.iconBuilder("Friends.UI.FriendOffline")
            .rawName(GRAY.wrap(FRIEND_NAME))
            .rawLore(GRAY.wrap("Last seen " + FRIEND_LAST_SEEN))
            .br()
            .appendClick("Click to open", GRAY)
            .build();

    public static final IconLocale ICON_FRIEND_FAVORITE = LangEntry.iconBuilder("Friends.UI.FriendFavorite")
            .rawName(SOFT_YELLOW.wrap("★ " + FRIEND_NAME))
            .rawLore(GRAY.wrap(SOFT_YELLOW.wrap("★") + " Your favourite friend."))
            .br()
            .appendClick("Click to open", SOFT_YELLOW)
            .build();

    public static final IconLocale ICON_PENDING_INCOMING = LangEntry.iconBuilder("Friends.UI.PendingIncoming")
            .rawName(GREEN.wrap("Incoming Request"))
            .rawLore(GRAY.wrap("From " + ORANGE.wrap(PLAYER_NAME)))
            .br()
            .appendClick("Click to accept", GREEN)
            .build();

    public static final IconLocale ICON_PENDING_OUTGOING = LangEntry.iconBuilder("Friends.UI.PendingOutgoing")
            .rawName(YELLOW.wrap("Sent Request"))
            .rawLore(GRAY.wrap("To " + ORANGE.wrap(PLAYER_NAME)))
            .br()
            .appendClick("Click to cancel", YELLOW)
            .build();

    private FriendsLang() {
    }
}