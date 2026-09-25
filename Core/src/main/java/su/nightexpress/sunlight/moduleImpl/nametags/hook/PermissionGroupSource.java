package su.nightexpress.sunlight.moduleImpl.nametags.hook;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.Players;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Fallback group source that reads the permission groups the server itself already
 * computed for the player. Always available, so the plugin keeps working without
 * LuckPerms installed.
 */
public class PermissionGroupSource implements GroupSource {

    @Override
    public @NotNull String getId() {
        return "permission-groups";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public @NotNull CompletableFuture<Set<String>> resolveGroups(@NotNull Player player) {
        Set<String> groups = new LinkedHashSet<>(Players.getInheritanceGroups(player));
        return CompletableFuture.completedFuture(groups);
    }
}
