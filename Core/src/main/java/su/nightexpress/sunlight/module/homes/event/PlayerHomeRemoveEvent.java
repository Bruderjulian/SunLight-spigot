package su.nightexpress.sunlight.module.homes.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.sunlight.module.homes.impl.Home;

public class PlayerHomeRemoveEvent extends Event {

    public static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final Home home;

    public PlayerHomeRemoveEvent(Player player, Home home) {
        this.player = player;
        this.home = home;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public Player getPlayer() {
        return player;
    }

    public Home getHome() {
        return home;
    }
}
