package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.kits.model.Kit;

/**
 * Fired after a new kit has been created, written to disk and registered.
 * <p>
 * Not cancellable: {@code createKit} returns {@code void} and throws on an invalid or duplicate
 * name, so there is no way for a listener to veto the kit after the fact without unwinding the
 * call. This event exists for observation — auditing, syncing to an external store, or seeding
 * default contents on a newly created kit.
 */
public class KitCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Kit kit;

    public KitCreateEvent(@NotNull Kit kit) {
        this.kit = kit;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /** The freshly created kit, already loaded with its default definition. */
    public @NotNull Kit getKit() {
        return this.kit;
    }
}