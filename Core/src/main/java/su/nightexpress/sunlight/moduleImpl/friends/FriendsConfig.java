package su.nightexpress.sunlight.moduleImpl.friends;

import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.integration.permission.PermissionPlugins;
import su.nightexpress.nightcore.util.RankTable;

import java.util.Set;

/**
 * Settings of the Friends module, read through {@code config.initializeOptions(FriendsConfig.class)}
 * like the Reports and Warmups modules do.
 */
public class FriendsConfig {

    public static final ConfigValue<String> DATA_TABLE_PREFIX = ConfigValue.create("Friends.Data.Table-Prefix",
            "sunlight_friends",
            "Prefix for the friendship and block tables."
    );

    private static final ConfigValue.Writer<RankTable> WRITE_RANK_TABLE = (config, path, table) -> table
            .write(config, path);

    public static final ConfigValue<RankTable> AMOUNT_PER_RANK = ConfigValue.create("Friends.Limits.Amount-Per-Rank",
            (config, path) -> RankTable.read(config, path),
            WRITE_RANK_TABLE,
            () -> RankTable.builder(RankTable.Mode.RANK, 20)
                    .addRankValue("vip", 40)
                    .addRankValue("gold", 60)
                    .addRankValue("admin", -1)
                    .permissionPrefix("friends.amount.")
                    .build(),
            "How many friends a player may hold, by their rank.",
            "If a player is in several groups listed here, the greater value wins.",
            "Use '-1' for unlimited.",
            "[*] Requires %s with a compatible permissions plugin OR %s to work.".formatted(PermissionPlugins.VAULT,
                    PermissionPlugins.LUCK_PERMS)
    );

    public static final ConfigValue<RankTable> REQUESTS_PER_RANK = ConfigValue.create("Friends.Limits.Requests-Per-Rank",
            (config, path) -> RankTable.read(config, path),
            WRITE_RANK_TABLE,
            () -> RankTable.builder(RankTable.Mode.RANK, 20)
                    .addRankValue("vip", 40)
                    .addRankValue("gold", 60)
                    .addRankValue("admin", -1)
                    .permissionPrefix("friends.requests.")
                    .build(),
            "How many friends a player may keep at once, counted from both ends of every request.",
            "An incoming request from somebody already at their cap is refused, because accepting it",
            "would push them over. Use '-1' for unlimited.",
            "[*] Requires %s with a compatible permissions plugin OR %s to work.".formatted(PermissionPlugins.VAULT,
                    PermissionPlugins.LUCK_PERMS)
    );

    public static final ConfigValue<Integer> REQUEST_TIMEOUT_SECONDS = ConfigValue.create(
            "Friends.Requests.Timeout-Seconds",
            120,
            "How long a pending friend request stays actionable, in seconds.",
            "An expired request is dropped on the next read rather than lingering forever.",
            "Use 0 to never expire requests."
    );

    public static final ConfigValue<Integer> REQUEST_COOLDOWN_SECONDS = ConfigValue.create(
            "Friends.Requests.Cooldown-Seconds",
            10,
            "Seconds between two friend requests sent by the same player.",
            "Stored in the player's command cooldown data, so it survives a relog.",
            "Use 0 to disable."
    );

    public static final ConfigValue<Integer> REQUESTS_PER_DAY = ConfigValue.create("Friends.Requests.Per-Day",
            40,
            "How many friend requests a player may send per real day. Use 0 for no limit.",
            "Bounds total volume, so one player cannot flood everybody's pending queue over time."
    );

    public static final ConfigValue<Integer> REQUESTS_PER_TARGET_PER_DAY = ConfigValue.create(
            "Friends.Requests.Per-Target-Per-Day",
            3,
            "How many requests one player may send to the same target per real day. Use 0 for no limit.",
            "The duplicate-request check already stops the immediate case; this bounds the patient one."
    );

    public static final ConfigValue<Integer> BLOCK_LIMIT = ConfigValue.create("Friends.Limits.Blocked",
            100,
            "How many players one player may block at once. Use -1 for unlimited."
    );

    public static final ConfigValue<Set<String>> JUMP_WORLD_BLACKLIST = ConfigValue.create(
            "Friends.Jump.World-Blacklist",
            Set.of(),
            "Worlds in which '/friend jump' does not work.",
            "A blocked world would otherwise be a way around whatever travel rule the server set there."
    );

    public static final ConfigValue<Double> JUMP_COST = ConfigValue.create("Friends.Jump.Cost",
            0D,
            "How much it costs (in Vault currency) to teleport to a friend.",
            "Set to '0' to disable."
    );

    public static final ConfigValue<Boolean> JUMP_REQUIRE_ONLINE = ConfigValue.create("Friends.Jump.Requires-Online",
            true,
            "When true, a friend must be online for '/friend jump' to do anything.",
            "Off by default would leave the command silently doing nothing for offline friends;",
            "with it on, an offline friend is reported instead."
    );

    public static final ConfigValue<Boolean> TELEPORT_BYPASSES_REQUESTS_DISABLED = ConfigValue.create(
            "Friends.Jump.Bypasses-Requests-Disabled",
            false,
            "When true, a friend may teleport to a player who has disabled incoming teleport requests.",
            "Off by default: the target's own toggle should win over the friendship.",
            "This only affects the friends shortcut, not the PTP module."
    );

    public static final ConfigValue<Boolean> NOTIFY_LOGIN_ENABLED = ConfigValue.create("Friends.Notify.Login.Enabled",
            true,
            "When enabled, players are told when a friend logs in or logs out.",
            "A player who does not want this turns off 'friends login alerts' for themselves."
    );

    public static final ConfigValue<Boolean> NOTIFY_REQUESTS_ENABLED = ConfigValue.create(
            "Friends.Notify.Requests.Enabled",
            true,
            "When enabled, sent, received, accepted and declined requests are announced to both parties."
    );

    public static final ConfigValue<String> NOTIFY_STAFF_PERMISSION = ConfigValue.create(
            "Friends.Notify.Staff.Permission",
            "sunlight.friends.notify",
            "Permission required to be told when a friendship is made between two other players.",
            "Leave empty to disable. The two parties are never told about this notification."
    );

    public static final ConfigValue<Boolean> COMMAND_GUI = ConfigValue.create("Friends.Command.Gui",
            true,
            "When true, '/friend' without arguments opens the friends menu.",
            "'/friend list' keeps working either way, so nobody is locked out of the command form."
    );

    public static final ConfigValue<Boolean> COMMAND_MENU_CLOSES_INVENTORY = ConfigValue.create(
            "Friends.Command.Menu-Close-Inventory",
            true,
            "When true, actions taken from a menu close it, so the player is not left staring at a",
            "menu whose contents the action already invalidated."
    );

    private FriendsConfig() {
    }
}