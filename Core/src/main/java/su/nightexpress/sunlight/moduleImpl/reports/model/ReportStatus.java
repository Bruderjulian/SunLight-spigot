package su.nightexpress.sunlight.moduleImpl.reports.model;

import su.nightexpress.sunlight.utils.Utils;

public enum ReportStatus {
    /** Filed, nobody has picked it up yet. */
    OPEN,
    /** A staff member has taken responsibility for working it. */
    CLAIMED,
    /** Terminal, justified. Eligible for a reporter reward. */
    RESOLVED,
    /** Terminal, unjustified. */
    DENIED;

    public boolean isTerminal() {
        return this == RESOLVED || this == DENIED;
    }

    public boolean isPending() {
        return this == OPEN || this == CLAIMED;
    }

    public String getConfigName() {
        return this.name().toLowerCase(java.util.Locale.ROOT);
    }

    public static ReportStatus fromString(String raw) {
        return Utils.enumValueOf(raw, ReportStatus.class);
    }
}
