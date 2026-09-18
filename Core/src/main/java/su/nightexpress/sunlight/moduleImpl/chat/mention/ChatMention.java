package su.nightexpress.sunlight.moduleImpl.chat.mention;

import org.bukkit.entity.Player;

public interface ChatMention {

    boolean isApplicable(Player player);

    String getFormat();
}
