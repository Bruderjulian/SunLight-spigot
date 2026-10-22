package su.nightexpress.sunlight.moduleImpl.voting.model;

/**
 * What happens when two or more passing choices finish tied on score.
 * <p>
 * Ties are measured on weighted score, not headcount: on a weighted poll the headcount
 * tie is not the tie that decides anything.
 */
public enum TieBreak {

    /** Lowest option key wins. Deterministic; the historical behavior. */
    FIRST,
    /** No winner: the vote fails like no choice had passed. */
    FAIL,
    /** A random choice among the tied leaders wins. */
    RANDOM
}
