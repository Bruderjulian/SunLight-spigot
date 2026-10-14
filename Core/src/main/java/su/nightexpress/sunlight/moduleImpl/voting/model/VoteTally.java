package su.nightexpress.sunlight.moduleImpl.voting.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The current state of a poll's votes, derived on demand from the ballot rows.
 * <p>
 * Deliberately not persisted: everything here is a sum over rows that already exist, so a stored
 * copy could only ever disagree with them.
 * <p>
 * Both a weighted score and a raw headcount are kept per option. They answer different questions
 * and neither is honest on its own — the headcount says how many people chose what, the score
 * says what the poll's own weighting rules made those choices worth — so the result surface shows
 * both rather than collapsing them into one number.
 */
public class VoteTally {

    private final Map<String, Double> scores;
    private final Map<String, Integer> counts;

    private final Set<UUID> participants;

    private final double totalScore;
    private final double passPercent;
    private final boolean neutralCounts;

    public VoteTally(final Collection<VoteBallot> ballots, final VoteRules rules) {
        this.scores = new HashMap<>();
        this.counts = new HashMap<>();
        this.participants = new HashSet<>();
        this.passPercent = rules.getPassPercent();
        this.neutralCounts = rules.countsNeutral();

        for (final VoteBallot ballot : ballots) {
            // Ballot keys are normalised on the way in: the lookup side lower-cases everything,
            // so a row stored with any other casing would otherwise split into a phantom tally
            // entry whose votes no share/leader computation can see.
            final String key = ballot.optionKey() == null ? null
                    : ballot.optionKey().toLowerCase(java.util.Locale.ROOT);
            if (key == null) {
                continue;
            }
            this.scores.merge(key, ballot.weight(), Double::sum);
            this.counts.merge(key, 1, Integer::sum);
            this.participants.add(ballot.playerId());
        }

        double total = 0D;
        for (final Map.Entry<String, Double> entry : this.scores.entrySet()) {
            if (!this.neutralCounts && VoteOption.isNeutralKey(entry.getKey())) {
                // Abstentions still show up in the participant count, they just do not dilute the
                // share of the choices that were actually made.
                continue;
            }
            total += entry.getValue();
        }
        this.totalScore = total;
    }

    private static String normalise(final String optionKey) {
        return optionKey == null ? null : optionKey.toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * @return whether this option takes part in the pass computation at all. Neutral is turnout,
     *         not a decision: when the poll excludes it from the total it can neither pass nor
     *         win, however many abstentions were cast.
     */
    private boolean counts(final String optionKey) {
        return this.neutralCounts || !VoteOption.isNeutralKey(optionKey);
    }

    public double score(final String optionKey) {
        final String key = normalise(optionKey);
        return key == null ? 0D : this.scores.getOrDefault(key, 0D);
    }

    public int count(final String optionKey) {
        final String key = normalise(optionKey);
        return key == null ? 0 : this.counts.getOrDefault(key, 0);
    }

    /**
     * @return this option's weighted share of the total, as a percentage. Always a proper split,
     *         because the denominator is the sum of every option's score — which is what keeps it
     *         meaningful when one player holds votes on several options at once. An option that
     *         does not count toward the total reports a share of zero rather than dividing its
     *         own score by everybody else's.
     */
    public double share(final String optionKey) {
        if (this.totalScore <= 0D || !this.counts(optionKey)) {
            return 0D;
        }
        return this.score(optionKey) / this.totalScore * 100D;
    }

    public boolean passes(final String optionKey) {
        if (!this.counts(optionKey)) {
            return false;
        }
        return this.share(optionKey) >= this.passPercent;
    }

    /**
     * @return the passing option with the highest score, or {@code null} when none passes. Ties
     *         break on the option key so the same poll always reports the same winner instead of
     *         whichever entry the hash order happened to visit first.
     */
    public String leader() {
        final List<String> tied = this.tiedLeaders();
        if (tied.isEmpty()) {
            return null;
        }
        return tied.getFirst();
    }

    /**
     * @return every passing choice tied at the top score, sorted by key. Empty when nothing passes.
     *         A single entry means a clean winner; several mean the poll's tie-break rule decides.
     */
    public java.util.List<String> tiedLeaders() {
        final java.util.List<String> keys = new java.util.ArrayList<>(this.scores.keySet());
        java.util.Collections.sort(keys);
        double bestScore = Double.NEGATIVE_INFINITY;
        final java.util.List<String> tied = new java.util.ArrayList<>();
        for (final String key : keys) {
            if (!this.passes(key)) {
                continue;
            }
            final double score = this.scores.getOrDefault(key, 0D);
            if (tied.isEmpty() || score > bestScore) {
                tied.clear();
                tied.add(key);
                bestScore = score;
            } else if (Double.compare(score, bestScore) == 0) {
                tied.add(key);
            }
        }
        return tied;
    }

    /**
     * @return the top options by score regardless of pass, best first. Used for runoffs,
     *         which re-run the strongest choices even when none of them passed.
     */
    public java.util.List<String> topOptions(final int limit) {
        final java.util.List<String> keys = new java.util.ArrayList<>(this.scores.keySet());
        keys.sort((first, second) -> {
            final int byScore = Double.compare(this.scores.getOrDefault(second, 0D),
                    this.scores.getOrDefault(first, 0D));
            return byScore != 0 ? byScore : first.compareTo(second);
        });
        if (limit > 0 && keys.size() > limit) {
            return keys.subList(0, limit);
        }
        return keys;
    }

    public int getParticipantCount() {
        return this.participants.size();
    }

    public boolean hasVoted(final UUID playerId) {
        return this.participants.contains(playerId);
    }

    public double getTotalScore() {
        return this.totalScore;
    }

    public double getPassPercent() {
        return this.passPercent;
    }
}