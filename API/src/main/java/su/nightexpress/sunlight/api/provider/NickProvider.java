package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;

public interface NickProvider {

    boolean hasNickname(Player player);

    String getNickname(Player player);

    void setNickname(Player player, String nickname);

    void clearNickname(Player player);
}
