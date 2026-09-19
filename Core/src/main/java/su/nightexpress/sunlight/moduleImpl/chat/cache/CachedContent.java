package su.nightexpress.sunlight.moduleImpl.chat.cache;

import su.nightexpress.sunlight.utils.TimeUtil;

public class CachedContent {

    private final String content;
    private final long creationTimestamp;
    private final long expirationTimestamp;

    private int count;

    private CachedContent(String content, long creationTimestamp, long expirationTimestamp) {
        this.content = content;
        this.creationTimestamp = creationTimestamp;
        this.expirationTimestamp = expirationTimestamp;
        this.count = 1;
    }

    public static CachedContent create(String content, long lifeTime) {
        return new CachedContent(content, System.currentTimeMillis(), TimeUtil.createFutureTimestamp(lifeTime));
    }

    public void addCount() {
        this.count++;
    }

    public boolean isExpired() {
        return TimeUtil.isPassed(this.expirationTimestamp);
    }

    public String content() {
        return this.content;
    }

    public long getCreationTimestamp() {
        return this.creationTimestamp;
    }

    public long getExpirationTimestamp() {
        return this.expirationTimestamp;
    }

    public int getCount() {
        return this.count;
    }
}
