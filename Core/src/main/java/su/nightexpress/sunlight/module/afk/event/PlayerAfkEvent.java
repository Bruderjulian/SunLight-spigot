package su.nightexpress.sunlight.module.afk.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerAfkEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final boolean state;

    public PlayerAfkEvent(Player player, boolean state) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.state = state;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public Player getPlayer() {
        return this.player;
    }

    public boolean getState() {
        return this.state;
    }
}
