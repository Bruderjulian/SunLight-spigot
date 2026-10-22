package su.nightexpress.sunlight.moduleImpl.voting.model;

/**
 * What happens to a ballot from a player whose balance is above a poll's ceiling.
 * <p>
 * {@code CLAMP} caps the vote at the value the ceiling corresponds to rather than throwing the
 * ballot away, which keeps rich players participating without letting them swing the poll.
 */
public enum OverMaxMode {

    REJECT,
    CLAMP
}