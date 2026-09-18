package su.nightexpress.sunlight.module.chat.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerPrivateMessageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player sender;
    private final Player target;
    private final String message;

    private boolean cancelled;

    public PlayerPrivateMessageEvent(Player sender, Player target, String message) {
        this.sender = sender;
        this.target = target;
        this.message = message;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Player getSender() {
        return this.sender;
    }

    public Player getTarget() {
        return this.target;
    }

    public String getMessage() {
        return this.message;
    }
}
