package su.nightexpress.sunlight.moduleImpl.voting.model;

/**
 * A requirement attached to a single option.
 * <p>
 * All gates on an option must pass, so a choice can be narrowed progressively by stacking them.
 * A gate that is not in the option's set is not evaluated at all — which matters, because an
 * unset payment gate reads a cost of {@code 0} and an unset playtime gate reads {@code 0} hours,
 * and both of those would otherwise be indistinguishable from "configured to be free".
 */
public enum GateType {

    /** Costs currency, charged when the vote is cast and refunded on withdrawal. */
    PAYMENT,
    /** Requires a permission node. */
    PERMISSION,
    /** Requires membership of a permission group. */
    RANK,
    /** Requires being in (or not being in) a set of worlds. */
    WORLD,
    /** Requires a minimum number of hours played. */
    PLAYTIME
}