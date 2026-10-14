package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;

import java.util.UUID;

/**
 * Fired once a poll has stopped accepting votes, whatever ended it.
 * <p>
 * Not cancellable: by the time it fires the outcome is already stored and the announcement and
 * outcome commands are already on their way, so there is nothing left to refuse. What this event
 * is for is reacting to a decision that has already been made.
 */
public class VoteConcludedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Vote vote;
    private final String outcomeKey;
    private final boolean manual;

    public VoteConcludedEvent(@NotNull final Vote vote, final @Nullable String outcomeKey, final boolean manual) {
        this.vote = vote;
        this.outcomeKey = outcomeKey;
        this.manual = manual;
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

    /**
     * @return the winning choice, or {@code null} when the poll failed or was cancelled. A failed
     *         poll and a cancelled one are both "no winner", and the {@link Vote}'s own status is
     *         what tells them apart.
     */
    public @Nullable String getOutcomeKey() {
        return this.outcomeKey;
    }

    public boolean hasPassed() {
        return this.outcomeKey != null;
    }

    /** Whether a staff member ended this poll, as opposed to its time running out. */
    public boolean isManual() {
        return this.manual;
    }

    public @NotNull UUID getVoteId() {
        return this.vote.getId();
    }
}