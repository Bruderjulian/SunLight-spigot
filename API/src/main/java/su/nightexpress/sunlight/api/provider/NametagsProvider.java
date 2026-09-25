package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface NametagsProvider {

    boolean hasTag(@NotNull Player player);

    @Nullable String getSelectedTagId(@NotNull Player player);

    boolean selectTag(@NotNull Player player, @Nullable String tagId);
}