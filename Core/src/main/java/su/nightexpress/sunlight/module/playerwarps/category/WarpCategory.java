package su.nightexpress.sunlight.module.playerwarps.category;

import su.nightexpress.sunlight.module.playerwarps.PlayerWarp;

public interface WarpCategory {

    String name();

    boolean isWarpOfThis(PlayerWarp warp);
}
