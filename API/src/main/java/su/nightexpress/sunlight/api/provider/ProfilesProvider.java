package su.nightexpress.sunlight.api.provider;

import java.util.List;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Public surface of the profiles feature: one player, many save slots.
 */
public interface ProfilesProvider {

    /** Id of the profile the player is currently playing on. */
    @NotNull String getActiveProfileId(@NotNull Player player);

    /** Display names of all profiles owned by the player. */
    @NotNull List<String> getProfileNames(@NotNull Player player);

    /** How many profiles the player currently owns. */
    int getProfileCount(@NotNull Player player);
}
