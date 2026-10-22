package su.nightexpress.sunlight.moduleImpl.nametags.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired before a player's selected tag changes, from any source: the GUI, a command, or
 * the API. Cancelling keeps the previous selection.
 */
public class PlayerTagChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String oldTag;
    private String newTag;
    private boolean cancelled;

    public PlayerTagChangeEvent(@NotNull Player player, @Nullable String oldTag, @Nullable String newTag) {
        super(player);
        this.oldTag = oldTag;
        this.newTag = newTag;
    }

    public @Nullable String getOldTag() {
        return this.oldTag;
    }

    public @Nullable String getNewTag() {
        return this.newTag;
    }

    public void setNewTag(@Nullable String newTag) {
        this.newTag = newTag;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
