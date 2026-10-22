package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.bans.punishment.AbstractPunishment;

/**
 * Fired after a punishment record has been permanently removed from both the in-memory repository
 * and the database.
 * <p>
 * This is a hard delete, not a pardon — a pardon only deactivates a record and keeps it for history,
 * which is covered by {@link PlayerPardonEvent}. Use this for the purge paths (history menus, data
 * cleanup) where the record is gone for good.
 * <p>
 * Both player and IP punishments are reported here; check {@code instanceof PlayerPunishment} versus
 * {@code InetPunishment}, or read {@link AbstractPunishment#getType()}.
 */
public class PunishmentDeleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final AbstractPunishment punishment;

    public PunishmentDeleteEvent(@NotNull AbstractPunishment punishment) {
        this.punishment = punishment;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /** The record as it was immediately before deletion. */
    public @NotNull AbstractPunishment getPunishment() {
        return this.punishment;
    }
}