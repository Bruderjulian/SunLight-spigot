package su.nightexpress.sunlight.api.provider.dto;

/**
 * What a player is counting down to.
 * <p>
 * Named {@code Kind} to keep it distinct from Core's own {@code WarmupType}, which the API module
 * must not depend on. The two are converted at the module boundary and may drift, so treat this as
 * the stable name to code against.
 */
public enum WarmupKind {

    /** A delay before a teleport goes through. */
    TELEPORT,

    /** A delay before a command runs. */
    COMMAND
}