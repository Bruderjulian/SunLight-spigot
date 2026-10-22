package su.nightexpress.sunlight.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentReason;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentType;
import su.nightexpress.sunlight.moduleImpl.bans.time.BanTime;

/**
 * Fired before a player punishment (ban, mute or warn) is applied, after every built-in check has
 * passed but before anything is mutated.
 * <p>
 * Cancelling aborts the punishment: no record is created, any currently active punishment of the
 * same type is <i>not</i> deactivated, and no kick, notification, broadcast or auto-command runs.
 * {@link #getVictim()} is {@code null} when the target is offline — use {@link #getVictimInfo()},
 * which is always present.
 * <p>
 * The post-state counterpart is {@code moduleImpl.bans.event.PlayerPunishEvent}, which fires only
 * once the punishment has actually been recorded.
 */
public class PrePunishEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UserInfo victimInfo;
    private final PunishmentType type;
    private final PunishmentReason reason;
    private final BanTime time;
    private final CommandSender executor;
    private final boolean silent;
    private boolean cancelled;

    public PrePunishEvent(@NotNull UserInfo victimInfo,
            @NotNull PunishmentType type,
            @NotNull PunishmentReason reason,
            @NotNull BanTime time,
            @NotNull CommandSender executor,
            boolean silent
    ) {
        this.victimInfo = victimInfo;
        this.type = type;
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

    /** Always the identity of the punished player, whether online or not. */
    public @NotNull UserInfo getVictimInfo() {
        return this.victimInfo;
    }

    public @NotNull PunishmentType getType() {
        return this.type;
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