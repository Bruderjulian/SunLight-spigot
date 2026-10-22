package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * View of the Nerf Phantoms module's per-player opt-out. Obtained via
 * {@code SunLightPlugin#phantomsProvider()}, which returns an empty {@link java.util.Optional} when
 * the module is disabled, so callers must treat this as a soft dependency.
 * <p>
 * The module's other knobs — spawn disabling, damage and health modifiers — are server-wide config
 * the module applies internally, with nothing per-player to query.
 */
public interface PhantomsProvider {

    /**
     * Whether the player has opted out of phantom spawns. A player who has, keeps their rest timer
     * topped up and never gets a phantom attached to them.
     */
    boolean isPhantomSpawnPrevented(@NotNull Player player);

    /**
     * Records the player's phantom opt-out, persisting the choice. Takes effect on the next rest
     * tick rather than immediately.
     */
    void setPhantomSpawnPrevented(@NotNull Player player, boolean prevented);
}