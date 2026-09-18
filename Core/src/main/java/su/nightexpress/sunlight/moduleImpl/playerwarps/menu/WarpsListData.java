package su.nightexpress.sunlight.moduleImpl.playerwarps.menu;

import su.nightexpress.sunlight.moduleImpl.playerwarps.category.WarpCategory;

public record WarpsListData(WarpCategory category, PlayerWarpSortType sortType, String searchText) {

}
