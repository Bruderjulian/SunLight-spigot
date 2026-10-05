package su.nightexpress.sunlight.moduleImpl.reports.config;

public class ReportsPerms {

    public static final String MODULE  = "sunlight.reports";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_REPORT          = COMMAND + ".report";
    public static final String COMMAND_REPORT_STATUS   = COMMAND + ".report.status";
    public static final String COMMAND_REPORT_DUPLICATE = COMMAND + ".report.duplicate";

    public static final String COMMAND_REPORTS = COMMAND + ".reports";

    public static final String COMMAND_REPORT_CLAIM   = COMMAND + ".report.claim";
    public static final String COMMAND_REPORT_RESOLVE = COMMAND + ".report.resolve";
    public static final String COMMAND_REPORT_DENY    = COMMAND + ".report.deny";
    public static final String COMMAND_REPORT_NOTE    = COMMAND + ".report.note";
    public static final String COMMAND_REPORT_TELEPORT = COMMAND + ".report.teleport";
    public static final String COMMAND_REPORT_TELEPORT_BYPASS_WARMUP = COMMAND + ".report.teleport.bypass-warmup";
    public static final String COMMAND_REPORT_DELETE  = COMMAND + ".report.delete";
    public static final String COMMAND_REPORTS_STATS  = COMMAND + ".reports.stats";
    public static final String COMMAND_REPORT_TOGGLE  = COMMAND + ".report.toggle";

    public static final String NOTIFY = MODULE + ".notify";
    public static final String EXEMPT = MODULE + ".exempt";

    public static final String BYPASS_COOLDOWN   = BYPASS + ".cooldown";
    public static final String BYPASS_COST       = BYPASS + ".cost";
    public static final String BYPASS_DAILY_LIMIT = BYPASS + ".daily-limit";

    public static final String ADMIN = MODULE + ".admin";

    /**
     * Per-category permissions are read from each category's own {@code Permission} field at check
     * time rather than declared here, because the set of categories is user-defined and a static
     * table cannot describe it.
     */
    public static String category(String id) {
        return MODULE + ".category." + id;
    }

    private ReportsPerms() {
    }
}
