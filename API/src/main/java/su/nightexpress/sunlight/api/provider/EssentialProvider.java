package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * View of the player-state the Essential module tracks for itself: god mode and invulnerability.
 * Obtained via {@code SunLightPlugin#essentialProvider()}, which returns an empty
 * {@link java.util.Optional} when the module is disabled, so callers must treat this as a soft
 * dependency.
 * <p>
 * God mode is per-player and persisted; invulnerability is read straight off the Bukkit player and
 * is not persisted. Both are only meaningful while their module features are enabled — see
 * {@link #isGodEnabled()} and {@link #isInvulnerabilityEnabled()}.
 * <p>
 * The module's remaining commands (fly, gamemode, feed, time, teleport and friends) wrap vanilla
 * state that every plugin can already read and set, so they are deliberately not exposed here.
 * Every type here is a primitive, a JDK type or a Bukkit type, because the API module must not
 * depend on Core.
 */
public interface EssentialProvider {

    /** Whether the module's god mode feature is switched on at all. */
    boolean isGodEnabled();

    /** Whether the module's invulnerability feature is switched on at all. */
    boolean isInvulnerabilityEnabled();

    /** Whether the player currently has god mode. Always {@code false} when the feature is off. */
    boolean isGod(@NotNull Player player);

    /**
     * Turns god mode on or off for a player, persisting the choice.
     * <p>
     * The actual invulnerability is applied by the module's own listener, on the next tick — not
     * synchronously by this call. {@link Player#isInvulnerable()} therefore lags this by a tick.
     */
    void setGod(@NotNull Player player, boolean god);

    /** Flips the player's god mode, returning the new state. */
    default boolean toggleGod(@NotNull Player player) {
        final boolean state = !this.isGod(player);
        this.setGod(player, state);
        return state;
    }

    /** Whether the player is currently invulnerable, through this plugin or any other. */
    default boolean isInvulnerable(@NotNull Player player) {
        return player.isInvulnerable();
    }
}