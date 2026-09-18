package su.nightexpress.sunlight.module.chat.context;

import org.bukkit.entity.Player;

import su.nightexpress.sunlight.module.chat.cache.CachedContent;
import su.nightexpress.sunlight.module.chat.cache.UserChatCache;

public class CommandContext extends ChatContext {

    public CommandContext(Player player, UserChatCache chatCache, String originalMessage) {
        super(player, chatCache, originalMessage);
    }

    @Override

    public CachedContent getLastContent() {
        return this.cache.getLastCommand();
    }

    @Override
    public void setLastContent(String message, long lifeTime) {
        this.cache.setLastCommand(message, lifeTime);
    }

    public String getCommandName() {
        int spaceIndex = this.message.indexOf(' ');
        String name = spaceIndex > 0 ? this.originalMessage.substring(0, spaceIndex) : this.originalMessage;

        int colonIndex = name.indexOf(':');
        return colonIndex > 0 ? name.substring(colonIndex + 1) : name;
    }

    public String getCommandBody() {
        int index = this.message.indexOf(' ');
        return index > 0 ? this.originalMessage.substring(index + 1) : null;
    }
}
