package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.api.provider.dto.WarmupKind;

import java.util.UUID;

/**
 * View of the Warmups module: which players are currently counting down to something, and how long
 * is left. Obtained via {@code SunLightPlugin#warmupsProvider()}, which returns an empty
 * {@link java.util.Optional} when the module is disabled, so callers must treat this as a soft
 * dependency.
 * <p>
 * Warmups live in memory only, so they do not survive a reload, and are dropped when the player
 * disconnects. All state is ticked on the main thread; read it from there too.
 * <p>
 * Every type here is a primitive, a JDK type or a Bukkit type, because the API module must not
 * depend on Core.
 */
public interface WarmupsProvider {

    /** Whether the player has a warmup in progress. */
    boolean hasWarmup(@NotNull Player player);

    /**
     * Seconds left before the player's warmup completes, rounded up. {@code 0} when there is no
     * warmup, which is indistinguishable from one that has just finished — check
     * {@link #hasWarmup(Player)} first.
     */
    int getRemainingSeconds(@NotNull Player player);

    /** The kind of warmup in progress, or {@code null} when there is none. */
    @Nullable WarmupKind getWarmupType(@NotNull Player player);

    /** How many warmups are in progress across all players. */
    int getWarmupCount();

    /** Every player with a warmup in progress. */
    @NotNull UUID[] getWarmingPlayers();

    /**
     * Cancels a player's warmup, running its cancellation logic — refunds, notifications, and so
     * on. A player with no warmup is left alone.
     *
     * @param silent suppress the module's own cancellation messages
     * @return {@code true} when a warmup was actually cancelled
     */
    boolean cancelWarmup(@NotNull Player player, boolean silent);

    /** As {@link #cancelWarmup(Player, boolean)}, sending the usual cancellation messages. */
    default boolean cancelWarmup(@NotNull Player player) {
        return this.cancelWarmup(player, false);
    }
}