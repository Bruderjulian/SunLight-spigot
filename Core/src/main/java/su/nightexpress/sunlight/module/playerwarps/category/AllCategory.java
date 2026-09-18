package su.nightexpress.sunlight.module.playerwarps.category;

import su.nightexpress.sunlight.module.playerwarps.PlayerWarp;
import su.nightexpress.sunlight.module.playerwarps.core.PlayerWarpsLang;

public class AllCategory implements WarpCategory {

    @Override

    public String name() {
        return PlayerWarpsLang.CATEGORY_ALL_NAME.text();
    }

    @Override
    public boolean isWarpOfThis(PlayerWarp warp) {
        return true;
    }
}
