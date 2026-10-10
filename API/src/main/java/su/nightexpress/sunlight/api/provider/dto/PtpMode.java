package su.nightexpress.sunlight.api.provider.dto;

/**
 * Whether a PTP entry is a request to be teleported or an invitation offering to teleport.
 * <p>
 * Module-prefixed to keep it distinct from Core's own {@code TeleportMode}, which the API module
 * must not depend on. The two are converted at the module boundary and may drift, so treat this
 * as the stable name to code against.
 */
public enum PtpMode {

    /** The target travels to the sender when they accept. */
    REQUEST,

    /** The sender travels to the target when they accept. */
    INVITE
}