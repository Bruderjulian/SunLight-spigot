package su.nightexpress.sunlight.moduleImpl.nametags.model;

import java.time.Duration;

/**
 * Billing period for a {@link TagAccessMode#SUBSCRIPTION} tag.
 */
public enum SubscriptionPeriod {

    WEEKLY("7d", Duration.ofDays(7)),
    MONTHLY("30d", Duration.ofDays(30)),
    QUARTERLY("90d", Duration.ofDays(90)),
    YEARLY("365d", Duration.ofDays(365));

    private final String id;
    private final Duration duration;

    SubscriptionPeriod(String id, Duration duration) {
        this.id = id;
        this.duration = duration;
    }

    public String getId() {
        return this.id;
    }

    public Duration getDuration() {
        return this.duration;
    }

    public long toMillis() {
        return this.duration.toMillis();
    }

    /**
     * Adds one period to the given timestamp, never returning a value in the past.
     *
     * @param fromMillis the timestamp to extend from
     * @param nowMillis  the current time, used to avoid producing an already-expired result
     * @return the new expiry timestamp
     */
    public long nextExpiryFrom(long fromMillis, long nowMillis) {
        long candidate = fromMillis + this.toMillis();
        return Math.max(candidate, nowMillis + this.toMillis());
    }
}
