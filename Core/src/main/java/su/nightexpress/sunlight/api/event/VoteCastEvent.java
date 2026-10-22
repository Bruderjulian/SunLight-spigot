package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteOption;

import java.util.UUID;

/**
 * Fired before a ballot is written or removed.
 * <p>
 * Cancelling is a full veto: nothing is stored and, on a cast that would have charged the player,
 * nothing is charged either. The payment is deliberately taken after this event rather than
 * before, so a veto can never leave a player out of pocket for a vote that did not happen.
 * <p>
 * The same event covers taking a vote back, distinguished by {@link #isWithdraw()}, because both
 * directions change the result of the poll and a moderation plugin may care about either.
 */
public class VoteCastEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Vote vote;
    private final VoteOption option;
    private final VoteOption replaced;
    private final UUID playerId;
    private final String playerName;
    private final double weight;
    private final double cost;
    private final boolean withdraw;

    private boolean cancelled;

    public VoteCastEvent(@NotNull final Vote vote,
            @NotNull final VoteOption option,
            @Nullable final VoteOption replaced,
            @NotNull final UUID playerId,
            @NotNull final String playerName,
            final double weight,
            final double cost,
            final boolean withdraw) {
        this.vote = vote;
        this.option = option;
        this.replaced = replaced;
        this.playerId = playerId;
        this.playerName = playerName;
        this.weight = weight;
        this.cost = cost;
        this.withdraw = withdraw;
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

    public @NotNull VoteOption getOption() {
        return this.option;
    }

    /**
     * @return the choice this ballot replaces, which is the only one in a single-vote poll, or
     *         {@code null} when the player held nothing on this option and nothing is being given
     *         up. Carried on the event so a listener does not have to query the poll and race the
     *         write that is about to happen.
     */
    public @Nullable VoteOption getReplaced() {
        return this.replaced;
    }

    public @NotNull UUID getPlayerId() {
        return this.playerId;
    }

    public @NotNull String getPlayerName() {
        return this.playerName;
    }

    /** The weight this ballot carries, rather than what the option itself is worth. */
    public double getWeight() {
        return this.weight;
    }

    /** What the player pays for this ballot, or is refunded. Zero when the option is ungated. */
    public double getCost() {
        return this.cost;
    }

    /** Whether this event is about taking a ballot back rather than casting one. */
    public boolean isWithdraw() {
        return this.withdraw;
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