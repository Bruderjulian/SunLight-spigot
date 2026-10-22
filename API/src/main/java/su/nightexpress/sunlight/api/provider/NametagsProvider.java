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
    // Visibility
    // -----------------------------------------------------

    /**
     * Whether the player currently renders their full nametag. When false the plate is
     * reduced to the bare player name, whatever the individual parts say.
     */
    boolean isNameplateVisible(@NotNull Player player);

    boolean isRankVisible(@NotNull Player player);

    boolean isTagVisible(@NotNull Player player);

    boolean isTeamVisible(@NotNull Player player);

    /** Replaces the player's visibility, recomputing their nameplate. */
    void setNameplateVisible(@NotNull Player player, boolean visible);

    // -----------------------------------------------------
    // Refresh
    // -----------------------------------------------------

    /** Rebuilds one player's nameplate and pushes it to TAB. */
    void recompute(@NotNull Player player);

    void recomputeAll();
}
