package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GlowProvider {

    boolean hasGlow(@NotNull Player player);

    @Nullable String getGlow(@NotNull Player player);

    void setGlow(@NotNull Player player, @Nullable String effectId);

    void clearGlow(@NotNull Player player);
}
