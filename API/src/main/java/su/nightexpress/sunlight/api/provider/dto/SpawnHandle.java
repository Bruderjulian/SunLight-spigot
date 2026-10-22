package su.nightexpress.sunlight.api.provider.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The subset of a spawn that is safe to hand to another plugin.
 * <p>
 * A record of primitives rather than the module's own model, because the API module must not
 * depend on Core. It carries no {@code Location}: a spawn's position is only meaningful together
 * with its world, and the world may be unloaded at the time it is read.
 *
 * @param id                 the spawn's id, always lowercase
 * @param name               the display name
 * @param worldName          the name of the world the spawn lives in, or {@code null} if that world is not loaded
 * @param priority           higher wins when several spawns match a player
 * @param active             whether the module currently teleports players here
 * @param permissionRequired whether the player needs a permission to use it
 */
public record SpawnHandle(@NotNull String id,
                          @NotNull String name,
                          @Nullable String worldName,
                          int priority,
                          boolean active,
                          boolean permissionRequired
) {
}