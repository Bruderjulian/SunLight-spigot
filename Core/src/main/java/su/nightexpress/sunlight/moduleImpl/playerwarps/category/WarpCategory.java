package su.nightexpress.sunlight.moduleImpl.playerwarps.category;

import su.nightexpress.sunlight.moduleImpl.playerwarps.PlayerWarp;

public interface WarpCategory {

    String name();

    boolean isWarpOfThis(PlayerWarp warp);
}
