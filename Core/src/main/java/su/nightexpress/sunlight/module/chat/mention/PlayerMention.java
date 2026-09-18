package su.nightexpress.sunlight.module.chat.mention;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerMention implements ChatMention {

    private final String playerName;
    private final String format;

    public PlayerMention(String playerName, String format) {
        this.playerName = playerName;
        this.format = format;
    }

    @Override

    public String getFormat() {
        return this.format;
    }

    @Override
    public boolean isApplicable(Player player) {
        return player.getName().equalsIgnoreCase(this.playerName);
    }
}
