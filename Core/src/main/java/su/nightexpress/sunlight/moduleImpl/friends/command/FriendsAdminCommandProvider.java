package su.nightexpress.sunlight.moduleImpl.friends.command;

import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsPlaceholders;
import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsLang;
import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsPerms;
import su.nightexpress.sunlight.utils.Utils;

/**
 * {@code /friendadmin}: the staff surface.
 * <p>
 * Everything resolves to ids rather than to {@link Player} handles, because a friendship exists
 * between two accounts and staff must be able to fix a pair who are both offline.
 */
public class FriendsAdminCommandProvider extends CommandProvider<FriendsModule> {

    private static final String COMMAND_ROOT = "friendadmin";

    private static final String COMMAND_INFO    = "info";
    private static final String COMMAND_ADD     = "add";
    private static final String COMMAND_REMOVE  = "remove";
    private static final String COMMAND_UNBLOCK = "unblock";

    public FriendsAdminCommandProvider(FriendsModule module) {
        super(module, "friends-admin");
    }

    @Override
    public void setup() {
        this.register(COMMAND_INFO, command -> command
                .withFullDescription(FriendsLang.COMMAND_ADMIN_INFO_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_ADMIN_ROOT)
                .withArguments(CommandArgumentConstants.targetArgument())
                .executes(this::info))
            .aliases("list", "show")
            .under(COMMAND_ROOT);

        this.register(COMMAND_ADD, command -> command
                .withFullDescription(FriendsLang.COMMAND_ADMIN_ADD_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_ADMIN_ROOT)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> this.module.getKnownNames()),
                        CommandArgumentConstants.string(CommandArgumentConstants.TARGET,
                                info -> this.module.getKnownNames()))
                .executes(this::add))
            .under(COMMAND_ROOT);

        this.register(COMMAND_REMOVE, command -> command
                .withFullDescription(FriendsLang.COMMAND_ADMIN_REMOVE_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_ADMIN_ROOT)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> this.module.getKnownNames()),
                        CommandArgumentConstants.string(CommandArgumentConstants.TARGET,
                                info -> this.module.getKnownNames()))
                .executes(this::remove))
            .under(COMMAND_ROOT);

        this.register(COMMAND_UNBLOCK, command -> command
                .withFullDescription(FriendsLang.COMMAND_ADMIN_UNBLOCK_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_ADMIN_ROOT)
                .withArguments(CommandArgumentConstants.string(CommandArgumentConstants.PLAYER,
                                info -> this.module.getKnownNames()),
                        CommandArgumentConstants.string(CommandArgumentConstants.TARGET,
                                info -> this.module.getKnownNames()))
                .executes(this::unblock))
            .under(COMMAND_ROOT);

        this.registerRoot(COMMAND_ROOT, command -> command
                .withFullDescription(FriendsLang.COMMAND_ADMIN_ROOT_DESC.text())
                .withPermission(FriendsPerms.COMMAND_FRIENDS_ADMIN_ROOT))
            .aliases("fadmin");
    }

    private int info(CommandSender sender, CommandArguments arguments) {
        CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, FriendsPerms.COMMAND_FRIENDS_ADMIN_ROOT,
                (user, targetPlayer) -> {
                    int count = this.module.countFriends(targetPlayer.getUniqueId());
                    int limit = this.module.getFriendLimit(targetPlayer);

                    this.module.sendPrefixed(FriendsLang.ADMIN_INFO_FEEDBACK, sender, builder -> builder
                            .with(CommonPlaceholders.PLAYER.resolver(targetPlayer))
                            .with(FriendsPlaceholders.FRIEND_LIST, () -> String.valueOf(count))
                            .with(SLPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(count))
                            .with(SLPlaceholders.GENERIC_MAX,
                                    () -> limit < 0 ? CoreLang.OTHER_INFINITY.text() : String.valueOf(limit)));

                    this.module.openFriends(targetPlayer, targetPlayer.getUniqueId());
                });
    }

    private int add(CommandSender sender, CommandArguments arguments) {
        UserInfo first = this.resolve(arguments, CommandArgumentConstants.PLAYER);
        UserInfo second = this.resolve(arguments, CommandArgumentConstants.TARGET);
        if (first == null || second == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        if (first.id().equals(second.id())) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_NOT_YOURSELF, sender);
            return 0;
        }
        if (this.module.isFriend(first.id(), second.id())) {
            this.module.sendPrefixed(FriendsLang.ADMIN_ADD_ERROR_ALREADY, sender);
            return 0;
        }

        this.module.addFriend(first.id(), second.id());

        this.module.sendPrefixed(FriendsLang.ADMIN_ADD_FEEDBACK, sender, builder -> builder
                .with(CommonPlaceholders.PLAYER_NAME, first::name)
                .with(SLPlaceholders.GENERIC_TARGET, second::name));
        this.module.notifyAdminAdd(first, second);
        return 1;
    }

    private int remove(CommandSender sender, CommandArguments arguments) {
        UserInfo first = this.resolve(arguments, CommandArgumentConstants.PLAYER);
        UserInfo second = this.resolve(arguments, CommandArgumentConstants.TARGET);
        if (first == null || second == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }
        if (!this.module.isFriend(first.id(), second.id())) {
            this.module.sendPrefixed(FriendsLang.ADMIN_REMOVE_ERROR_NOT_FRIEND, sender);
            return 0;
        }

        this.module.removeFriend(first.id(), second.id());

        this.module.sendPrefixed(FriendsLang.ADMIN_REMOVE_FEEDBACK, sender, builder -> builder
                .with(CommonPlaceholders.PLAYER_NAME, first::name)
                .with(SLPlaceholders.GENERIC_TARGET, second::name));
        return 1;
    }

    private int unblock(CommandSender sender, CommandArguments arguments) {
        UserInfo first = this.resolve(arguments, CommandArgumentConstants.PLAYER);
        UserInfo second = this.resolve(arguments, CommandArgumentConstants.TARGET);
        if (first == null || second == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        // Both directions, because a staff member asking to clear a pair means the pair, not the
        // one arrow they happened to type first.
        this.module.unblock(first.id(), second.id());
        this.module.unblock(second.id(), first.id());

        this.module.sendPrefixed(FriendsLang.ADMIN_UNBLOCK_FEEDBACK, sender, builder -> builder
                .with(CommonPlaceholders.PLAYER_NAME, first::name)
                .with(SLPlaceholders.GENERIC_TARGET, second::name));
        return 1;
    }

    /** Resolves a name to a profile, online or cached, so offline pairs are still reachable. */
    private UserInfo resolve(CommandArguments arguments, String key) {
        Object raw = arguments.get(key);
        if (!(raw instanceof final String name)) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        Player online = Utils.getPlayer(trimmed);
        if (online != null) {
            return UserInfo.of(online);
        }

        UUID id = this.module.userManager().getRepository().getAssociatedId(trimmed);
        if (id == null) {
            return null;
        }
        return this.module.userManager().getRepository().getById(id)
                .map(user -> new UserInfo(user.getId(), user.getName()))
                .orElse(null);
    }
}