package su.nightexpress.sunlight.module.playerwarps.menu;

import su.nightexpress.sunlight.module.playerwarps.category.WarpCategory;

public record WarpsListData(WarpCategory category, PlayerWarpSortType sortType, String searchText) {

}
