package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.kits.model.Kit;

/**
 * Fired after a kit has been deleted from disk, unregistered and had its stored player data queued
 * for removal.
 * <p>
 * Not cancellable, for the same reason as {@link KitCreateEvent} — {@code deleteKit} returns
 * {@code void}. Use this to clean up anything keyed by kit id in other plugins; nothing else will
 * hear about the deletion.
 * <p>
 * The {@link Kit} is no longer reachable through the module's lookup, but its id, name and
 * definition are still readable here, which is all an audit or cleanup hook needs.
 */
public class KitDeleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Kit kit;

    public KitDeleteEvent(@NotNull Kit kit) {
        this.kit = kit;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /** The deleted kit, as it was immediately before removal. */
    public @NotNull Kit getKit() {
        return this.kit;
    }
}