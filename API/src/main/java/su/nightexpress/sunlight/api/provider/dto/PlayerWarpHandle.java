package su.nightexpress.sunlight.api.provider.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A player-owned warp, as handed out by {@link su.nightexpress.sunlight.api.provider.PlayerWarpsProvider}.
 * <p>
 * A record of JDK types rather than the module's own model, because the API module must not depend
 * on Core. The world is carried by name rather than as a {@code Location}, because a warp's world
 * is routinely unloaded.
 *
 * @param id                the warp's id, always lowercase
 * @param name              the display name
 * @param ownerId           the owning player's id
 * @param ownerName         the owning player's last known name
 * @param worldName         the world the warp sits in, or {@code null} when that world is not loaded
 * @param categoryId        the category the warp is filed under, or {@code null} for none
 * @param description       the raw description lines, placeholders not yet applied
 * @param price             the cost of a paid teleport, {@code 0} when the warp is free
 * @param featured          whether the warp currently occupies a featured slot
 * @param totalVisits       lifetime teleport count
 * @param creationTimestamp epoch milliseconds of creation
 */
public record PlayerWarpHandle(@NotNull String id,
                                @NotNull String name,
                                @Nullable UUID ownerId,
                                @NotNull String ownerName,
                                @Nullable String worldName,
                                @Nullable String categoryId,
                                @NotNull List<String> description,
                                double price,
                                boolean featured,
                                long totalVisits,
                                long creationTimestamp
) {
}