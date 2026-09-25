package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GlowProvider {

    boolean hasGlow(@NotNull Player player);

    @Nullable String getGlow(@NotNull Player player);

    /**
     * The colour name the player's active glow frame renders as, e.g. {@code gold}.
     * The nametags module appends it as the final colour token of the nameplate.
     */
    @Nullable String getGlowColor(@NotNull Player player);

    void setGlow(@NotNull Player player, @Nullable String effectId);

    void clearGlow(@NotNull Player player);
}
