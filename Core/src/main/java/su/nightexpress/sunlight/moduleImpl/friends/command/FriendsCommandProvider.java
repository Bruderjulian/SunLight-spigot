package su.nightexpress.sunlight.moduleImpl.friends.command;

import java.util.Collection;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.friends.FriendToggle;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsConfig;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsLang;
import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsPerms;
import su.nightexpress.sunlight.moduleImpl.friends.dialog.FriendsDialogKeys;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRecord;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRequest;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.Utils;

/**
 * {@code /friend} and everything under it.
 * <p>
 * The root takes no argument and opens the menu when that is enabled, so the menu is the default
 * surface and every action still has a command form for players who prefer one.
 */
public class FriendsCommandProvider extends CommandProvider<FriendsModule> {

    private static final String COMMAND_ROOT = "friend";

    private static final String COMMAND_ADD     = "add";
    private static final String COMMAND_REMOVE  = "remove";
    private static final String COMMAND_ACCEPT  = "accept";
    private static final String COMMAND_DECLINE = "decline";
    private static final String COMMAND_CANCEL  = "cancel";
    private static final String COMMAND_LIST    = "list";
    private static final String COMMAND_PENDING = "pending";
    private static final String COMMAND_BLOCKED = "blocked";
    private static final String COMMAND_BLOCK   = "block";
    private static final String COMMAND_UNBLOCK = "unblock";
    private static final String COMMAND_TOGGLE  = "toggle";
    private static final String COMMAND_ON      = "on";
    private static final String COMMAND_OFF     = "off";
    private static final String COMMAND_JUMP    = "jump";
    private static final String COMMAND_MESSAGE = "message";
    private static final String COMMAND_FAVORITE = "favorite";

    public FriendsCommandProvider(FriendsModule module) {
        super(module, "friends");
    }

    @Override
    public void setup() {
        this.register(COMMAND_ADD, command -> command
                .withFullDescription(FriendsLang.COMMAND_ADD_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_ADD)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.friendSuggestions(info.sender())))
                .executes(this::addFriend))
            .under(COMMAND_ROOT);

        this.register(COMMAND_REMOVE, command -> command
                .withFullDescription(FriendsLang.COMMAND_REMOVE_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_REMOVE)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.friendSuggestions(info.sender())))
                .executes(this::removeFriend))
            .aliases("delete", "rm")
            .under(COMMAND_ROOT);

        this.register(COMMAND_ACCEPT, command -> command
                .withFullDescription(FriendsLang.COMMAND_ACCEPT_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_ACCEPT)
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.incomingSuggestions(info.sender())))
                .executes(this::acceptRequest))
            .aliases("yes")
            .under(COMMAND_ROOT);

        this.register(COMMAND_DECLINE, command -> command
                .withFullDescription(FriendsLang.COMMAND_DECLINE_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_DECLINE)
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.incomingSuggestions(info.sender())))
                .executes(this::declineRequest))
            .aliases("no", "deny")
            .under(COMMAND_ROOT);

        this.register(COMMAND_CANCEL, command -> command
                .withFullDescription(FriendsLang.COMMAND_CANCEL_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_CANCEL)
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.outgoingSuggestions(info.sender())))
                .executes(this::cancelRequest))
            .under(COMMAND_ROOT);

        this.register(COMMAND_LIST, command -> command
                .withFullDescription(FriendsLang.COMMAND_LIST_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_LIST)
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(CommandArgumentConstants.targetArgument())
                .executes(this::listFriends))
            .under(COMMAND_ROOT);

        this.register(COMMAND_PENDING, command -> command
                .withFullDescription(FriendsLang.COMMAND_PENDING_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_PENDING)
                .withRequirement(sender -> sender instanceof Player)
                .executes((sender, arguments) -> {
                    this.module.openPending((Player) sender);
                    return 1;
                }))
            .aliases("requests")
            .under(COMMAND_ROOT);

        this.register(COMMAND_BLOCKED, command -> command
                .withFullDescription(FriendsLang.COMMAND_BLOCKED_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_BLOCKED)
                .withRequirement(sender -> sender instanceof Player)
                .executes((sender, arguments) -> {
                    this.module.openBlocked((Player) sender);
                    return 1;
                }))
            .under(COMMAND_ROOT);

        this.register(COMMAND_BLOCK, command -> command
                .withFullDescription(FriendsLang.COMMAND_BLOCK_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_BLOCK)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.friendSuggestions(info.sender())))
                .executes(this::blockPlayer))
            .under(COMMAND_ROOT);

        this.register(COMMAND_UNBLOCK, command -> command
                .withFullDescription(FriendsLang.COMMAND_UNBLOCK_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_UNBLOCK)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.blockedSuggestions(info.sender())))
                .executes(this::unblockPlayer))
            .under(COMMAND_ROOT);

        this.register(COMMAND_FAVORITE, command -> command
                .withFullDescription(FriendsLang.COMMAND_FAVORITE_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_FAVORITE)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.friendSuggestions(info.sender())))
                .executes(this::toggleFavorite))
            .aliases("favourite", "pin")
            .under(COMMAND_ROOT);

        this.register(COMMAND_TOGGLE, command -> this.buildToggle(command, ToggleMode.TOGGLE,
                FriendsLang.COMMAND_TOGGLE_DESC)).under(COMMAND_ROOT);
        this.register(COMMAND_ON, command -> this.buildToggle(command, ToggleMode.ON,
                FriendsLang.COMMAND_TOGGLE_ON_DESC)).under(COMMAND_ROOT);
        this.register(COMMAND_OFF, command -> this.buildToggle(command, ToggleMode.OFF,
                FriendsLang.COMMAND_TOGGLE_OFF_DESC)).under(COMMAND_ROOT);

        this.register(COMMAND_JUMP, command -> command
                .withFullDescription(FriendsLang.COMMAND_JUMP_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_JUMP)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                        info -> this.onlineFriendSuggestions(info.sender())))
                .executes(this::jumpToFriend))
            .aliases("tp", "warp")
            .under(COMMAND_ROOT);

        this.register(COMMAND_MESSAGE, command -> command
                .withFullDescription(FriendsLang.COMMAND_MESSAGE_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_MESSAGE)
                .withRequirement(sender -> sender instanceof Player)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> this.onlineFriendSuggestions(info.sender())),
                        CommandArgumentConstants.greedy(CommandArgumentConstants.TEXT, info -> List.of()))
                .executes(this::messageFriend))
            .aliases("msg", "tell", "whisper", "w")
            .under(COMMAND_ROOT);

        this.registerRoot(COMMAND_ROOT, command -> command
                .withFullDescription(FriendsLang.COMMAND_ROOT_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_ROOT)
                .executes((sender, arguments) -> {
                    if (!(sender instanceof final Player player)) {
                        this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
                        return 0;
                    }
                    if (!FriendsConfig.COMMAND_GUI.get()) {
                        this.module.sendPrefixed(FriendsLang.ERROR_NO_FRIENDS, player);
                        return 0;
                    }
                    this.module.openFriends(player);
                    return 1;
                }))
            .aliases("friends", "f", "buddy", "bff");
    }

    private void buildToggle(CommandAPICommand command, ToggleMode mode, TextLocale description) {
        command.withFullDescription(description.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_TOGGLE)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.NAME,
                                info -> FriendToggle.ids()),
                        CommandArgumentConstants.targetArgument())
                .executes((sender, arguments) -> {
                    return this.toggle(sender, arguments, mode);
                });
    }

    private int toggle(CommandSender sender, CommandArguments arguments, ToggleMode mode) {
        Object nameObj = arguments.get(CommandArgumentConstants.NAME);
        String name = nameObj instanceof final String value ? value : null;

        FriendToggle toggle = FriendToggle.byId(name);
        if (toggle == null) {
            this.module.sendPrefixed(FriendsLang.TOGGLE_ERROR_UNKNOWN, sender, builder -> builder
                    .with(su.nightexpress.nightcore.util.placeholder.CommonPlaceholders.GENERIC_INPUT, () -> String
                    .valueOf(name))
                    .with(SLPlaceholders.GENERIC_LIST, () -> String.join(", ", FriendToggle.ids())));
            return 0;
        }

        CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, FriendsPerms.COMMAND_FRIENDS_TOGGLE_OTHERS, (user, targetPlayer) -> {
            boolean state = mode.apply(toggle.get(user));

            this.module.applyToggle(user, toggle, state);

            if (sender != targetPlayer) {
                this.module.sendPrefixed(FriendsLang.TOGGLE_FEEDBACK, sender, builder -> builder
                        .with(CommonPlaceholders.PLAYER.resolver(targetPlayer))
                        .with(SLPlaceholders.GENERIC_NAME, toggle::getDisplay)
                        .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
            }

            this.module.sendToggle(targetPlayer, toggle, state, sender);
        });
    }

    private int addFriend(CommandSender sender, CommandArguments arguments) {
        final Player player = this.requirePlayer(sender);
        if (player == null) {
            return 0;
        }

        final String name = this.string(arguments, CommandArgumentConstants.PLAYER);
        final Player target = name == null ? null : Utils.getPlayer(name);
        if (target == null || !this.canSee(sender, target)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return this.module.sendRequest(player, target) ? 1 : 0;
    }

    private int removeFriend(CommandSender sender, CommandArguments arguments) {
        final Player player = this.requirePlayer(sender);
        if (player == null) {
            return 0;
        }

        final String name = this.string(arguments, CommandArgumentConstants.PLAYER);
        final Player target = name == null ? null : Utils.getPlayer(name);
        if (target == null || !this.canSee(sender, target)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return this.module.removeFriendCommand(player, target) ? 1 : 0;
    }

    private int acceptRequest(CommandSender sender, CommandArguments arguments) {
        final Player player = this.requirePlayer(sender);
        if (player == null) {
            return 0;
        }
        return this.module.acceptIncoming(player, this.string(arguments, CommandArgumentConstants.PLAYER)) ? 1 : 0;
    }

    private int declineRequest(CommandSender sender, CommandArguments arguments) {
        final Player player = this.requirePlayer(sender);
        if (player == null) {
            return 0;
        }
        return this.module.declineIncoming(player, this.string(arguments, CommandArgumentConstants.PLAYER)) ? 1 : 0;
    }

    private int cancelRequest(CommandSender sender, CommandArguments arguments) {
        final Player player = this.requirePlayer(sender);
        if (player == null) {
            return 0;
        }
        return this.module.cancelRequest(player, this.string(arguments, CommandArgumentConstants.PLAYER)) ? 1 : 0;
    }

    private int listFriends(CommandSender sender, CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, FriendsPerms.COMMAND_FRIENDS_LIST_OTHERS,
                (user, targetPlayer) -> this.module.openFriends(player, targetPlayer.getUniqueId()));
    }

    private int blockPlayer(CommandSender sender, CommandArguments arguments) {
        final Player player = this.requirePlayer(sender);
        if (player == null) {
            return 0;
        }

        final String name = this.string(arguments, CommandArgumentConstants.PLAYER);
        final Player target = name == null ? null : Utils.getPlayer(name);
        if (target == null || !this.canSee(sender, target)) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return this.module.blockCommand(player, target) ? 1 : 0;
    }

    private int unblockPlayer(CommandSender sender, CommandArguments arguments) {
        final Player player = this.requirePlayer(sender);
        if (player == null) {
            return 0;
        }

        final String name = this.string(arguments, CommandArgumentConstants.PLAYER);
        if (name == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return this.module.unblockCommand(player, name) ? 1 : 0;
    }

    private int toggleFavorite(CommandSender sender, CommandArguments arguments) {
        final Player player = this.requirePlayer(sender);
        if (player == null) {
            return 0;
        }

        final String name = this.string(arguments, CommandArgumentConstants.PLAYER);
        final Player target = name == null ? null : Utils.getPlayer(name);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return this.module.toggleFavorite(player, target.getUniqueId()) ? 1 : 0;
    }

    private int jumpToFriend(CommandSender sender, CommandArguments arguments) {
        final Player player = this.requirePlayer(sender);
        if (player == null) {
            return 0;
        }

        final String name = this.string(arguments, CommandArgumentConstants.PLAYER);
        if (name == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return this.module.jumpToFriend(player, name) ? 1 : 0;
    }

    private int messageFriend(CommandSender sender, CommandArguments arguments) {
        final Player player = this.requirePlayer(sender);
        if (player == null) {
            return 0;
        }

        final String name = this.string(arguments, CommandArgumentConstants.PLAYER);
        final String message = this.string(arguments, CommandArgumentConstants.TEXT);
        if (name == null || message == null) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_MISSING_ARGUMENTS, sender);
            return 0;
        }

        return this.module.messageFriend(player, name, message) ? 1 : 0;
    }

    // Helpers

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof final Player player) {
            return player;
        }
        this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
        return null;
    }

    private String string(CommandArguments arguments, String key) {
        Object raw = arguments.get(key);
        return raw instanceof final String value ? value.trim() : null;
    }

    private Collection<String> friendSuggestions(CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return List.of();
        }
        return this.module.getFriendRecords(player.getUniqueId()).stream()
                .map(record -> record.getFriend().name())
                .toList();
    }

    private Collection<String> onlineFriendSuggestions(CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return List.of();
        }
        return this.module.getOnlineFriendRecords(player.getUniqueId()).stream()
                .map(record -> record.getFriend().name())
                .toList();
    }

    private Collection<String> incomingSuggestions(CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return List.of();
        }
        return this.module.getRequestRepository().getIncoming(player.getUniqueId()).stream()
                .map(FriendRequest::getSenderName)
                .toList();
    }

    private Collection<String> outgoingSuggestions(CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return List.of();
        }
        return this.module.getRequestRepository().getOutgoing(player.getUniqueId()).stream()
                .map(FriendRequest::getTargetName)
                .toList();
    }

    private Collection<String> blockedSuggestions(CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            return List.of();
        }
        return this.module.getRepository().getBlocks(player.getUniqueId()).stream()
                .map(block -> block.getBlocked().name())
                .toList();
    }
}