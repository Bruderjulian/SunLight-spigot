package su.nightexpress.sunlight.moduleImpl.bans.event;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentReason;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentType;

/**
 * Fired after a player punishment has been issued and the victim notified. Not fired for
 * failed attempts: self-punishment, immunity, insufficient rank, duration limit violations and
 * connected-IP bans all return before this point.
 * <p>
 * Extends {@link Event} rather than {@code PlayerEvent} because punishments are routinely issued
 * against offline players, in which case {@link #getVictim()} is {@code null}. Use
 * {@link #getVictimInfo()} for the identity, which is always present.
 */
public class PlayerPunishEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable Player victim;
    private final UserInfo victimInfo;
    private final PunishmentType type;
    private final PunishmentReason reason;
    private final CommandSender executor;
    private final boolean silent;

    public PlayerPunishEvent(@Nullable Player victim,
            @NotNull UserInfo victimInfo,
            @NotNull PunishmentType type,
            @NotNull PunishmentReason reason,
            @NotNull CommandSender executor,
            boolean silent
    ) {
        this.victim = victim;
        this.victimInfo = victimInfo;
        this.type = type;
        this.reason = reason;
        this.executor = executor;
        this.silent = silent;
    }

    /** The punished player, or {@code null} when they were offline. */
    public @Nullable Player getVictim() {
        return this.victim;
    }

    public @NotNull UserInfo getVictimInfo() {
        return this.victimInfo;
    }

    public @NotNull PunishmentType getType() {
        return this.type;
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
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
