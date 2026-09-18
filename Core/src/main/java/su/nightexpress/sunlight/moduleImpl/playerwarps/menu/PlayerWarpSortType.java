package su.nightexpress.sunlight.moduleImpl.playerwarps.menu;

import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.sunlight.moduleImpl.playerwarps.PlayerWarp;
import su.nightexpress.sunlight.moduleImpl.playerwarps.core.PlayerWarpsLang;

import java.util.Comparator;

public enum PlayerWarpSortType {

    NAME(Comparator.comparing(PlayerWarp::getName)),
    DATE_CREATION(Comparator.comparingLong(PlayerWarp::getCreationTimestamp).reversed()),
    VISITS(Comparator.comparingLong(PlayerWarp::getTotalVisits).reversed()),
    ;

    private final Comparator<PlayerWarp> comparator;

    PlayerWarpSortType(Comparator<PlayerWarp> comparator) {
        this.comparator = comparator;
    }

    public Comparator<PlayerWarp> getComparator() {
        return this.comparator;
    }

    public PlayerWarpSortType next() {
        return Lists.next(this);
    }

    public String localized() {
        return PlayerWarpsLang.SORT_TYPE.getLocalized(this);
    }
}
