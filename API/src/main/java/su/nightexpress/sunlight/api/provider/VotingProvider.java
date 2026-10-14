package su.nightexpress.sunlight.api.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of the Voting module's open polls. Obtained via
 * {@code SunLightPlugin#votingProvider()}, which returns an empty {@link java.util.Optional} when
 * the module is disabled, so callers must treat this as a soft dependency.
 * <p>
 * Every type here is a primitive or a JDK type on purpose: this interface lives in the API module,
 * which must not depend on Core, so the module's own {@code Vote} model cannot be exposed. Scores
 * and shares are therefore returned as plain numbers rather than as a tally object.
 * <p>
 * Two different numbers are available per choice and they are not interchangeable: {@code votes} is
 * how many people picked it, {@code score} is what those votes were worth under the poll's own
 * weighting. Neither alone says how the poll will resolve.
 */
public interface VotingProvider {

    /** Keys of the polls that are currently open, in no particular order. */
    @NotNull List<String> getOpenVoteKeys();

    /** How many polls are open across the whole server. */
    int getOpenVoteCount();

    /**
     * A poll by its key, or {@code null} when there is no such poll. Note the overload below:
     * passing a bare {@code null} is ambiguous and needs a cast to either type.
     */
    @Nullable VoteHandle getVote(@Nullable String voteKey);

    /** A poll by id, or {@code null} when there is no such poll. */
    @Nullable VoteHandle getVote(@Nullable UUID voteId);

    /** How many people voted for a choice. */
    int getOptionVotes(@NotNull UUID voteId, @NotNull String optionKey);

    /** What the votes on a choice were worth after the poll's weighting rules. */
    double getOptionScore(@NotNull UUID voteId, @NotNull String optionKey);

    /** That choice's weighted share of the poll, as a percentage. */
    double getOptionShare(@NotNull UUID voteId, @NotNull String optionKey);

    /** How many distinct people have voted in this poll, counting a multi-vote voter once. */
    int getParticipantCount(@NotNull UUID voteId);

    /** Whether the given player holds a ballot in this poll. */
    boolean hasVoted(@NotNull UUID voteId, @NotNull UUID playerId);

    /**
     * @return the choices this player holds a ballot on, or an empty list. More than one entry
     *         only when the poll allows multiple votes.
     */
    @NotNull List<String> getPlayerVotes(@NotNull UUID voteId, @NotNull UUID playerId);

    /**
     * The weight one of this player's ballots carries, or {@code 0} when they have no ballot on
     * that choice. Weight is frozen when the vote is cast, so this does not change if the player
     * gains a rank mid-poll.
     */
    double getPlayerWeight(@NotNull UUID voteId, @NotNull UUID playerId, @NotNull String optionKey);

    /** The poll's description, or an empty string when it has none. */
    default @NotNull String getVoteDescription(@NotNull UUID voteId) {
        final VoteHandle handle = this.getVote(voteId);
        return handle == null ? "" : handle.description();
    }

    /** Whether the poll hides live counts until it concludes. */
    default boolean isSecretBallot(@NotNull UUID voteId) {
        final VoteHandle handle = this.getVote(voteId);
        return handle != null && handle.secret();
    }

    /** Keys of the scheduled polls that have not opened yet, in no particular order. */
    default @NotNull List<String> getScheduledVoteKeys() {
        return List.of();
    }
}