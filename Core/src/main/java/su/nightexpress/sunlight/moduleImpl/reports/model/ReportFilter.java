package su.nightexpress.sunlight.moduleImpl.reports.model;

import su.nightexpress.sunlight.utils.Utils;

public enum ReportFilter {
    /** Unclaimed, unresolved. The staff work queue. */
    OPEN,
    /** Claimed by a staff member, still unresolved. */
    CLAIMED,
    /** Everything the viewer filed, regardless of status. */
    MINE,
    /** Everything, including concluded reports. */
    ALL,
    /** Everything against one specific player. */
    TARGET,
    /** Everything that belongs to one case. */
    CASE;

    public boolean isStaffOnly() {
        return this != MINE;
    }

    public String getConfigName() {
        return this.name().toLowerCase(java.util.Locale.ROOT);
    }

    public static ReportFilter fromString(String raw) {
        return Utils.enumValueOf(raw, ReportFilter.class);
    }
}
