package su.nightexpress.sunlight.moduleImpl.voting.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteOption;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteStatus;

/**
 * In-memory index of votes and their choices.
 * <p>
 * Every mutator is synchronized because {@code addTableSync} delivers rows from the database sync
 * thread while the main thread reads and writes — the same reason {@code ReportRepository} in the
 * Reports module synchronizes throughout.
 * <p>
 * There is no ORDER BY anywhere in the query API, so every accessor hands back an unordered
 * collection and callers sort. That is why the whole table is loaded eagerly at boot instead of
 * being queried per menu page.
 * <p>
 * Choices are indexed per vote rather than being kept on the {@link Vote} that the row mapper
 * builds: the mapper sees one table row at a time and has no way to know which vote it belongs to,
 * and a vote's row may well arrive after its choices have.
 */
public class VoteRepository {

    private final Map<UUID, Vote> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byKey = new ConcurrentHashMap<>();
    private final Map<VoteStatus, Set<UUID>> byStatus = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> byCreator = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, VoteOption>> optionsByVote = new ConcurrentHashMap<>();

    public synchronized void clear() {
        this.byId.clear();
        this.byKey.clear();
        this.byStatus.clear();
        this.byCreator.clear();
        this.optionsByVote.clear();
    }

    /**
     * Inserts a vote, or re-indexes it if it is already tracked. Used by the boot load and by table
     * sync, where the incoming object is an exact stand-in for the tracked one.
     */
    public synchronized void upsertVote(final Vote vote) {
        final Vote existing = this.byId.get(vote.getId());
        if (existing != null && existing != vote) {
            this.unindex(existing);
        } else if (existing == vote) {
            // Same instance re-announced (e.g. after an in-place state change the module already
            // re-indexed via applyTransition): drop every bucket it may sit in and re-add below.
            this.unindexAll(vote.getId());
        }

        this.byId.put(vote.getId(), vote);
        this.byKey.put(vote.getKey(), vote.getId());
        this.byStatus.computeIfAbsent(vote.getStatus(), key -> ConcurrentHashMap.newKeySet()).add(vote.getId());
        if (vote.getCreatorId() != null) {
            this.byCreator.computeIfAbsent(vote.getCreatorId(), key -> ConcurrentHashMap.newKeySet())
                    .add(vote.getId());
        }
        // Choices may have synced before their poll did. The sidecar map is the authority for
        // early rows, so attach them onto the fresh vote object rather than the other way round.
        final Map<String, VoteOption> known = this.optionsByVote.get(vote.getId());
        if (known != null) {
            known.values().forEach(vote::addOption);
        }
    }

    /**
     * Re-indexes a vote whose state changed in place (conclude/cancel/extend). The live object
     * already carries the new status, so the old bucket cannot be derived from it — every status
     * bucket is swept for the id instead. Must be called by the module right after mutating a
     * tracked vote and before/after persisting it.
     */
    public synchronized void applyTransition(final Vote vote) {
        this.unindexAll(vote.getId());
        this.byStatus.computeIfAbsent(vote.getStatus(), key -> ConcurrentHashMap.newKeySet()).add(vote.getId());
    }

    private void unindexAll(final UUID voteId) {
        for (final Set<UUID> ids : this.byStatus.values()) {
            ids.remove(voteId);
        }
        this.byStatus.values().removeIf(Set::isEmpty);
    }

    public synchronized void removeVote(final Vote vote) {
        if (!this.byId.containsKey(vote.getId())) {
            return;
        }
        this.unindex(vote);
        this.byId.remove(vote.getId());
        this.byKey.remove(vote.getKey(), vote.getId());
        this.optionsByVote.remove(vote.getId());
    }

    private void unindex(final Vote vote) {
        discard(this.byStatus, vote.getStatus(), vote.getId());
        if (vote.getCreatorId() != null) {
            discard(this.byCreator, vote.getCreatorId(), vote.getId());
        }
    }

    private static <K> void discard(final Map<K, Set<UUID>> map, final K key, final UUID voteId) {
        final Set<UUID> ids = map.get(key);
        if (ids == null) {
            return;
        }
        ids.remove(voteId);
        if (ids.isEmpty()) {
            map.remove(key, ids);
        }
    }

    // -- Choices ----------------------------------------------------------------------------------

    public synchronized void upsertOption(final VoteOption option, final UUID voteId) {
        final Map<String, VoteOption> options = this.optionsByVote.computeIfAbsent(voteId,
                key -> new ConcurrentHashMap<>());
        options.put(option.getKey(), option);

        final Vote vote = this.byId.get(voteId);
        if (vote != null) {
            vote.addOption(option);
        }
    }

    public synchronized void removeOption(final UUID voteId, final String optionKey) {
        final String key = optionKey == null ? null : optionKey.toLowerCase(Locale.ROOT);
        final Map<String, VoteOption> options = this.optionsByVote.get(voteId);
        if (options != null && key != null) {
            options.remove(key);
        }
        final Vote vote = this.byId.get(voteId);
        if (vote != null) {
            vote.removeOption(key);
        }
    }

    // -- Reads ------------------------------------------------------------------------------------

    public Vote getVote(final UUID voteId) {
        return voteId == null ? null : this.byId.get(voteId);
    }

    public Vote getVote(final String voteKey) {
        if (voteKey == null) {
            return null;
        }
        final UUID id = this.byKey.get(voteKey.toLowerCase(Locale.ROOT));
        return id == null ? null : this.byId.get(id);
    }

    public boolean containsKey(final String voteKey) {
        return voteKey != null && this.byKey.containsKey(voteKey.toLowerCase(Locale.ROOT));
    }

    public Collection<Vote> getVotes() {
        return new ArrayList<>(this.byId.values());
    }

    public List<Vote> getVotes(final VoteStatus status) {
        final Set<UUID> ids = this.byStatus.get(status);
        if (ids == null) {
            return List.of();
        }
        final List<Vote> votes = new ArrayList<>(ids.size());
        for (final UUID id : ids) {
            final Vote vote = this.byId.get(id);
            if (vote != null) {
                votes.add(vote);
            }
        }
        return votes;
    }

    public List<Vote> getOpenVotes() {
        return this.getVotes(VoteStatus.OPEN);
    }

    public List<Vote> getVotesByCreator(final UUID creatorId) {
        if (creatorId == null) {
            return List.of();
        }
        final Set<UUID> ids = this.byCreator.get(creatorId);
        if (ids == null) {
            return List.of();
        }
        final List<Vote> votes = new ArrayList<>(ids.size());
        for (final UUID id : ids) {
            final Vote vote = this.byId.get(id);
            if (vote != null) {
                votes.add(vote);
            }
        }
        return votes;
    }

    public int getOpenCount() {
        return this.getVotes(VoteStatus.OPEN).size();
    }

    public int getOpenCount(final UUID creatorId) {
        return (int) this.getVotesByCreator(creatorId).stream().filter(Vote::isOpen).count();
    }
}