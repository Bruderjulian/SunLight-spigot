package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.api.provider.dto.FriendHandle;
import su.nightexpress.sunlight.api.provider.dto.FriendRelation;
import su.nightexpress.sunlight.api.provider.dto.FriendRequestHandle;

import java.util.List;
import java.util.UUID;

/**
 * View of the Friends module: the friendship graph, the pending request queue and the per-player
 * privacy toggles.
 * <p>
 * Obtained via {@code SunLightPlugin#friendsProvider()}, which returns an empty
 * {@link java.util.Optional} when the module is disabled, so callers must treat this as a soft
 * dependency.
 * <p>
 * Friendship rows are stored one per owner, so a mutual friendship is two rows and
 * {@link #isFriend(UUID, UUID)} is true from either side. Removing one side removes both: the
 * module never leaves half a friendship behind.
 * <p>
 * Every type here is a primitive, a JDK type, a Bukkit type, or a type declared in this package,
 * because the API module must not depend on Core.
 */
public interface FriendsProvider {

    // Relations

    /** Whether the two players are friends. True from either side of a mutual friendship. */
    boolean isFriend(@NotNull UUID playerId, @NotNull UUID otherId);

    /** Whether that player has the other one blocked. */
    boolean isBlocked(@NotNull UUID playerId, @NotNull UUID otherId);

    /** The full verdict between two players, used by commands and menus that explain refusals. */
    @NotNull FriendRelation getRelation(@NotNull UUID playerId, @NotNull UUID otherId);

    // Lists

    /** That player's friends, online first, then alphabetically. */
    @NotNull List<FriendHandle> getFriends(@NotNull UUID playerId);

    /** That player's friends that are online right now. */
    @NotNull List<FriendHandle> getOnlineFriends(@NotNull UUID playerId);

    /** That player's pinned friend, or {@code null} when there is none. */
    @Nullable FriendHandle getFavoriteFriend(@NotNull UUID playerId);

    /** That player's blocked players. */
    @NotNull List<FriendHandle> getBlocked(@NotNull UUID playerId);

    /** How many friends that player has. */
    int countFriends(@NotNull UUID playerId);

    /** How many of that player's friends are online. */
    int countOnlineFriends(@NotNull UUID playerId);

    /**
     * The highest number of friends that player may hold, by their configured rank table.
     *
     * @return {@code -1} for unlimited
     */
    int getFriendLimit(@NotNull Player player);

    // Requests

    /** Requests sent to that player, expired ones already removed. */
    @NotNull List<FriendRequestHandle> getIncomingRequests(@NotNull UUID playerId);

    /** Requests that player has sent, expired ones already removed. */
    @NotNull List<FriendRequestHandle> getOutgoingRequests(@NotNull UUID playerId);

    /** The request from a specific sender to that player, or {@code null} when there is none. */
    @Nullable FriendRequestHandle getIncomingRequest(@NotNull UUID playerId, @NotNull String senderName);

    /**
     * Sends a request or invite, firing the module's cancellable event and sending the usual
     * localized feedback to both players.
     *
     * @return {@code false} when they are already friends, either party has blocked the other, the
     * target has requests disabled, a request is already pending, a limit is reached, or the event
     * was cancelled
     */
    boolean sendRequest(@NotNull Player sender, @NotNull Player target);

    /**
     * Accepts a pending request, making the friendship mutual.
     *
     * @param senderName the sender to accept, or {@code null} for the most recent one
     * @return {@code false} when there was nothing to accept
     */
    boolean acceptRequest(@NotNull Player player, @Nullable String senderName);

    /**
     * Declines or cancels a pending request. Sends the usual localized feedback either way.
     *
     * @param senderName the sender to act on, or {@code null} for the most recent one
     * @return {@code false} when there was nothing to act on
     */
    boolean declineRequest(@NotNull Player player, @Nullable String senderName);

    /** Drops every request that player has sent or received. */
    void clearRequests(@NotNull UUID playerId);

    // Mutations

    /**
     * Makes two players friends regardless of any pending request, as the admin command does.
     * Fails silently, so a caller driving this should check {@link #isFriend(UUID, UUID)} first.
     *
     * @return {@code false} when they were already friends
     */
    boolean addFriend(@NotNull UUID playerId, @NotNull UUID otherId);

    /**
     * Removes the friendship from both sides.
     *
     * @return {@code false} when they were not friends
     */
    boolean removeFriend(@NotNull UUID playerId, @NotNull UUID otherId);

    /**
     * Blocks that player, dropping any friendship in both directions first, because a friendship
     * that survives a block is not a block.
     *
     * @return {@code false} when they were already blocked
     */
    boolean block(@NotNull UUID playerId, @NotNull UUID otherId);

    /**
     * Lifts a block.
     *
     * @return {@code false} when they were not blocked
     */
    boolean unblock(@NotNull UUID playerId, @NotNull UUID otherId);

    // Toggles

    /** Whether that player accepts incoming friend requests. */
    boolean isRequestsEnabled(@NotNull UUID playerId);

    /** Records the player's friend-request preference, persisting the choice. */
    void setRequestsEnabled(@NotNull Player player, boolean enabled);

    /** Whether that player wants to be told when a friend logs in or out. */
    boolean isLoginAlertsEnabled(@NotNull UUID playerId);

    /** Records the player's friend-login-alert preference, persisting the choice. */
    void setLoginAlertsEnabled(@NotNull Player player, boolean enabled);

    /** Whether that player lets friends teleport to them. */
    boolean isFriendTeleportEnabled(@NotNull UUID playerId);

    /** Records the player's friend-teleport preference, persisting the choice. */
    void setFriendTeleportEnabled(@NotNull Player player, boolean enabled);
}