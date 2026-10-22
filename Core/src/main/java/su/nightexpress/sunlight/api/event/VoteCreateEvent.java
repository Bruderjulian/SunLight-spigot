package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;

/**
 * Fired just before a poll is written and indexed, while it can still be refused cleanly.
 * <p>
 * Cancelling here means nothing was stored, so unlike {@link PlayerReportEvent} there is no
 * rollback to do. This is the point at which a plugin that wants to own who may open polls should
 * intercept.
 */
public class VoteCreateEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Vote vote;

    private boolean cancelled;

    public VoteCreateEvent(@NotNull final Vote vote) {
        this.vote = vote;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull Vote getVote() {
        return this.vote;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(final boolean cancelled) {
        this.cancelled = cancelled;
    }
}