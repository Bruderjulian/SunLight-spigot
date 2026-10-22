package su.nightexpress.sunlight.api.provider;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.api.provider.dto.LastRtpHandle;

/**
 * View of the RTP module's random-teleport engine. Obtained via
 * {@code SunLightPlugin#rtpProvider()}, which returns an empty {@link java.util.Optional} when the
 * module is disabled, so callers must treat this as a soft dependency.
 * <p>
 * Teleports here are asynchronous: a successful return means the lookup started, not that the
 * player has moved.
 * <p>
 * Every type here is a primitive, a JDK type, a Bukkit type or a record declared in this package,
 * because the API module must not depend on Core.
 */
public interface RtpProvider {

    /**
     * Throws a player to a random valid location.
     *
     * @param world the world to search, or {@code null} for the player's current world; the module
     *              falls back to its configured fallback world when that one has no lookup range
     * @return {@code false} when the world has no usable lookup range or the player lacks access
     * to it. {@code true} means the lookup started, and may still fail afterwards
     */
    boolean teleportToRandomPlace(@NotNull Player player, @Nullable World world);

    /** As {@link #teleportToRandomPlace(Player, World)}, searching the player's current world. */
    default boolean teleportToRandomPlace(@NotNull Player player) {
        return this.teleportToRandomPlace(player, null);
    }

    /** The player's most recent completed random teleport, or {@code null} when they have not done one. */
    @Nullable LastRtpHandle getLastRtp(@NotNull Player player);

    /**
     * Whether a location sits inside a region the module refuses to drop players into.
     * Always {@code false} when protection checks are disabled in the module settings.
     */
    boolean isProtected(@NotNull org.bukkit.Location location);
}