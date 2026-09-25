package su.nightexpress.sunlight.moduleImpl.reports.model;

public enum CaseStatus {
    /** Reports against this player are coming in and nobody has taken responsibility yet. */
    OPEN,
    /** A staff member is working the case. */
    CLAIMED,
    /** Terminal, the offender's situation was addressed. */
    RESOLVED,
    /** Terminal, nothing came of it. */
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
}
