package su.nightexpress.sunlight.moduleImpl.nametags.model;

import su.nightexpress.sunlight.utils.Utils;

import java.util.Objects;

/**
 * A stored entitlement for a single tag. One grant row per player and tag id.
 * <p>
 * Gson friendly: keeps a no-arg constructor and plain fields so it round-trips
 * through the user {@code properties} JSON column.
 */
public class GrantRecord {

    private String tagId;
    private TagAccessMode mode;
    private long grantedAt;
    /** {@code 0} means permanent. */
    private long expiresAt;
    /** Set for {@link PriceMode#EXTERNAL} tags, identifies the owning system. */
    private String external;

    public GrantRecord() {
        this("", TagAccessMode.PURCHASE, 0L, 0L, "");
    }

    public GrantRecord(@org.jetbrains.annotations.NotNull String tagId,
            @org.jetbrains.annotations.NotNull TagAccessMode mode,
            long grantedAt,
            long expiresAt,
            @org.jetbrains.annotations.NotNull String external
    ) {
        this.tagId = Utils.lowercase(tagId);
        this.mode = mode;
        this.grantedAt = grantedAt;
        this.expiresAt = Math.max(0L, expiresAt);
        this.external = external == null ? "" : external;
    }

    /** Creates a permanent purchase grant. */
    public static GrantRecord purchase(@org.jetbrains.annotations.NotNull String tagId, long nowMillis) {
        return new GrantRecord(tagId, TagAccessMode.PURCHASE, nowMillis, 0L, "");
    }

    public @org.jetbrains.annotations.NotNull String getTagId() {
        return this.tagId;
    }

    public void setTagId(@org.jetbrains.annotations.NotNull String tagId) {
        this.tagId = Utils.lowercase(tagId);
    }

    public @org.jetbrains.annotations.NotNull TagAccessMode getMode() {
        return this.mode;
    }

    public void setMode(@org.jetbrains.annotations.NotNull TagAccessMode mode) {
        this.mode = mode;
    }

    public long getGrantedAt() {
        return this.grantedAt;
    }

    public void setGrantedAt(long grantedAt) {
        this.grantedAt = grantedAt;
    }

    public long getExpiresAt() {
        return this.expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = Math.max(0L, expiresAt);
    }

    public @org.jetbrains.annotations.NotNull String getExternal() {
        return this.external;
    }

    public void setExternal(@org.jetbrains.annotations.NotNull String external) {
        this.external = external == null ? "" : external;
    }

    /** A subscription with {@code expiresAt == 0} is treated as a lifetime subscription. */
    public boolean isPermanent() {
        return this.expiresAt <= 0L;
    }

    public boolean isExpired(long nowMillis) {
        if (this.isPermanent()) return false;
        return nowMillis >= this.expiresAt;
    }

    public boolean isActive(long nowMillis) {
        return !this.isExpired(nowMillis);
    }

    /**
     * Extends a subscription from its current expiry.
     *
     * @param nowMillis current time
     * @param period    billing period
     * @return the new grant, or {@code null} if this is not an active subscription
     */
    public GrantRecord extend(@org.jetbrains.annotations.NotNull SubscriptionPeriod period, long nowMillis) {
        if (this.mode != TagAccessMode.SUBSCRIPTION) return null;
        if (this.isExpired(nowMillis)) {
            return new GrantRecord(this.tagId, this.mode, nowMillis, nowMillis + period.toMillis(), this.external);
        }
        return new GrantRecord(this.tagId, this.mode, this.grantedAt, this.expiresAt + period.toMillis(), this.external);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GrantRecord record)) return false;
        return this.grantedAt == record.grantedAt
            && this.expiresAt == record.expiresAt
            && this.tagId.equals(record.tagId)
            && this.mode == record.mode
            && this.external.equals(record.external);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.tagId, this.mode, this.grantedAt, this.expiresAt, this.external);
    }

    @Override
    public String toString() {
        return "GrantRecord{tag=%s, mode=%s, expiresAt=%d}".formatted(this.tagId, this.mode, this.expiresAt);
    }
}
