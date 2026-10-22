package su.nightexpress.sunlight.moduleImpl.nametags.hook;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Supplies the permission-group names used to pick a {@code RankDefinition}.
 * <p>
 * Resolution is asynchronous because permission plugins may need to load a user from
 * their storage first; implementations must never block the main thread.
 */
public interface GroupSource {

    /** Short id used for logging and config toggles. */
    @NotNull String getId();

    boolean isAvailable();

    /**
     * Resolves every group the player belongs to, including inherited parents.
     *
     * @return a future that never completes exceptionally; an empty set means "no groups"
     */
    @NotNull CompletableFuture<Set<String>> resolveGroups(@NotNull Player player);

    /** Registers whatever change notifications this source can offer. */
    default void registerListeners(@NotNull Runnable onChange) {
    }

    default void shutdown() {
    }
}
