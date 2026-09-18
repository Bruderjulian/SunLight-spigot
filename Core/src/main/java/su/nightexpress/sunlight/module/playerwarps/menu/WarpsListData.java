package su.nightexpress.sunlight.module.playerwarps.menu;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import su.nightexpress.sunlight.module.playerwarps.category.WarpCategory;

public record WarpsListData(WarpCategory category, PlayerWarpSortType sortType, String searchText) {

}
