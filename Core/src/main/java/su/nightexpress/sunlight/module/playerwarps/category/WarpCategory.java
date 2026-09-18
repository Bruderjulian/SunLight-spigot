package su.nightexpress.sunlight.module.playerwarps.category;

import org.jspecify.annotations.NonNull;
import su.nightexpress.sunlight.module.playerwarps.PlayerWarp;

public interface WarpCategory {

     String name();

    boolean isWarpOfThis( PlayerWarp warp);
}
