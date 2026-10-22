package su.nightexpress.sunlight.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentReason;
import su.nightexpress.sunlight.moduleImpl.bans.time.BanTime;

import java.net.InetAddress;

/**
 * Fired before an IP ban is applied, after the immunity check but before any record is created.
 * <p>
 * Cancelling aborts the ban: nothing is stored and the address is not notified.
 * <p>
 * This is the IP counterpart of {@link PrePunishEvent}, which covers ban/mute/warn issued against a
 * player. There is deliberately no post-state variant here — an IP ban has no meaningful "after"
 * state a listener could observe beyond the ban list itself.
 */
public class InetPunishEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final InetAddress address;
    private final PunishmentReason reason;
    private final BanTime time;
    private final CommandSender executor;
    private final boolean silent;
    private boolean cancelled;

    public InetPunishEvent(@NotNull InetAddress address,
            @NotNull PunishmentReason reason,
            @NotNull BanTime time,
            @NotNull CommandSender executor,
            boolean silent
    ) {
        this.address = address;
        this.reason = reason;
        this.time = time;
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

    public @NotNull InetAddress getAddress() {
        return this.address;
    }

    public @NotNull PunishmentReason getReason() {
        return this.reason;
    }

    public @NotNull BanTime getTime() {
        return this.time;
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