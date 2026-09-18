package su.nightexpress.sunlight.module.chat.context;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.module.chat.cache.CachedContent;
import su.nightexpress.sunlight.module.chat.cache.UserChatCache;

public class ConversationContext extends FormattedContext {

    protected final Player target;

    public ConversationContext(Player player,
            UserChatCache cache,
            String originalMessage,
            String proxyFormat,
            Player target) {
        super(player, cache, originalMessage);
        this.target = target;
        this.setFormat(proxyFormat);
    }

    @Override

    public CachedContent getLastContent() {
        return null;
    }

    @Override
    public void setLastContent(String message, long lifeTime) {

    }

    public Player getTarget() {
        return this.target;
    }
}
