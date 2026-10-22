package su.nightexpress.sunlight.moduleImpl.friends;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.FriendsProvider;
import su.nightexpress.sunlight.api.provider.dto.FriendHandle;
import su.nightexpress.sunlight.api.provider.dto.FriendRelation;
import su.nightexpress.sunlight.api.provider.dto.FriendRequestHandle;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.friends.command.FriendsAdminCommandProvider;
import su.nightexpress.sunlight.moduleImpl.friends.command.FriendsCommandProvider;
import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsLang;
import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsPerms;
import su.nightexpress.sunlight.moduleImpl.friends.data.FriendsDataManager;
import su.nightexpress.sunlight.moduleImpl.friends.dialog.FriendAddDialog;
import su.nightexpress.sunlight.moduleImpl.friends.dialog.FriendBlockDialog;
import su.nightexpress.sunlight.moduleImpl.friends.dialog.FriendRemoveDialog;
import su.nightexpress.sunlight.moduleImpl.friends.dialog.FriendsDialogKeys;
import su.nightexpress.sunlight.moduleImpl.friends.event.PlayerFriendBlockEvent;
import su.nightexpress.sunlight.moduleImpl.friends.event.PlayerFriendChangeEvent;
import su.nightexpress.sunlight.moduleImpl.friends.event.PlayerFriendRequestEvent;
import su.nightexpress.sunlight.moduleImpl.friends.event.PreFriendBlockEvent;
import su.nightexpress.sunlight.moduleImpl.friends.event.PreFriendChangeEvent;
import su.nightexpress.sunlight.moduleImpl.friends.event.PreFriendRequestEvent;
import su.nightexpress.sunlight.moduleImpl.friends.listener.FriendsListener;
import su.nightexpress.sunlight.moduleImpl.friends.menu.FriendOptionsMenu;
import su.nightexpress.sunlight.moduleImpl.friends.menu.FriendsBlockedMenu;
import su.nightexpress.sunlight.moduleImpl.friends.menu.FriendsListMenu;
import su.nightexpress.sunlight.moduleImpl.friends.menu.FriendsPendingMenu;
import su.nightexpress.sunlight.moduleImpl.friends.menu.FriendsSettingsMenu;
import su.nightexpress.sunlight.moduleImpl.friends.model.BlockRecord;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRecord;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRequest;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendSnapshots;
import su.nightexpress.sunlight.moduleImpl.friends.repository.FriendsRepository;
import su.nightexpress.sunlight.moduleImpl.friends.request.FriendRequestRepository;
import su.nightexpress.sunlight.teleport.TeleportContext;
import su.nightexpress.sunlight.teleport.TeleportFlag;
import su.nightexpress.sunlight.teleport.TeleportManager;
import su.nightexpress.sunlight.teleport.TeleportType;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.EconomyUtils;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The Friends module.
 * <p>
 * The friendship graph and the block list live in the database and are mirrored in
 * {@link FriendsRepository}; pending requests live only in memory, like the PTP request queue.
 * <p>
 * A mutual friendship is two rows, one owned by each side, because the favourite flag is per-owner.
 * Every change goes through {@link #addFriend} or {@link #removeFriend}, which write both rows
 * together, so the module never leaves half a friendship behind.
 */
public class FriendsModule extends Module implements FriendsProvider {

    /** Cooldown key for {@code /friend add}, stored in the user's command cooldown data. */
    private static final su.nightexpress.sunlight.command.CommandKey REQUEST_COOLDOWN_KEY = new su.nightexpress.sunlight.command
            .CommandKey("friends", "add");

    private static final int FRIEND_LIST_LIMITED_COUNT = 10;

    private final TeleportManager teleportManager;
    private final FriendsDataManager dataManager;
    private final FriendsRepository repository;
    private final FriendRequestRepository requestRepository;

    private FriendsListMenu listMenu;
    private FriendsPendingMenu pendingMenu;
    private FriendsBlockedMenu blockedMenu;
    private FriendsSettingsMenu settingsMenu;
    private FriendOptionsMenu optionsMenu;

    private volatile boolean dataLoaded;

    public FriendsModule(ModuleDefinition<FriendsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.teleportManager = plugin.teleportManager();
        this.dataManager = new FriendsDataManager(this.dataHandler, this);
        this.repository = new FriendsRepository();
        this.requestRepository = new FriendRequestRepository();
    }

    @Override
    protected void loadModule(FileConfig config) {
        config.initializeOptions(FriendsConfig.class);

        // Must happen before any SunUser is deserialised: UserColumns drops any key it cannot find
        // in the registry, so an unregistered property is silently lost on every load.
        UserPropertyRegistry.register(FriendsProperties.REQUESTS_ENABLED);
        UserPropertyRegistry.register(FriendsProperties.LOGIN_ALERTS);
        UserPropertyRegistry.register(FriendsProperties.FRIEND_TELEPORT);
        UserPropertyRegistry.register(FriendsProperties.REQUEST_DAY_KEY);
        UserPropertyRegistry.register(FriendsProperties.REQUEST_DAY_COUNT);
        UserPropertyRegistry.register(FriendsProperties.REQUEST_TARGET_DAY_COUNTS);

        this.plugin.injectLang(FriendsLang.class);

        this.dataManager.init(FriendsConfig.DATA_TABLE_PREFIX.get());

        this.loadMenus();
        this.loadDialogs();
        this.loadData();

        this.addListener(new FriendsListener(this.plugin, this));

        this.commandRegistry.addProvider(new FriendsCommandProvider(this));
        this.commandRegistry.addProvider(new FriendsAdminCommandProvider(this));
    }

    @Override
    protected void unloadModule() {
        this.repository.clear();
        this.requestRepository.clear();
        this.dataLoaded = false;
        this.listMenu = null;
        this.pendingMenu = null;
        this.blockedMenu = null;
        this.settingsMenu = null;
        this.optionsMenu = null;
    }

    private void loadMenus() {
        this.listMenu = new FriendsListMenu(this.plugin, this);
        this.listMenu.load(FileConfig.load(this.getLocalUIPath(), FriendsFiles.FILE_UI_LIST));

        this.pendingMenu = new FriendsPendingMenu(this.plugin, this);
        this.pendingMenu.load(FileConfig.load(this.getLocalUIPath(), FriendsFiles.FILE_UI_PENDING));

        this.blockedMenu = new FriendsBlockedMenu(this.plugin, this);
        this.blockedMenu.load(FileConfig.load(this.getLocalUIPath(), FriendsFiles.FILE_UI_BLOCKED));

        this.settingsMenu = new FriendsSettingsMenu(this.plugin, this);
        this.settingsMenu.load(FileConfig.load(this.getLocalUIPath(), FriendsFiles.FILE_UI_SETTINGS));

        this.optionsMenu = new FriendOptionsMenu(this.plugin, this);
        this.optionsMenu.load(FileConfig.load(this.getLocalUIPath(), FriendsFiles.FILE_UI_OPTIONS));
    }

    private void loadDialogs() {
        this.dialogRegistry.register(FriendsDialogKeys.FRIEND_ADD, () -> new FriendAddDialog(this));
        this.dialogRegistry.register(FriendsDialogKeys.FRIEND_REMOVE, () -> new FriendRemoveDialog(this));
        this.dialogRegistry.register(FriendsDialogKeys.FRIEND_BLOCK, () -> new FriendBlockDialog(this));
    }

    private void loadData() {
        this.dataLoaded = false;
        this.plugin.runTaskAsync(() -> {
            for (FriendRecord record : this.dataManager.getFriends()) {
                this.repository.upsert(record);
            }
            for (BlockRecord block : this.dataManager.getBlocks()) {
                this.repository.upsertBlock(block);
            }
            this.dataLoaded = true;
            this.info("Loaded and cached " + this.repository.size() + " friendship rows.");
        });
    }

    public boolean isDataLoaded() {
        return this.dataLoaded;
    }

    public FriendsRepository getRepository() {
        return this.repository;
    }

    public FriendRequestRepository getRequestRepository() {
        return this.requestRepository;
    }

    public FriendsDataManager getDataManager() {
        return this.dataManager;
    }

    // Placeholders

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        // Counters are prefixed 'count_' rather than named after the metric, because the registry
        // resolves the longest matching key and hands the remainder to the handler as a payload:
        // a key like 'friends_online' would swallow '%sunlight_friends_online_notch%' and answer
        // with the count instead of failing.
        registry.register("friends_count", (player, payload) -> String
                .valueOf(this.countFriends(player.getUniqueId())));
        registry.register("friends_count_online", (player, payload) -> String
                .valueOf(this.countOnlineFriends(player.getUniqueId())));
        registry.register("friends_count_offline", (player, payload) -> String
                .valueOf(this.countFriends(player.getUniqueId()) - this.countOnlineFriends(player.getUniqueId())));
        registry.register("friends_count_blocked", (player, payload) -> String
                .valueOf(this.repository.getBlocks(player.getUniqueId()).size()));
        registry.register("friends_count_pending", (player, payload) -> String
                .valueOf(this.requestRepository.countTotal(player.getUniqueId())));
        registry.register("friends_count_pending_in", (player, payload) -> String
                .valueOf(this.requestRepository.countIncoming(player.getUniqueId())));
        registry.register("friends_count_pending_out", (player, payload) -> String
                .valueOf(this.requestRepository.countOutgoing(player.getUniqueId())));

        registry.register("friends_limit", (player, payload) -> {
            int limit = this.getFriendLimit(player);
            return limit >= 0 ? NumberUtil.format(limit) : CoreLang.OTHER_INFINITY.text();
        });
        registry.register("friends_slots_free", (player, payload) -> {
            int limit = this.getFriendLimit(player);
            if (limit < 0) {
                return CoreLang.OTHER_INFINITY.text();
            }
            return NumberUtil.format(Math.max(0, limit - this.countFriends(player.getUniqueId())));
        });

        registry.register("friends_favorite_name", (player, payload) -> {
            FriendRecord favorite = this.repository.getFavorite(player.getUniqueId());
            return favorite == null ? CoreLang.OTHER_NONE.text() : favorite.getFriend().name();
        });
        registry.register("friends_favorite_online", (player, payload) -> {
            FriendRecord favorite = this.repository.getFavorite(player.getUniqueId());
            return CoreLang.STATE_YES_NO.get(favorite != null && FriendsRepository.isOnline(favorite.getFriendId()));
        });

        registry.register("friends_list_names", (player, payload) -> FriendsPlaceholders
                .names(this.getFriendRecords(player.getUniqueId())));
        registry.register("friends_list_names_online", (player, payload) -> FriendsPlaceholders
                .names(this.getOnlineFriendRecords(player.getUniqueId())));
        registry.register("friends_list_limited", (player, payload) -> {
            List<FriendRecord> records = this.getFriendRecords(player.getUniqueId());
            if (records.isEmpty()) {
                return CoreLang.OTHER_NONE.text();
            }
            return records.stream().limit(FRIEND_LIST_LIMITED_COUNT)
                    .map(record -> record.getFriend().name())
                    .collect(Collectors.joining(", "));
        });
        registry.register("friends_list_online_first", (player, payload) -> {
            List<FriendRecord> records = this.getFriendRecords(player.getUniqueId());
            return records.isEmpty() ? CoreLang.OTHER_NONE.text() : records.getFirst().getFriend().name();
        });

        // Per-player checks. The payload is the other player's name, resolved online first and
        // then through the user cache, so a scoreboard can ask about an offline friend without a
        // database round trip.
        registry.register("friends_relation", (player, payload) -> this.relationText(player, payload));
        registry.register("friends_is_friend", (player, payload) -> CoreLang.STATE_YES_NO
                .get(this.isFriendByName(player, payload)));
        registry.register("friends_is_blocked", (player, payload) -> CoreLang.STATE_YES_NO
                .get(this.isBlockedByName(player, payload)));
        registry.register("friends_last_seen", (player, payload) -> {
            UUID otherId = this.resolveId(payload);
            return otherId == null ? CoreLang.OTHER_NONE.text() : this.getLastSeenText(otherId);
        });

        // Toggles, matching how the other modules expose theirs.
        registry.register("friends_requests_enabled", (player, payload) -> CoreLang.STATE_ENABLED_DISALBED
                .get(this.isRequestsEnabled(player.getUniqueId())));
        registry.register("friends_login_alerts", (player, payload) -> CoreLang.STATE_ENABLED_DISALBED
                .get(this.isLoginAlertsEnabled(player.getUniqueId())));
        registry.register("friends_teleport_enabled", (player, payload) -> CoreLang.STATE_ENABLED_DISALBED
                .get(this.isFriendTeleportEnabled(player.getUniqueId())));

        registry.register("friends_total", (player, payload) -> String.valueOf(this.repository.size()));
    }

    private String relationText(Player player, String name) {
        UUID otherId = this.resolveId(name);
        if (otherId == null) {
            return FriendRelation.NONE.name();
        }
        return this.getRelation(player.getUniqueId(), otherId).name();
    }

    private boolean isFriendByName(Player player, String name) {
        UUID otherId = this.resolveId(name);
        return otherId != null && this.isFriend(player.getUniqueId(), otherId);
    }

    private boolean isBlockedByName(Player player, String name) {
        UUID otherId = this.resolveId(name);
        return otherId != null && this.isBlocked(player.getUniqueId(), otherId);
    }

    /** Resolves a name against online players, then the user cache. */
    @Nullable
    public UUID resolveId(@Nullable String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        Player online = Utils.getPlayer(trimmed);
        if (online != null) {
            return online.getUniqueId();
        }
        return this.userManager.getRepository().getAssociatedId(trimmed);
    }

    // Relations

    @Override
    public boolean isFriend(@NotNull UUID playerId, @NotNull UUID otherId) {
        return this.repository.isFriend(playerId, otherId);
    }

    @Override
    public boolean isBlocked(@NotNull UUID playerId, @NotNull UUID otherId) {
        return this.repository.isBlocked(playerId, otherId);
    }

    @Override
    public @NotNull FriendRelation getRelation(@NotNull UUID playerId, @NotNull UUID otherId) {
        if (playerId.equals(otherId)) {
            return FriendRelation.SELF;
        }
        if (this.repository.isFriend(playerId, otherId)) {
            return FriendRelation.FRIEND;
        }
        if (this.repository.isBlockedEitherWay(playerId, otherId)) {
            return FriendRelation.BLOCKED;
        }
        if (this.requestRepository.hasIncoming(playerId, otherId)) {
            return FriendRelation.PENDING_INCOMING;
        }
        if (this.requestRepository.getOutgoing(otherId, playerId) != null) {
            return FriendRelation.PENDING_OUTGOING;
        }
        return FriendRelation.NONE;
    }

    // Lists

    public List<FriendRecord> getFriendRecords(UUID playerId) {
        return this.repository.getFriends(playerId);
    }

    public List<FriendRecord> getOnlineFriendRecords(UUID playerId) {
        return this.repository.getOnlineFriends(playerId);
    }

    /** That player's own side of the friendship with the other one, or {@code null}. */
    @Nullable
    public FriendRecord getFriendRecord(UUID playerId, UUID friendId) {
        return this.repository.getFriend(playerId, friendId);
    }

    @Override
    public @NotNull List<FriendHandle> getFriends(@NotNull UUID playerId) {
        return this.getFriendRecords(playerId).stream().map(this::snapshot).toList();
    }

    @Override
    public @NotNull List<FriendHandle> getOnlineFriends(@NotNull UUID playerId) {
        return this.getOnlineFriendRecords(playerId).stream().map(this::snapshot).toList();
    }

    @Override
    public @Nullable FriendHandle getFavoriteFriend(@NotNull UUID playerId) {
        FriendRecord favorite = this.repository.getFavorite(playerId);
        return favorite == null ? null : this.snapshot(favorite);
    }

    @Override
    public @NotNull List<FriendHandle> getBlocked(@NotNull UUID playerId) {
        return this.repository.getBlocks(playerId).stream()
                .map(block -> FriendSnapshots.ofBlocked(block, this.lastSeen(block.getBlockedId())))
                .toList();
    }

    @Override
    public int countFriends(@NotNull UUID playerId) {
        return this.repository.getFriends(playerId).size();
    }

    @Override
    public int countOnlineFriends(@NotNull UUID playerId) {
        return this.getOnlineFriendRecords(playerId).size();
    }

    @Override
    public int getFriendLimit(@NotNull Player player) {
        if (player.hasPermission(FriendsPerms.BYPASS_LIMITS)) {
            return -1;
        }
        return FriendsConfig.AMOUNT_PER_RANK.get().getGreatestOrNegative(player).intValue();
    }

    public int countIncoming(Player player) {
        return this.requestRepository.countIncoming(player.getUniqueId());
    }

    public int countOutgoing(Player player) {
        return this.requestRepository.countOutgoing(player.getUniqueId());
    }

    public int countBlocked(Player player) {
        return this.repository.getBlocks(player.getUniqueId()).size();
    }

    /** The highest number of pending requests that player may keep, from their rank table. */
    public int getRequestLimit(Player player) {
        if (player.hasPermission(FriendsPerms.BYPASS_LIMITS)) {
            return -1;
        }
        return FriendsConfig.REQUESTS_PER_RANK.get().getGreatestOrNegative(player).intValue();
    }

    public boolean canAddMore(Player player) {
        int limit = this.getRequestLimit(player);
        return limit < 0 || this.countFriends(player.getUniqueId()) < limit;
    }

    private FriendHandle snapshot(FriendRecord record) {
        return FriendSnapshots.of(record, this.lastSeen(record.getFriendId()));
    }

    private long lastSeen(UUID playerId) {
        return this.userManager.getRepository().getById(playerId).map(SunUser::getLastOnline).orElse(0L);
    }

    /** Human-readable last-seen text, shared by the menus and the placeholder. */
    public String getLastSeenText(UUID playerId) {
        Player online = Utils.getPlayer(playerId);
        if (online != null) {
            return "Online now";
        }
        return FriendsPlaceholders.formatLastSeen(this.lastSeen(playerId));
    }

    /** Names of every player this module can resolve, for the admin command's suggestions. */
    public Collection<String> getKnownNames() {
        return this.repository.getAll().stream()
                .flatMap(record -> java.util.stream.Stream.of(record.getOwner().name(), record.getFriend().name()))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    // Mutations

    @Override
    public boolean addFriend(@NotNull UUID playerId, @NotNull UUID otherId) {
        UserInfo owner = this.infoOf(playerId);
        UserInfo friend = this.infoOf(otherId);
        if (owner == null || friend == null || owner.id().equals(friend.id())) {
            return false;
        }

        this.addRow(owner, friend);
        this.addRow(friend, owner);
        return true;
    }

    private boolean addRow(UserInfo owner, UserInfo friend) {
        // Another server in the same sync window may already have written this row, in which case
        // this one would be a duplicate rather than an update.
        if (this.dataManager.existsInDatabase(owner.id(), friend.id())) {
            return false;
        }

        FriendRecord record = FriendRecord.create(owner, friend);
        PreFriendChangeEvent pre = new PreFriendChangeEvent(record, false);
        this.plugin.getPluginManager().callEvent(pre);
        if (pre.isCancelled()) {
            return false;
        }

        this.repository.upsert(record);
        this.plugin.runTaskAsync(() -> this.dataManager.insertFriend(record));

        this.plugin.getPluginManager().callEvent(new PlayerFriendChangeEvent(record, true));
        return true;
    }

    @Override
    public boolean removeFriend(@NotNull UUID playerId, @NotNull UUID otherId) {
        List<FriendRecord> rows = this.repository.getBothSides(playerId, otherId);
        if (rows.isEmpty()) {
            return false;
        }

        boolean removed = false;
        for (FriendRecord record : rows) {
            PreFriendChangeEvent pre = new PreFriendChangeEvent(record, true);
            this.plugin.getPluginManager().callEvent(pre);
            if (pre.isCancelled()) {
                continue;
            }

            this.repository.remove(record);
            this.plugin.runTaskAsync(() -> this.dataManager.deleteFriend(record));
            this.plugin.getPluginManager().callEvent(new PlayerFriendChangeEvent(record, false));
            removed = true;
        }
        return removed;
    }

    /** Removal as the command and menu drive it: one message to the player, one to the friend. */
    public boolean removeFriendCommand(Player player, Player friend) {
        if (!this.isFriend(player.getUniqueId(), friend.getUniqueId())) {
            this.sendPrefixed(FriendsLang.REMOVE_ERROR_NOT_FRIEND, player,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(friend)));
            return false;
        }

        boolean removed = this.removeFriend(player.getUniqueId(), friend.getUniqueId());
        if (!removed) {
            return false;
        }

        this.sendPrefixed(FriendsLang.REMOVE_FEEDBACK, player,
                builder -> builder.with(CommonPlaceholders.PLAYER.resolver(friend)));
        this.sendPrefixed(FriendsLang.REMOVE_NOTIFY, friend,
                builder -> builder.with(CommonPlaceholders.PLAYER.resolver(player)));
        return true;
    }

    @Override
    public boolean block(@NotNull UUID playerId, @NotNull UUID otherId) {
        UserInfo blocker = this.infoOf(playerId);
        UserInfo blocked = this.infoOf(otherId);
        if (blocker == null || blocked == null || blocker.id().equals(blocked.id())) {
            return false;
        }

        PreFriendBlockEvent pre = new PreFriendBlockEvent(blocker, blocked, true);
        this.plugin.getPluginManager().callEvent(pre);
        if (pre.isCancelled() || this.repository.isBlocked(playerId, otherId)) {
            return false;
        }

        // A friendship that survives a block is not a block, so this comes first. Both sides are
        // dropped, and both request directions with them.
        this.removeFriend(playerId, otherId);
        this.requestRepository.removeBetween(playerId, otherId);
        this.requestRepository.removeBetween(otherId, playerId);

        BlockRecord block = BlockRecord.create(blocker, blocked);
        this.repository.upsertBlock(block);
        this.plugin.runTaskAsync(() -> this.dataManager.insertBlock(block));

        this.plugin.getPluginManager().callEvent(new PlayerFriendBlockEvent(blocker, blocked, true));
        return true;
    }

    /** Blocking as the command and menu drive it, with the usual feedback. */
    public boolean blockCommand(Player player, Player target) {
        if (player.getUniqueId().equals(target.getUniqueId())) {
            this.sendPrefixed(FriendsLang.BLOCK_ERROR_SELF, player);
            return false;
        }
        if (this.repository.isBlocked(player.getUniqueId(), target.getUniqueId())) {
            this.sendPrefixed(FriendsLang.BLOCK_ERROR_ALREADY, player,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
            return false;
        }

        int limit = FriendsConfig.BLOCK_LIMIT.get();
        if (limit >= 0 && this.countBlocked(player) >= limit
                && !player.hasPermission(FriendsPerms.BYPASS_LIMITS)) {
            this.sendPrefixed(FriendsLang.BLOCK_ERROR_LIMIT, player,
                    builder -> builder.with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(limit)));
            return false;
        }

        boolean wasFriend = this.isFriend(player.getUniqueId(), target.getUniqueId());
        if (!this.block(player.getUniqueId(), target.getUniqueId())) {
            return false;
        }

        this.sendPrefixed(FriendsLang.BLOCK_DONE_FEEDBACK, player,
                builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
        if (wasFriend) {
            this.sendPrefixed(FriendsLang.BLOCK_DONE_NOTIFY, target,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(player)));
        }
        return true;
    }

    @Override
    public boolean unblock(@NotNull UUID playerId, @NotNull UUID otherId) {
        UserInfo blocker = this.infoOf(playerId);
        UserInfo blocked = this.infoOf(otherId);
        if (blocker == null || blocked == null) {
            return false;
        }

        PreFriendBlockEvent pre = new PreFriendBlockEvent(blocker, blocked, false);
        this.plugin.getPluginManager().callEvent(pre);
        if (pre.isCancelled() || !this.repository.isBlocked(playerId, otherId)) {
            return false;
        }

        List<BlockRecord> rows = this.repository.getBlocks(playerId).stream()
                .filter(record -> record.getBlockedId().equals(otherId))
                .toList();
        for (BlockRecord block : rows) {
            this.repository.removeBlock(block);
            this.plugin.runTaskAsync(() -> this.dataManager.deleteBlock(block));
        }

        this.plugin.getPluginManager().callEvent(new PlayerFriendBlockEvent(blocker, blocked, false));
        return !rows.isEmpty();
    }

    /** Unblocking by name, so it works for a player who is offline. */
    public boolean unblockCommand(Player player, String name) {
        UUID otherId = this.resolveId(name);
        if (otherId == null) {
            this.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, player);
            return false;
        }
        if (!this.repository.isBlocked(player.getUniqueId(), otherId)) {
            this.sendPrefixed(FriendsLang.UNBLOCK_ERROR_NOT_BLOCKED, player,
                    builder -> builder.with(CommonPlaceholders.PLAYER_NAME, () -> name));
            return false;
        }
        if (!this.unblock(player.getUniqueId(), otherId)) {
            return false;
        }

        this.sendPrefixed(FriendsLang.UNBLOCK_DONE, player,
                builder -> builder.with(CommonPlaceholders.PLAYER_NAME, () -> name));
        return true;
    }

    /** Pins one friend and unpins whichever was pinned before. */
    public boolean toggleFavorite(Player player, UUID friendId) {
        FriendRecord record = this.repository.getFriend(player.getUniqueId(), friendId);
        if (record == null) {
            return false;
        }

        // Captured before the flip, because setFavorite clears it in place and the old row would
        // otherwise be written back still marked as the favourite.
        FriendRecord previous = this.repository.getFavorite(player.getUniqueId());

        boolean state = !record.isFavorite();
        this.repository.setFavorite(player.getUniqueId(), friendId, state);

        if (previous != null && !previous.getFriendId().equals(friendId)) {
            this.plugin.runTaskAsync(() -> this.dataManager.updateFriend(previous));
        }
        FriendRecord updated = this.repository.getFriend(player.getUniqueId(), friendId);
        if (updated != null) {
            this.plugin.runTaskAsync(() -> this.dataManager.updateFriend(updated));
        }

        this.sendPrefixed(state ? FriendsLang.FAVORITE_SET : FriendsLang.FAVORITE_UNSET, player,
                builder -> builder.with(CommonPlaceholders.PLAYER_NAME, record.getFriend()::name));
        return true;
    }

    @Nullable
    private UserInfo infoOf(UUID playerId) {
        return this.userManager.getRepository().getById(playerId)
                .map(user -> new UserInfo(user.getId(), user.getName()))
                .orElseGet(() -> {
                    Player online = Utils.getPlayer(playerId);
                    return online == null ? null : UserInfo.of(online);
                });
    }

    // Requests

    @Override
    public @NotNull List<FriendRequestHandle> getIncomingRequests(@NotNull UUID playerId) {
        return this.requestRepository.getIncoming(playerId).stream().map(FriendRequest::toHandle).toList();
    }

    @Override
    public @NotNull List<FriendRequestHandle> getOutgoingRequests(@NotNull UUID playerId) {
        return this.requestRepository.getOutgoing(playerId).stream().map(FriendRequest::toHandle).toList();
    }

    @Override
    public @Nullable FriendRequestHandle getIncomingRequest(@NotNull UUID playerId, @NotNull String senderName) {
        FriendRequest request = this.requestRepository.getIncoming(playerId).stream()
                .filter(entry -> entry.isSender(senderName))
                .findFirst()
                .orElse(null);
        return request == null ? null : request.toHandle();
    }

    @Override
    public boolean sendRequest(@NotNull Player sender, @NotNull Player target) {
        if (!this.dataLoaded) {
            this.sendPrefixed(FriendsLang.ERROR_DATA_NOT_LOADED, sender);
            return false;
        }
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            this.sendPrefixed(FriendsLang.REQUEST_ERROR_SELF, sender);
            return false;
        }
        if (this.isFriend(sender.getUniqueId(), target.getUniqueId())) {
            this.sendPrefixed(FriendsLang.REQUEST_ERROR_ALREADY_FRIEND, sender,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
            return false;
        }
        if (this.repository.isBlockedEitherWay(sender.getUniqueId(), target.getUniqueId())) {
            // Deliberately says nothing about which side blocked: the other player does not need
            // to learn that a block exists in order to be refused by it.
            this.sendPrefixed(FriendsLang.REQUEST_ERROR_BLOCKED, sender,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
            return false;
        }
        if (!this.isRequestsEnabled(target.getUniqueId())
                && !sender.hasPermission(FriendsPerms.BYPASS_REQUESTS_DISABLED)) {
            this.sendPrefixed(FriendsLang.REQUEST_ERROR_DISABLED, sender,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
            return false;
        }
        if (this.requestRepository.hasBetween(sender.getUniqueId(), target.getUniqueId())) {
            this.sendPrefixed(FriendsLang.REQUEST_ERROR_PENDING, sender,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
            return false;
        }

        SunUser senderUser = this.userManager.getOrFetch(sender);

        if (!this.checkRequestCooldown(sender, senderUser)) {
            return false;
        }
        if (!this.checkDailyLimits(sender, senderUser, target)) {
            return false;
        }
        if (!this.checkAddLimit(sender)) {
            return false;
        }
        if (!this.checkTargetLimit(sender, target)) {
            return false;
        }

        UserInfo senderInfo = UserInfo.of(sender);
        UserInfo targetInfo = UserInfo.of(target);

        PreFriendRequestEvent pre = new PreFriendRequestEvent(senderInfo, targetInfo);
        this.plugin.getPluginManager().callEvent(pre);
        if (pre.isCancelled()) {
            return false;
        }

        FriendRequest request = FriendRequest.create(senderInfo, targetInfo,
                FriendsConfig.REQUEST_TIMEOUT_SECONDS.get());
        this.requestRepository.add(request);

        FriendRequestCounters.increment(senderUser, target.getUniqueId());

        if (FriendsConfig.NOTIFY_REQUESTS_ENABLED.get()) {
            this.sendPrefixed(FriendsLang.REQUEST_SENT_FEEDBACK, sender,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
            this.sendPrefixed(FriendsLang.REQUEST_SENT_NOTIFY, target,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(sender)));
        }

        this.plugin.getPluginManager().callEvent(new PlayerFriendRequestEvent(senderInfo, targetInfo,
                request.getExpireDate()));
        return true;
    }

    private boolean checkRequestCooldown(Player sender, SunUser senderUser) {
        int cooldown = FriendsConfig.REQUEST_COOLDOWN_SECONDS.get();
        if (cooldown <= 0 || EconomyUtils.hasCooldownBypass(sender, FriendsPerms.BYPASS_COOLDOWN)) {
            return true;
        }

        Long expireDate = senderUser.getCommandCooldown(REQUEST_COOLDOWN_KEY);
        if (expireDate != null && !TimeUtil.isPassed(expireDate)) {
            this.sendPrefixed(FriendsLang.REQUEST_ERROR_COOLDOWN, sender,
                    builder -> builder.with(SLPlaceholders.GENERIC_TIME,
                            () -> TimeFormats.formatDuration(expireDate, TimeFormatType.LITERAL)));
            return false;
        }

        senderUser.setCommandCooldown(REQUEST_COOLDOWN_KEY, TimeUtil.createFutureTimestamp(cooldown));
        return true;
    }

    private boolean checkDailyLimits(Player sender, SunUser senderUser, Player target) {
        if (sender.hasPermission(FriendsPerms.BYPASS_LIMITS)) {
            return true;
        }

        int perDay = FriendsConfig.REQUESTS_PER_DAY.get();
        if (perDay > 0 && FriendRequestCounters.getTotal(senderUser) >= perDay) {
            this.sendPrefixed(FriendsLang.REQUEST_ERROR_DAILY_LIMIT, sender,
                    builder -> builder.with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(perDay)));
            return false;
        }

        int perTarget = FriendsConfig.REQUESTS_PER_TARGET_PER_DAY.get();
        if (perTarget > 0 && FriendRequestCounters.getForTarget(senderUser, target.getUniqueId()) >= perTarget) {
            this.sendPrefixed(FriendsLang.REQUEST_ERROR_TARGET_DAILY_LIMIT, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(perTarget))
                    .with(CommonPlaceholders.PLAYER.resolver(target)));
            return false;
        }

        return true;
    }

    /** Refuses when the sender is already at their own cap. */
    private boolean checkAddLimit(Player player) {
        if (this.canAddMore(player)) {
            return true;
        }
        this.sendPrefixed(FriendsLang.REQUEST_ERROR_LIMIT, player,
                builder -> builder.with(SLPlaceholders.GENERIC_MAX, () -> String.valueOf(this.getRequestLimit(player))));
        return false;
    }

    /**
     * Refuses when the target is already at their cap, which would otherwise only be discovered
     * when they accepted and silently could not.
     */
    private boolean checkTargetLimit(Player sender, Player target) {
        if (this.canAddMore(target)) {
            return true;
        }
        this.sendPrefixed(FriendsLang.REQUEST_ERROR_OTHER_LIMIT, sender,
                builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
        return false;
    }

    @Override
    public boolean acceptRequest(@NotNull Player player, @Nullable String senderName) {
        return this.acceptIncoming(player, senderName);
    }

    /** Accepts an incoming request addressed to that player. */
    public boolean acceptIncoming(Player player, @Nullable String senderName) {
        List<FriendRequest> incoming = this.requestRepository.getIncoming(player.getUniqueId());
        FriendRequest request = senderName == null
                ? (incoming.isEmpty() ? null : incoming.getLast())
                : incoming.stream().filter(entry -> entry.isSender(senderName)).findFirst().orElse(null);

        if (request == null) {
            this.sendPrefixed(FriendsLang.REQUEST_NOTHING_PENDING, player, builder -> builder
                    .with(CommonPlaceholders.PLAYER_NAME, () -> senderName == null ? "" : senderName));
            return false;
        }

        // Re-checked rather than trusted from the send: this player's own list may have grown while
        // the request sat pending, and the sender's may have shrunk by then too.
        if (!this.checkAddLimit(player)) {
            return false;
        }

        this.requestRepository.remove(request);
        this.addFriend(player.getUniqueId(), request.getSenderId());

        this.sendPrefixed(FriendsLang.REQUEST_ACCEPT_FEEDBACK, player,
                builder -> builder.with(CommonPlaceholders.PLAYER_NAME, request::getSenderName));

        Player sender = Utils.getPlayer(request.getSenderId());
        if (sender != null) {
            this.sendPrefixed(FriendsLang.REQUEST_ACCEPT_NOTIFY, sender,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(player)));
            this.notifyStaff(player, sender);
        } else {
            // Accepted while they are offline, which is the common case: the friendship is real,
            // they are told about it when they next log in through the login alert.
            this.sendStoredAccept(request.getSenderId(), request.getSenderName());
        }
        return true;
    }

    @Override
    public boolean declineRequest(@NotNull Player player, @Nullable String senderName) {
        return this.declineIncoming(player, senderName);
    }

    /** Declines an incoming request addressed to that player. */
    public boolean declineIncoming(Player player, @Nullable String senderName) {
        List<FriendRequest> incoming = this.requestRepository.getIncoming(player.getUniqueId());
        FriendRequest request = senderName == null
                ? (incoming.isEmpty() ? null : incoming.getLast())
                : incoming.stream().filter(entry -> entry.isSender(senderName)).findFirst().orElse(null);

        if (request == null) {
            this.sendPrefixed(FriendsLang.REQUEST_NOTHING_PENDING, player, builder -> builder
                    .with(CommonPlaceholders.PLAYER_NAME, () -> senderName == null ? "" : senderName));
            return false;
        }

        this.requestRepository.remove(request);

        this.sendPrefixed(FriendsLang.REQUEST_DECLINE_FEEDBACK, player,
                builder -> builder.with(CommonPlaceholders.PLAYER_NAME, request::getSenderName));

        Player sender = Utils.getPlayer(request.getSenderId());
        if (sender != null) {
            this.sendPrefixed(FriendsLang.REQUEST_DECLINE_NOTIFY, sender,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(player)));
        }
        return true;
    }

    /** Withdraws a request this player has sent. */
    public boolean cancelRequest(Player player, @Nullable String targetName) {
        List<FriendRequest> outgoing = this.requestRepository.getOutgoing(player.getUniqueId());
        FriendRequest request = targetName == null
                ? (outgoing.isEmpty() ? null : outgoing.getLast())
                : outgoing.stream().filter(entry -> entry.isTarget(targetName)).findFirst().orElse(null);

        if (request == null) {
            this.sendPrefixed(FriendsLang.REQUEST_NOTHING_SENT, player, builder -> builder
                    .with(CommonPlaceholders.PLAYER_NAME, () -> targetName == null ? "" : targetName));
            return false;
        }

        this.requestRepository.remove(request);

        this.sendPrefixed(FriendsLang.REQUEST_CANCEL_FEEDBACK, player,
                builder -> builder.with(CommonPlaceholders.PLAYER_NAME, request::getTargetName));

        Player target = Utils.getPlayer(request.getTargetId());
        if (target != null) {
            this.sendPrefixed(FriendsLang.REQUEST_CANCEL_NOTIFY, target,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(player)));
        }
        return true;
    }

    @Override
    public void clearRequests(@NotNull UUID playerId) {
        this.requestRepository.clearFor(playerId);
    }

    /**
     * Tells an offline sender their request was accepted.
     * <p>
     * There is no offline message channel in SunLight, so this is a best-effort send to a cached
     * player object; when that fails the friendship itself is unaffected and they are told by the
     * login alert instead.
     */
    private void sendStoredAccept(UUID playerId, String senderName) {
        Player offline = Utils.getPlayer(playerId);
        if (offline != null && offline.isOnline()) {
            this.sendPrefixed(FriendsLang.REQUEST_ACCEPT_NOTIFY, offline, builder -> builder
                    .with(CommonPlaceholders.PLAYER_NAME, () -> senderName));
        }
    }

    // Toggles

    @Override
    public boolean isRequestsEnabled(@NotNull UUID playerId) {
        return this.userManager.getRepository().getById(playerId)
                .map(FriendToggle.REQUESTS::get)
                .orElse(FriendsProperties.REQUESTS_ENABLED.getDefaultValue());
    }

    @Override
    public void setRequestsEnabled(@NotNull Player player, boolean enabled) {
        FriendToggle.REQUESTS.set(this.userManager.getOrFetch(player), enabled);
    }

    @Override
    public boolean isLoginAlertsEnabled(@NotNull UUID playerId) {
        return this.userManager.getRepository().getById(playerId)
                .map(FriendToggle.LOGIN_ALERTS::get)
                .orElse(FriendsProperties.LOGIN_ALERTS.getDefaultValue());
    }

    @Override
    public void setLoginAlertsEnabled(@NotNull Player player, boolean enabled) {
        FriendToggle.LOGIN_ALERTS.set(this.userManager.getOrFetch(player), enabled);
    }

    @Override
    public boolean isFriendTeleportEnabled(@NotNull UUID playerId) {
        return this.userManager.getRepository().getById(playerId)
                .map(FriendToggle.TELEPORT::get)
                .orElse(FriendsProperties.FRIEND_TELEPORT.getDefaultValue());
    }

    @Override
    public void setFriendTeleportEnabled(@NotNull Player player, boolean enabled) {
        FriendToggle.TELEPORT.set(this.userManager.getOrFetch(player), enabled);
    }

    /** Applies a toggle to a loaded user, used by both the command and the settings menu. */
    public void applyToggle(SunUser user, FriendToggle toggle, boolean value) {
        toggle.set(user, value);
    }

    /**
     * Confirms a toggle to the player, naming the setting so a bare "set to enabled" is never sent
     * on its own.
     */
    public void sendToggle(Player player, FriendToggle toggle, boolean state, @Nullable CommandSender executor) {
        if (executor != null && executor != player) {
            this.sendPrefixed(FriendsLang.TOGGLE_FEEDBACK, executor, builder -> builder
                    .with(CommonPlaceholders.PLAYER.resolver(player))
                    .with(SLPlaceholders.GENERIC_NAME, toggle::getDisplay)
                    .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
        }
        this.sendPrefixed(FriendsLang.TOGGLE_NOTIFY, player, builder -> builder
                .with(SLPlaceholders.GENERIC_NAME, toggle::getDisplay)
                .with(SLPlaceholders.GENERIC_STATE, () -> CoreLang.STATE_ENABLED_DISALBED.get(state)));
    }

    // Jump

    /**
     * Teleports one friend to another.
     * <p>
     * Routed through the teleport manager with the PTP type, so the Warmups module's boss bar
     * applies here exactly as it does to a plain teleport request.
     */
    public boolean jumpToFriend(Player player, String friendName) {
        Player target = Utils.getPlayer(friendName);
        if (target == null) {
            this.sendPrefixed(FriendsLang.JUMP_ERROR_OFFLINE, player,
                    builder -> builder.with(CommonPlaceholders.PLAYER_NAME, () -> friendName));
            return false;
        }
        if (player.getUniqueId().equals(target.getUniqueId())) {
            this.sendPrefixed(CoreLang.COMMAND_EXECUTION_NOT_YOURSELF, player);
            return false;
        }
        if (!this.isFriend(player.getUniqueId(), target.getUniqueId())) {
            this.sendPrefixed(FriendsLang.JUMP_ERROR_NOT_FRIEND, player,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
            return false;
        }
        if (!this.isFriendTeleportEnabled(target.getUniqueId())
                && !FriendsConfig.TELEPORT_BYPASSES_REQUESTS_DISABLED.get()) {
            this.sendPrefixed(FriendsLang.JUMP_ERROR_DISABLED, player,
                    builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
            return false;
        }
        if (!player.hasPermission(FriendsPerms.BYPASS_WORLD_BLACKLIST)
                && FriendsConfig.JUMP_WORLD_BLACKLIST.get().stream()
                        .anyMatch(world -> world.equalsIgnoreCase(player.getWorld().getName()))) {
            this.sendPrefixed(FriendsLang.JUMP_ERROR_WORLD, player);
            return false;
        }

        double cost = FriendsConfig.JUMP_COST.get();
        boolean charge = cost > 0D && EconomyUtils.hasCurrency()
                && !EconomyUtils.hasBypass(player, FriendsPerms.BYPASS_COST);
        if (charge && !EconomyUtils.canAfford(player, cost)) {
            this.sendPrefixed(Lang.COST_ERROR_NOT_ENOUGH_FUNDS, player,
                    builder -> builder.with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost)));
            return false;
        }

        World world = target.getWorld();
        Location destination = target.getLocation();

        TeleportContext context = TeleportContext.builder(this, player, destination)
                .withFlag(TeleportFlag.LOOK_FOR_SURFACE)
                .withFlag(TeleportFlag.AVOID_LAVA)
                .callback(() -> {
                    if (charge) {
                        EconomyUtils.withdraw(player, cost);
                    }
                    this.sendPrefixed(FriendsLang.JUMP_SUCCESS, player,
                            builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
                })
                .build();

        return this.teleportManager.teleport(context, TeleportType.PTP);
    }

    // Messages

    /**
     * Points a player at the private-message command for one friend.
     * <p>
     * A menu cannot ask for free text, so this does not fake a message: it names the command that
     * does the job, which is also the form that still works when the Chat module is disabled.
     */
    public boolean promptMessage(Player player, String friendName) {
        Player target = Utils.getPlayer(friendName);
        if (target == null) {
            this.sendPrefixed(FriendsLang.JUMP_ERROR_OFFLINE, player,
                    builder -> builder.with(CommonPlaceholders.PLAYER_NAME, () -> friendName));
            return false;
        }

        this.sendPrefixed(FriendsLang.MESSAGE_PROMPT, player,
                builder -> builder.with(CommonPlaceholders.PLAYER.resolver(target)));
        return this.plugin.chatProvider().isPresent();
    }

    /** Sends a private message to a friend, resolved by name. */
    public boolean messageFriend(Player player, String friendName, String message) {
        Player target = Utils.getPlayer(friendName);
        if (target == null) {
            this.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, player);
            return false;
        }

        var chat = this.plugin.chatProvider();
        if (chat.isEmpty()) {
            this.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, player);
            return false;
        }

        return chat.get().sendPrivateMessage(player, target, message);
    }

    // Notifications

    /** Tells that player's online friends that they logged in. */
    public void notifyLogin(Player player) {
        this.notifyFriends(player, FriendsLang.NOTIFY_LOGIN);
    }

    /** Tells that player's online friends that they left. */
    public void notifyQuit(Player player) {
        this.notifyFriends(player, FriendsLang.NOTIFY_QUIT);
    }

    private void notifyFriends(Player player, MessageLocale message) {
        if (!FriendsConfig.NOTIFY_LOGIN_ENABLED.get()) {
            return;
        }

        for (FriendRecord record : this.getOnlineFriendRecords(player.getUniqueId())) {
            Player viewer = Utils.getPlayer(record.getOwnerId());
            if (viewer == null || !this.isLoginAlertsEnabled(viewer.getUniqueId())) {
                continue;
            }
            this.sendPrefixed(message, viewer, builder -> builder.with(CommonPlaceholders.PLAYER.resolver(player)));
        }
    }

    /** Tells third parties holding the staff permission that a friendship was made. */
    public void notifyStaff(Player player, Player other) {
        this.notifyWatchers(UserInfo.of(player), UserInfo.of(other));
    }

    /** The admin command's notification, which names the two accounts rather than two players. */
    public void notifyAdminAdd(UserInfo first, UserInfo second) {
        this.notifyWatchers(first, second);
    }

    private void notifyWatchers(UserInfo first, UserInfo second) {
        String permission = FriendsConfig.NOTIFY_STAFF_PERMISSION.get();
        if (permission == null || permission.isBlank()) {
            return;
        }

        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (!candidate.hasPermission(permission)) {
                continue;
            }
            // The two parties are never told about this; only third parties are.
            if (candidate.getUniqueId().equals(first.id()) || candidate.getUniqueId().equals(second.id())) {
                continue;
            }
            this.sendPrefixed(FriendsLang.NOTIFY_ADMIN_ADD, candidate, builder -> builder
                    .with(CommonPlaceholders.PLAYER_NAME, first::name)
                    .with(SLPlaceholders.GENERIC_TARGET, second::name));
        }
    }

    // Menus

    public void openAddDialog(Player player) {
        this.plugin.showDialog(player, FriendsDialogKeys.FRIEND_ADD, "", () -> this.openFriends(player));
    }

    public boolean openFriends(Player player) {
        return this.listMenu.show(player, player.getUniqueId());
    }

    public boolean openFriends(Player player, UUID targetId) {
        return this.listMenu.show(player, targetId);
    }

    public boolean openPending(Player player) {
        return this.pendingMenu.show(player);
    }

    public boolean openBlocked(Player player) {
        return this.blockedMenu.show(player);
    }

    public boolean openSettings(Player player) {
        return this.settingsMenu.show(player);
    }

    public boolean openOptions(Player player, UUID friendId) {
        return this.optionsMenu.show(player, friendId);
    }

    /**
     * Closes the viewer's inventory before an action opens another one, when configured to.
     * <p>
     * Without the close the player is left looking at a menu whose rows the action already
     * invalidated, which reads as a bug even when everything worked.
     */
    public void closeInventoryOnAction(Player player) {
        if (FriendsConfig.COMMAND_MENU_CLOSES_INVENTORY.get()) {
            player.closeInventory();
        }
    }

    /** Formatted count, so every surface reads a number the same way. */
    public static String countText(int value) {
        return NumberUtil.format(value);
    }
}