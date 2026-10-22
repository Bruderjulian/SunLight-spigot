package su.nightexpress.sunlight.moduleImpl.voting.model;

/**
 * How a set of world names is read.
 * <p>
 * The same enum serves both the poll-level restriction and an option-level gate, because "only in
 * these worlds" and "never in these worlds" are the only two questions either one can ask.
 */
public enum WorldMode {

    BLACKLIST,
    WHITELIST;

    /**
     * @param listed  Whether {@code worldName} appears in the configured set.
     * @param current The world the player is actually in.
     * @return whether the vote is allowed here.
     */
    public boolean allows(final String current, final boolean listed) {
        if (current == null) {
            // No world to match against: a whitelist cannot be satisfied, a blacklist cannot be
            // violated. Deny rather than guess, otherwise a vote is accepted from a player whose
            // world the module cannot even see.
            return this == BLACKLIST;
        }
        return this == BLACKLIST ? !listed : listed;
    }
}