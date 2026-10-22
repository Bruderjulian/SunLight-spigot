package su.nightexpress.sunlight.api.provider.dto;

/**
 * What a Chat module spy is watching.
 * <p>
 * Module-prefixed to keep it distinct from Core's own {@code SpyType}, which the API module must
 * not depend on. The two are converted at the module boundary and may drift, so treat this as the
 * stable name to code against.
 */
public enum ChatSpyType {

    /** Social (Discord) traffic. */
    SOCIAL,

    /** Commands as players run them. */
    COMMAND,

    /** Chat messages as players send them. */
    CHAT
}