package su.nightexpress.sunlight.moduleImpl.nick.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerNickChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final String oldNickname;
    private String newNickname;
    private boolean cancelled;

    public PlayerNickChangeEvent(@NotNull Player player, @Nullable String oldNickname, @Nullable String newNickname) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.oldNickname = oldNickname;
        this.newNickname = newNickname;
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

    public @Nullable String getOldNickname() {
        return this.oldNickname;
    }

    public @Nullable String getNewNickname() {
        return this.newNickname;
    }

    public void setNewNickname(@Nullable String newNickname) {
        this.newNickname = newNickname;
    }

    public boolean isClear() {
        return this.newNickname == null;
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
