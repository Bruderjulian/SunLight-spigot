package su.nightexpress.sunlight.moduleImpl.glow.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import su.nightexpress.sunlight.moduleImpl.glow.GlowEffect;

public class PlayerGlowChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final GlowEffect oldEffect;
    private GlowEffect newEffect;
    private boolean cancelled;

    public PlayerGlowChangeEvent(Player player, GlowEffect oldEffect, GlowEffect newEffect) {
        super(player);
        this.oldEffect = oldEffect;
        this.newEffect = newEffect;
    }

    public GlowEffect getOldEffect() {
        return this.oldEffect;
    }

    public GlowEffect getNewEffect() {
        return this.newEffect;
    }

    public void setNewEffect(GlowEffect newEffect) {
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
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
