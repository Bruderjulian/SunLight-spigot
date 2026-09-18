package su.nightexpress.sunlight.module.chat.mention;

import org.bukkit.entity.Player;

public interface ChatMention {

    boolean isApplicable(Player player);

    String getFormat();
}
