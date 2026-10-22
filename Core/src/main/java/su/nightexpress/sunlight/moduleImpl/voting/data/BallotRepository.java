package su.nightexpress.sunlight.moduleImpl.voting.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import su.nightexpress.sunlight.moduleImpl.voting.model.VoteBallot;

/**
 * In-memory index of ballots, keyed by vote and then by the player who cast them.
 * <p>
 * A player holding votes on several options of one poll has one row per option, so
 * {@link #getBallots(UUID, UUID)} — not {@link #getBallot(UUID, UUID)} — is the normal lookup:
 * it is what the vote limit, the tally and the refund pass all iterate.
 * <p>
 * Synchronized for the same reason as {@code VoteRepository}: rows arrive from the database sync
 * thread while the main thread reads and writes.
 */
public class BallotRepository {

    private final Map<UUID, Map<UUID, List<VoteBallot>>> byVote = new ConcurrentHashMap<>();

    public synchronized void clear() {
        this.byVote.clear();
    }

    public synchronized void upsert(final VoteBallot ballot) {
        final Map<UUID, List<VoteBallot>> voters = this.byVote.computeIfAbsent(ballot.voteId(),
                key -> new ConcurrentHashMap<>());
        final List<VoteBallot> ballots = voters.computeIfAbsent(ballot.playerId(),
                key -> new ArrayList<>());

        // The ballot id is derived from (vote, player, option), so a re-synced row replaces its
        // own earlier copy instead of stacking up beside it.
        ballots.removeIf(existing -> existing.optionKey().equalsIgnoreCase(ballot.optionKey()));
        ballots.add(ballot);
    }

    public synchronized void remove(final VoteBallot ballot) {
        this.remove(ballot.voteId(), ballot.playerId(), ballot.optionKey());
    }

    /**
     * Removes one player's ballot on one choice, leaving their other choices in that poll alone.
     * That distinction is the whole reason this exists: a player may hold several, and replacing
     * one must not discard the rest.
     */
    public synchronized void remove(final UUID voteId, final UUID playerId, final String optionKey) {
        final Map<UUID, List<VoteBallot>> voters = this.byVote.get(voteId);
        if (voters == null) {
            return;
        }
        final List<VoteBallot> ballots = voters.get(playerId);
        if (ballots == null) {
            return;
        }
        ballots.removeIf(existing -> existing.optionKey().equalsIgnoreCase(optionKey));
        if (ballots.isEmpty()) {
            voters.remove(playerId);
        }
        if (voters.isEmpty()) {
            this.byVote.remove(voteId);
        }
    }

    public synchronized void removeVote(final UUID voteId) {
        this.byVote.remove(voteId);
    }

    public synchronized void remove(final UUID voteId, final UUID playerId) {
        final Map<UUID, List<VoteBallot>> voters = this.byVote.get(voteId);
        if (voters == null) {
            return;
        }
        voters.remove(playerId);
        if (voters.isEmpty()) {
            this.byVote.remove(voteId);
        }
    }

    // -- Reads ------------------------------------------------------------------------------------
    // Synchronized like the writers: rows arrive from the database sync thread while the main
    // thread tallies and renders, and copying an ArrayList mid-removeIf would throw
    // ConcurrentModificationException from the middle of a menu render.

    public synchronized Collection<VoteBallot> getBallots(final UUID voteId) {
        if (voteId == null) {
            return List.of();
        }
        final Map<UUID, List<VoteBallot>> voters = this.byVote.get(voteId);
        if (voters == null) {
            return List.of();
        }
        final List<VoteBallot> all = new ArrayList<>();
        voters.values().forEach(all::addAll);
        return all;
    }

    public synchronized List<VoteBallot> getBallots(final UUID voteId, final UUID playerId) {
        if (voteId == null || playerId == null) {
            return List.of();
        }
        final Map<UUID, List<VoteBallot>> voters = this.byVote.get(voteId);
        if (voters == null) {
            return List.of();
        }
        final List<VoteBallot> ballots = voters.get(playerId);
        return ballots == null ? List.of() : List.copyOf(ballots);
    }

    public synchronized boolean hasVoted(final UUID voteId, final UUID playerId) {
        if (voteId == null || playerId == null) {
            return false;
        }
        final Map<UUID, List<VoteBallot>> voters = this.byVote.get(voteId);
        return voters != null && voters.containsKey(playerId);
    }

    /**
     * @return how many of the given open votes this player already holds a ballot in. Derived from
     *         the rows rather than kept as a counter, so it cannot drift out of step with the
     *         ballots that actually exist.
     */
    public synchronized int getParticipationCount(final UUID playerId, final Collection<UUID> openVoteIds) {
        if (playerId == null || openVoteIds == null) {
            return 0;
        }
        int count = 0;
        for (final UUID voteId : openVoteIds) {
            if (this.hasVoted(voteId, playerId)) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getCount(final UUID voteId) {
        return this.getBallots(voteId).size();
    }
}