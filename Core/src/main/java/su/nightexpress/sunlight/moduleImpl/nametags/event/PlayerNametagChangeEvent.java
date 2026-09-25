package su.nightexpress.sunlight.moduleImpl.nametags.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerNametagChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final String oldTagId;
    private String newTagId;
    private boolean cancelled;

    public PlayerNametagChangeEvent(@NotNull Player player, @Nullable String oldTagId, @Nullable String newTagId) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.oldTagId = oldTagId;
        this.newTagId = newTagId;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public @NotNull Player getPlayer() {
        return this.player;
    }

    public @Nullable String getOldTagId() {
        return this.oldTagId;
    }

    public @Nullable String getNewTagId() {
        return this.newTagId;
    }

    public void setNewTagId(@Nullable String newTagId) {
        this.newTagId = newTagId;
    }

    public boolean isClear() {
        return this.newTagId == null;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}