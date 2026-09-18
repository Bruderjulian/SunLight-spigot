package su.nightexpress.sunlight.moduleImpl.chat.context;

import org.bukkit.entity.Player;

import su.nightexpress.sunlight.moduleImpl.chat.cache.UserChatCache;

public abstract class FormattedContext extends ChatContext {

    protected String format;

    public FormattedContext(Player player, UserChatCache cache, String originalMessage) {
        super(player, cache, originalMessage);
    }

    public String getFormat() {
        return this.format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
