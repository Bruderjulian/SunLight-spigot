package su.nightexpress.sunlight.moduleImpl.profiles.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.profiles.PlayerProfile;

/**
 * Fired before a profile switch is applied. Cancelling keeps the player on
 * the current profile with their saved state restored.
 */
public class ProfileSwitchEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PlayerProfile from;
    private final PlayerProfile to;
    private boolean cancelled;

    public ProfileSwitchEvent(@NotNull Player player, PlayerProfile from, PlayerProfile to) {
        super(player);
        this.from = from;
        this.to = to;
    }

    public PlayerProfile getFrom() {
        return this.from;
    }

    public PlayerProfile getTo() {
        return this.to;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
