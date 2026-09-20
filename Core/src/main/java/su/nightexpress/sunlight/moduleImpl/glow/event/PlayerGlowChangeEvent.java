package su.nightexpress.sunlight.moduleImpl.glow.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerGlowChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String oldEffect;
    private String newEffect;
    private boolean cancelled;

    public PlayerGlowChangeEvent(@NotNull Player player, @Nullable String oldEffect, @Nullable String newEffect) {
        super(player);
        this.oldEffect = oldEffect;
        this.newEffect = newEffect;
    }

    public @Nullable String getOldEffect() {
        return this.oldEffect;
    }

    public @Nullable String getNewEffect() {
        return this.newEffect;
    }

    public void setNewEffect(@Nullable String newEffect) {
        this.newEffect = newEffect;
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
