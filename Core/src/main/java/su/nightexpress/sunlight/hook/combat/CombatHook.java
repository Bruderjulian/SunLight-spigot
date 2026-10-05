package su.nightexpress.sunlight.hook.combat;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A combat-tag source. Implementations must never throw and must degrade to
 * {@code false} when the backing plugin is missing or changed its API.
 */
public interface CombatHook {

    @NotNull String getId();

    boolean isAvailable();

    boolean isInCombat(@NotNull Player player);
}
