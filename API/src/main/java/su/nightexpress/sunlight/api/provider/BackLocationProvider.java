package su.nightexpress.sunlight.api.provider;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.api.provider.dto.BackLocationType;
import su.nightexpress.sunlight.api.provider.dto.StoredLocationHandle;

import java.util.List;
import java.util.UUID;

/**
 * View of the Back Location module: the cached previous/death locations, and teleporting back to
 * them. Obtained via {@code SunLightPlugin#backLocationProvider()}, which returns an empty
 * {@link java.util.Optional} when the module is disabled, so callers must treat this as a soft
 * dependency.
 * <p>
 * The cache lives in memory only, so entries do not survive a reload.
 * <p>
 * Every type here is a primitive, a JDK type, a Bukkit type, or a type declared in this package,
 * because the API module must not depend on Core.
 */
public interface BackLocationProvider {

    /**
     * Whether that slot is excluded for the given world, meaning the module refuses to cache
     * locations there and discards anything already held for it.
     */
    boolean isDisabledWorld(@NotNull String worldName, @NotNull BackLocationType type);

    /** Whether that slot is excluded for the given teleport cause. */
    boolean isDisabledCause(@NotNull PlayerTeleportEvent.TeleportCause cause);

    /** How long, in seconds, an entry in that slot stays valid. */
    int getDuration(@NotNull BackLocationType type);

    /**
     * The cached entry for that slot, or {@code null} when there is none, it has expired, or its
     * world is gone. Resolving this drops the entry when it is expired.
     */
    @Nullable StoredLocationHandle getLocation(@NotNull UUID playerId, @NotNull BackLocationType type);

    /** Whether {@link #getLocation(UUID, BackLocationType)} would return an entry for that slot. */
    default boolean hasLocation(@NotNull UUID playerId, @NotNull BackLocationType type) {
        return this.getLocation(playerId, type) != null;
    }

    /** Every cached entry for that player, expired ones already dropped. */
    @NotNull List<StoredLocationHandle> getLocations(@NotNull UUID playerId);

    /**
     * Caches a location in that slot, resetting its expiry. Silently does nothing when the world
     * is not loaded.
     */
    void saveLocation(@NotNull UUID playerId, @NotNull Location location, @NotNull BackLocationType type);

    /** As {@link #saveLocation(UUID, Location, BackLocationType)}, keyed by the online player. */
    default void saveLocation(@NotNull Player player, @NotNull Location location, @NotNull BackLocationType type) {
        this.saveLocation(player.getUniqueId(), location, type);
    }

    /**
     * Teleports a player back to a cached location, applying the module's cost rules and sending
     * the usual command feedback.
     *
     * @param silent suppress the module's own messages, for callers that report the outcome
     *              themselves
     * @return {@code false} when nothing is cached there, the cost could not be paid, or the
     * teleport was refused
     */
    boolean teleportToLocation(@NotNull Player player, @NotNull BackLocationType type, boolean silent);

    /** As {@link #teleportToLocation(Player, BackLocationType, boolean)}, sending the usual feedback. */
    default boolean teleportToLocation(@NotNull Player player, @NotNull BackLocationType type) {
        return this.teleportToLocation(player, type, false);
    }
}