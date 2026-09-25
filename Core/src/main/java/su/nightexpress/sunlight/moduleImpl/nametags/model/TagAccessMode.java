package su.nightexpress.sunlight.moduleImpl.nametags.model;

/**
 * How a player gains access to a {@link TagDefinition}.
 */
public enum TagAccessMode {

    /** Available to everyone allowed by the profile. */
    FREE,

    /** Requires the derived {@code nametags.tag.<id>} or the tag's own permission node. */
    PERMISSION,

    /** One-time purchase, stored as a permanent grant. */
    PURCHASE,

    /** Renewable subscription, stored as a grant with an expiry timestamp. */
    SUBSCRIPTION

}
