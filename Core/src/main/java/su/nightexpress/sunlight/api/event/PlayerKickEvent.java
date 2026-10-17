package su.nightexpress.sunlight.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentReason;

/**
 * Fired before a player is kicked through the Bans module, after every check has passed but before
 * the disconnect.
 * <p>
 * Cancelling keeps the player online; no disconnect screen is shown and no feedback or broadcast is
 * sent to the executor or the server.
 * <p>
 * This is distinct from a ban: no punishment record is created here. For bans see
 * {@link PrePunishEvent}.
 */
public class PlayerKickEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player victim;
    private final PunishmentReason reason;
    private final CommandSender executor;
    private final boolean silent;
    private boolean cancelled;

    public PlayerKickEvent(@NotNull Player victim,
            @NotNull PunishmentReason reason,
            @NotNull CommandSender executor,
            boolean silent
    ) {
        this.victim = victim;
        this.reason = reason;
        this.executor = executor;
        this.silent = silent;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull Player getVictim() {
        return this.victim;
    }

    public @NotNull PunishmentReason getReason() {
        return this.reason;
    }

    public @NotNull CommandSender getExecutor() {
        return this.executor;
    }

    public boolean isSilent() {
        return this.silent;
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