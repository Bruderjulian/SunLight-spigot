package su.nightexpress.sunlight.moduleImpl.playerwarps.category;

import org.bukkit.entity.Player;

import su.nightexpress.sunlight.moduleImpl.playerwarps.PlayerWarp;
import su.nightexpress.sunlight.moduleImpl.playerwarps.core.PlayerWarpsLang;

import java.util.UUID;

public class OwnCategory implements WarpCategory {

    private final UUID playerId;

    public OwnCategory(Player player) {
        this.playerId = player.getUniqueId();
    }

    @Override

    public String name() {
        return PlayerWarpsLang.CATEGORY_OWN_NAME.text();
    }

    @Override
    public boolean isWarpOfThis(PlayerWarp warp) {
        return warp.getOwnerId().equals(this.playerId);
    }
}
