package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.api.provider.dto.PtpMode;
import su.nightexpress.sunlight.api.provider.dto.TeleportRequestHandle;

import java.util.List;
import java.util.UUID;

/**
 * View of the PTP module's teleport request queue. Obtained via
 * {@code SunLightPlugin#ptpProvider()}, which returns an empty {@link java.util.Optional} when the
 * module is disabled, so callers must treat this as a soft dependency.
 * <p>
 * The queue lives in memory only, so requests do not survive a reload. Reads drop expired entries
 * as a side effect, which is why a list taken from here may be shorter than it was a moment ago.
 * <p>
 * Every type here is a primitive, a JDK type, a Bukkit type, or a type declared in this package,
 * because the API module must not depend on Core.
 */
public interface PtpProvider {

    /** Whether the given player currently accepts teleport requests. */
    boolean isRequestsEnabled(@NotNull Player player);

    /** Requests addressed to the given player, expired ones already removed. */
    @NotNull List<TeleportRequestHandle> getPendingRequests(@NotNull UUID playerId);

    /** The most recently received request for that player, or {@code null} when there is none. */
    @Nullable TeleportRequestHandle getLatestRequest(@NotNull UUID playerId);

    /**
     * The request from a specific sender to that player, or {@code null} when there is none or the
     * one there has expired. The name is resolved against currently online players.
     */
    @Nullable TeleportRequestHandle getRequest(@NotNull UUID playerId, @NotNull String senderName);

    /**
     * Sends a request or invite, firing the module's cancellable request event and sending the
     * usual localized feedback to both players.
     *
     * @return {@code false} when the target has requests disabled, an unexpired request from this
     * sender is already pending, or the event was cancelled
     */
    boolean sendRequest(@NotNull Player sender, @NotNull Player target, @NotNull PtpMode mode);

    /**
     * Accepts a pending request and teleports the appropriate party, charging the configured cost.
     * Sends the usual localized feedback either way.
     *
     * @param senderName the sender to accept, or {@code null} for the most recent request
     * @return {@code false} when there was nothing to accept, the sender is offline, the cost
     * could not be paid, or the teleport was refused
     */
    boolean acceptRequest(@NotNull Player player, @Nullable String senderName);

    /**
     * Declines a pending request. Sends the usual localized feedback either way.
     *
     * @param senderName the sender to decline, or {@code null} for the most recent request
     * @return {@code false} when there was nothing to decline or the sender is offline
     */
    boolean declineRequest(@NotNull Player player, @Nullable String senderName);

    /** Drops every pending request addressed to that player. */
    void clearRequests(@NotNull UUID playerId);
}