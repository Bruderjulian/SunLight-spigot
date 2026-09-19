package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;

public interface FreezeProvider {

    boolean isFrozen(Player player);

    void setFrozen(Player player, boolean frozen);
}
