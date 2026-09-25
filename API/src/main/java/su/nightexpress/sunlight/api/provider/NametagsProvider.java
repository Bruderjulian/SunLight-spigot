package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public surface of the nametags feature.
 */
public interface NametagsProvider {

    boolean hasTag(@NotNull Player player);

    @Nullable String getSelectedTagId(@NotNull Player player);

    boolean selectTag(@NotNull Player player, @Nullable String tagId);

    // -----------------------------------------------------
    // Profiles and ranks
    // -----------------------------------------------------

    /** The profile the player has explicitly chosen, or an empty string. */
    @Nullable String getSelectedProfileId(@NotNull Player player);

    boolean selectProfile(@NotNull Player player, @Nullable String profileId);

    /** The rank the player has explicitly chosen, or an empty string. */
    @Nullable String getSelectedRankId(@NotNull Player player);

    boolean selectRank(@NotNull Player player, @Nullable String rankId);

    // -----------------------------------------------------
    // Access and entitlements
    // -----------------------------------------------------

    /** Whether the player may currently use the tag, grants included. */
    boolean hasAccess(@NotNull Player player, @NotNull String tagId);

    /** Grants the tag permanently or for one subscription period. */
    boolean grantTag(@NotNull Player player, @NotNull String tagId);

    boolean revokeTag(@NotNull Player player, @NotNull String tagId);

    // -----------------------------------------------------
    // Refresh
    // -----------------------------------------------------

    /** Rebuilds one player's nameplate and pushes it to TAB. */
    void recompute(@NotNull Player player);

    void recomputeAll();
}
