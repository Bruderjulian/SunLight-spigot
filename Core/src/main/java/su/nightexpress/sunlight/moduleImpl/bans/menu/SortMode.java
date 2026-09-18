package su.nightexpress.sunlight.moduleImpl.bans.menu;

import java.util.Comparator;

import su.nightexpress.sunlight.moduleImpl.bans.punishment.AbstractPunishment;

public enum SortMode {

    NAME_ASCENT(Comparator.comparing(AbstractPunishment::getName)),
    NAME_DESCENT(NAME_ASCENT.comparator.reversed()),
    OLDEST(Comparator.comparingLong(AbstractPunishment::getCreationDate)),
    NEWEST(OLDEST.comparator.reversed()),
    LEAST_DURATION(Comparator.comparingLong(AbstractPunishment::getDuration)),
    MOST_DURATION(LEAST_DURATION.comparator.reversed()),
    PUNISHER(Comparator.comparing(AbstractPunishment::getWho)),
    REASON(Comparator.comparing(AbstractPunishment::getReason));

    private final Comparator<AbstractPunishment> comparator;

    SortMode(Comparator<AbstractPunishment> comparator) {
        this.comparator = comparator;
    }

    public Comparator<AbstractPunishment> comparator() {
        return this.comparator;
    }
}
