package su.nightexpress.sunlight.api.provider.dto;

/**
 * The slots the Back Location module caches locations in.
 * <p>
 * Module-prefixed to keep it distinct from Core's own {@code LocationType}, which the API module
 * must not depend on. The two are converted at the module boundary and may drift, so treat this
 * as the stable name to code against.
 */
public enum BackLocationType {

    /** Where the player stood before their last qualifying teleport. */
    PREVIOUS,

    /** Where the player died last. */
    DEATH
}