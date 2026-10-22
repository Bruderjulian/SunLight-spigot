package su.nightexpress.sunlight.moduleImpl.nametags.hook;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Supplies an optional team contribution to the nameplate.
 */
public interface TeamSource {

    @NotNull String getId();

    boolean isAvailable();

    /** @return a future that never completes exceptionally, empty result when teamless */
    @NotNull CompletableFuture<TeamInfo> resolveTeam(@NotNull Player player);

    default void registerListeners(@NotNull Runnable onChange) {
    }

    default void shutdown() {
    }
}
