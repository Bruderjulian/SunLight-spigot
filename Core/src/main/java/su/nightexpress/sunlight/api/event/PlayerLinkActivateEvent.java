package su.nightexpress.sunlight.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.sunlight.moduleImpl.links.Link;

/**
 * Fired before a link is activated by a player, after view/use permission checks
 * but before cooldown, cost and command execution.
 * <p>
 * Cancellable: cancel to silently (or with your own feedback) block the activation.
 * Clicks, cooldowns, costs and rewards are all skipped when cancelled.
 */
public class PlayerLinkActivateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Link link;
    private final boolean firstClick;

    private boolean cancelled;

    public PlayerLinkActivateEvent(@NotNull Player player, @NotNull Link link, boolean firstClick) {
        this.player = player;
        this.link = link;
        this.firstClick = firstClick;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull Player getPlayer() {
        return this.player;
    }

    public @NotNull Link getLink() {
        return this.link;
    }

    public @NotNull String getLinkId() {
        return this.link.getId();
    }

    public @NotNull String getLinkUrl() {
        return this.link.getUrl();
    }

    /** Whether this is the player's first recorded activation of this link. */
    public boolean isFirstClick() {
        return this.firstClick;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
