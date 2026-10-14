package su.nightexpress.sunlight.moduleImpl.voting.model;

import java.util.UUID;

/**
 * One player's vote on one option of one poll.
 * <p>
 * The weight and the amount paid are frozen at cast time rather than recomputed on read. A player
 * whose rank changes, or whose balance drops, after voting must not silently rewrite what they
 * voted for; the poll's own caps still bound how much any single ballot could have been worth when
 * it was cast.
 * <p>
 * The (vote, player, option) triple is the ballot table's identity, so a player holding votes on
 * several options has several rows and one row is a unit of {@code costPaid}.
 */
public record VoteBallot(UUID voteId,
                         String optionKey,
                         UUID playerId,
                         String playerName,
                         double weight,
                         double costPaid,
                         long date) {

    public static VoteBallot create(final UUID voteId,
            final String optionKey,
            final UUID playerId,
            final String playerName,
            final double weight,
            final double costPaid) {
        return new VoteBallot(voteId, optionKey, playerId, playerName, weight, costPaid,
                System.currentTimeMillis());
    }
}