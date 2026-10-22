package su.nightexpress.sunlight.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PlayerPunishment;
import su.nightexpress.sunlight.moduleImpl.bans.punishment.PunishmentType;

import java.util.Collection;

/**
 * Fired after a player's active punishments have been deactivated by a pardon.
 * <p>
 * Not cancellable: a pardon has no vetoable pre-state hook, because by the time any observer could
 * intervene the records are already deactivated. The pardoned records themselves are supplied, which
 * is what an audit or logging plugin actually needs.
 * <p>
 * A single pardon may deactivate more than one record at once — all active punishments of
 * {@link #getType()} are lifted together. IP pardons are not covered by this event.
 */
public class PlayerPardonEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UserInfo userInfo;
    private final PunishmentType type;
    private final Collection<PlayerPunishment> punishments;
    private final CommandSender executor;
    private final boolean silent;

    /**
     * @param userInfo     the pardoned player, whether online or not
     * @param type         the punishment type that was lifted
     * @param punishments  every record deactivated by this pardon, never empty
     * @param executor     who issued the pardon
     * @param silent       whether notifications and the broadcast were suppressed
     */
    public PlayerPardonEvent(@NotNull UserInfo userInfo,
            @NotNull PunishmentType type,
            @NotNull Collection<PlayerPunishment> punishments,
            @NotNull CommandSender executor,
            boolean silent
    ) {
        this.userInfo = userInfo;
        this.type = type;
        this.punishments = punishments;
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

    public @NotNull UserInfo getUserInfo() {
        return this.userInfo;
    }

    public @NotNull PunishmentType getType() {
        return this.type;
    }

    /** The deactivated records, as they were immediately before being lifted. */
    public @NotNull Collection<PlayerPunishment> getPunishments() {
        return this.punishments;
    }

    public @NotNull CommandSender getExecutor() {
        return this.executor;
    }

    public boolean isSilent() {
        return this.silent;
    }
}