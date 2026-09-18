package su.nightexpress.sunlight.module.chat.context;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.module.chat.cache.CachedContent;
import su.nightexpress.sunlight.module.chat.cache.UserChatCache;

public abstract class ChatContext {

    protected final Player player;
    protected final UserChatCache cache;
    protected final String originalMessage;

    protected String message;
    protected boolean cancelled;

    public ChatContext(Player player, UserChatCache cache, String originalMessage) {
        this.player = player;
        this.cache = cache;
        this.originalMessage = originalMessage;

        this.setMessage(originalMessage);
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public abstract CachedContent getLastContent();

    public abstract void setLastContent(String message, long lifeTime);

    public Player getPlayer() {
        return this.player;
    }

    public UserChatCache getCache() {
        return this.cache;
    }

    public String getOriginalMessage() {
        return this.originalMessage;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
