package su.nightexpress.sunlight.api.provider;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.api.provider.dto.PlayerWarpHandle;

import java.util.List;

/**
 * View of the Player Warps module: player-created warps, and teleporting to them. Obtained via
 * {@code SunLightPlugin#playerWarpsProvider()}, which returns an empty {@link java.util.Optional}
 * when the module is disabled, so callers must treat this as a soft dependency.
 * <p>
 * Warps are addressed by id; ids are matched case-insensitively. The module's own menus are not
 * exposed here — use {@link #teleportToWarp} rather than driving its dialogs from outside.
 * <p>
 * Every type here is a primitive, a JDK type, a Bukkit type or a record declared in this package,
 * because the API module must not depend on Core.
 */
public interface PlayerWarpsProvider {

    /** The warp with that id, or {@code null} when there is none. */
    @Nullable PlayerWarpHandle getWarp(@NotNull String id);

    /** Every warp the given player is allowed to see, own or not, as detached snapshots. */
    @NotNull List<PlayerWarpHandle> listAvailableWarps(@NotNull Player player);

    /** Every warp owned by the given player, as detached snapshots. */
    @NotNull List<PlayerWarpHandle> listOwnedWarps(@NotNull Player player);

    /** Whether the given player owns a warp of that id. */
    boolean ownsWarp(@NotNull Player player, @NotNull String warpId);

    /** How many more warps the given player may create, given their rank. Never negative. */
    int getAllowedWarpsAmount(@NotNull Player player);

    /** How many warps the given player currently owns. */
    int getOwnedWarpsAmount(@NotNull Player player);

    /**
     * Creates a warp at the player's location, sending the usual command feedback.
     *
     * @param force skip the per-player warp limit and the economy cost
     * @return {@code false} when the name is taken, the limit is reached, the cost could not be
     * paid, or a permission or region check refused it
     */
    boolean createWarp(@NotNull Player player, @NotNull String name, boolean force);

    /**
     * Deletes a warp.
     *
     * @param force delete warps the sender does not own
     * @return {@code false} when there is no such warp, or the sender lacks permission
     */
    boolean deleteWarp(@NotNull CommandSender sender, @NotNull String warpId, boolean force);

    /**
     * Teleports a player to a warp, applying the module's warmup, cost and permission rules and
     * sending the usual command feedback.
     *
     * @param force skip permissions, the cost and the warmup
     * @return {@code false} when there is no such warp, the player may not use it, cannot afford
     * it, or the teleport was refused
     */
    boolean teleportToWarp(@NotNull Player player, @NotNull String warpId, boolean force);
}