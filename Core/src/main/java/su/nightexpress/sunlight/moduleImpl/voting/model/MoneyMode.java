package su.nightexpress.sunlight.moduleImpl.voting.model;

/**
 * What a poll does with the economy.
 * <p>
 * These pull in opposite directions, which is why they are separate axes rather than three
 * booleans: {@code GATE} says "spend to be allowed in", {@code WEIGHT} says "spend to be allowed
 * to count for more". A poll can enable either, both, or neither.
 */
public enum MoneyMode {

    NONE,
    GATE,
    WEIGHT,
    BOTH;

    public boolean hasGate() {
        return this == GATE || this == BOTH;
    }

    public boolean hasWeight() {
        return this == WEIGHT || this == BOTH;
    }
}