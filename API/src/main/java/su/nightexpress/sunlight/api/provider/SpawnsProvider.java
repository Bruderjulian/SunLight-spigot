package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.api.provider.dto.SpawnHandle;

import java.util.List;

/**
 * View of the Spawns module: its spawn registry, and teleporting players to it. Obtained via
 * {@code SunLightPlugin#spawnsProvider()}, which returns an empty {@link java.util.Optional} when
 * the module is disabled, so callers must treat this as a soft dependency.
 * <p>
 * Every type here is a primitive, a JDK type, a Bukkit type or a record declared in this package,
 * because the API module must not depend on Core. Spawns are addressed by id; ids are matched
 * case-insensitively.
 */
public interface SpawnsProvider {

    /** Ids of every loaded spawn, in no particular order. */
    @NotNull List<String> getSpawnIds();

    /**
     * The spawn with that id, or {@code null} when there is none.
     * <p>
     * Named {@code find} rather than {@code get} because the module's own model-returning lookup
     * keeps that name; this one hands back a detached snapshot, not a live spawn.
     */
    @Nullable SpawnHandle findSpawn(@NotNull String id);

    /** Every loaded spawn, in no particular order. */
    @NotNull List<SpawnHandle> listSpawns();

    /** The configured default spawn, or {@code null} when the configured id resolves to nothing. */
    @Nullable SpawnHandle getDefaultSpawnInfo();

    /**
     * The spawn a returning player would be sent to on join: the highest-priority spawn they are
     * allowed to use. {@code null} when none matches.
     */
    @Nullable SpawnHandle getLoginSpawnInfo(@NotNull Player player);

    /**
     * The spawn a player would be respawned at: the highest-priority spawn they are allowed to use
     * for respawn. {@code null} when none matches.
     */
    @Nullable SpawnHandle getDeathSpawnInfo(@NotNull Player player);

    /** Whether the player may teleport to that spawn, ignoring whether it is active. */
    boolean hasSpawnPermission(@NotNull Player player, @NotNull String spawnId);

    /**
     * Creates a spawn at the player's current location, or moves the existing spawn of that id
     * there. The id is normalised the same way as the {@code /spawn set} command, which is not
     * plain lowercasing. Sends the usual command feedback to the player.
     *
     * @return {@code false} only when the player has no usable location
     */
    boolean createSpawn(@NotNull Player player, @NotNull String id);

    /**
     * Deletes a spawn and its backing file.
     *
     * @return {@code false} when no spawn with that id exists, or the file could not be removed
     */
    boolean deleteSpawn(@NotNull String id);

    /**
     * Teleports a player to a spawn, applying the module's warmup, cost and permission rules and
     * sending the usual command feedback.
     *
     * @param forced skip permissions, the cost and the warmup
     * @return {@code false} when the spawn is inactive, the player lacks permission, cannot afford
     * the cost, or the teleport was refused
     */
    boolean teleport(@NotNull Player player, @NotNull String id, boolean forced);

    /** As {@link #teleport(Player, String, boolean)}, but with {@code forced} defaulting to {@code true}. */
    default boolean teleport(@NotNull Player player, @NotNull String id) {
        return this.teleport(player, id, true);
    }
}