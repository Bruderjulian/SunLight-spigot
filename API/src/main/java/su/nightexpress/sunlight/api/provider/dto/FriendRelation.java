package su.nightexpress.sunlight.api.provider.dto;

/** Which way a friendship stands between two players, for a caller that only needs the verdict. */
public enum FriendRelation {

    SELF,
    FRIEND,
    BLOCKED,
    PENDING_INCOMING,
    PENDING_OUTGOING,
    NONE;

    public boolean isFriend() {
        return this == FRIEND;
    }
}