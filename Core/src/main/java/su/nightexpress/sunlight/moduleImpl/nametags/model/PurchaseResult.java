package su.nightexpress.sunlight.moduleImpl.nametags.model;

/**
 * Outcome of a tag purchase, so callers can tell a player why it was refused instead of
 * reporting the same message for every failure.
 */
public enum PurchaseResult {

    SUCCESS,
    /** The tag is owned by another plugin, so we must not touch it. */
    EXTERNAL,
    /** No economy plugin is installed, so a priced tag cannot be charged. */
    NO_ECONOMY,
    /** The player's balance is below the price. */
    NO_FUNDS,
    /** The entitlement could not be stored. */
    REFUSED;

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
