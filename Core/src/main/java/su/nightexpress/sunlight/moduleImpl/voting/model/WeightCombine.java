package su.nightexpress.sunlight.moduleImpl.voting.model;

/**
 * How a player who qualifies for more than one multiplier ends up with one multiplier.
 */
public enum WeightCombine {

    /** The best qualifying multiplier wins. A VIP who is also staff is worth VIP. */
    HIGHEST,
    /**
     * Every qualifying multiplier stacks additively: a player matching 2x and 3x is worth 5x.
     * Multipliers combine as a sum, not a product, so stacked tiers stay predictable and the
     * multiplier cap means what it says.
     */
    ADDITIVE
}