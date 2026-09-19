package su.nightexpress.sunlight.moduleImpl.chat.cache;

import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;
import su.nightexpress.sunlight.user.cache.UserCacheContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserChatCache implements UserCacheContainer {

    private final Map<String, Long> channelCooldownTimestamps;
    private final Map<String, Long> mentionCooldownTimestamps;

    private CachedContent lastMessage;
    private CachedContent lastCommand;

    private UUID lastConversationWith;
    private long nextCommandTimestamp;

    public UserChatCache() {
        this.channelCooldownTimestamps = new HashMap<>();
        this.mentionCooldownTimestamps = new HashMap<>();
    }

    @Override
    public void clear() {
        this.channelCooldownTimestamps.clear();
        this.mentionCooldownTimestamps.clear();
        this.lastMessage = null;
        this.lastCommand = null;
        this.lastConversationWith = null;
        this.nextCommandTimestamp = 0L;
    }

    @Override
    public void clearExpired() {
        this.channelCooldownTimestamps.values().removeIf(TimeUtil::isPassed);
        this.mentionCooldownTimestamps.values().removeIf(TimeUtil::isPassed);

        if (this.lastMessage != null && this.lastMessage.isExpired())
            this.lastMessage = null;
        if (this.lastCommand != null && this.lastCommand.isExpired())
            this.lastCommand = null;
    }

    public UUID getLastConversationWith() {
        return this.lastConversationWith;
    }

    public void setLastConversationWith(UUID lastConversationWith) {
        this.lastConversationWith = lastConversationWith;
    }

    public CachedContent getLastMessage() {
        return this.lastMessage;
    }

    public CachedContent getLastCommand() {
        return this.lastCommand;
    }

    public void setLastMessage(String message, long lifeTime) {
        this.lastMessage = CachedContent.create(message, lifeTime);
    }

    public void setLastCommand(String command, long lifeTime) {
        this.lastCommand = CachedContent.create(command, lifeTime);
    }

    public long getNextCommandTimestamp() {
        return this.nextCommandTimestamp;
    }

    public void setNextCommandTimestamp(long duration) {
        this.nextCommandTimestamp = System.currentTimeMillis() + duration;
    }

    public long getChannelCooldownTimestamp(String channelId) {
        return this.channelCooldownTimestamps.getOrDefault(Utils.lowercase(channelId), 0L);
    }

    public void setChannelCooldown(String channelId, long duration) {
        this.channelCooldownTimestamps.put(Utils.lowercase(channelId),
                TimeUtil.createFutureTimestamp(duration));
    }

    public boolean hasChannelCooldown(String channelId) {
        return !TimeUtil.isPassed(this.getChannelCooldownTimestamp(channelId));
    }

    public long getMentionCooldownTimestamp(String mentionId) {
        return this.mentionCooldownTimestamps.getOrDefault(Utils.lowercase(mentionId), 0L);
    }

    public void setMentionCooldown(String mentionId, long duration) {
        this.mentionCooldownTimestamps.put(Utils.lowercase(mentionId),
                TimeUtil.createFutureTimestamp(duration));
    }

    public boolean hasMentionCooldown(String mentionId) {
        return !TimeUtil.isPassed(this.getMentionCooldownTimestamp(mentionId));
    }
}
