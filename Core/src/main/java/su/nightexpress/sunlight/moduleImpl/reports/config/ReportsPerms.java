package su.nightexpress.sunlight.moduleImpl.reports.config;

import org.bukkit.permissions.Permission;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;

public class ReportsPerms {

    public static final PermissionTree ROOT    = Perms.detached("reports");
    public static final PermissionTree COMMAND = ROOT.branch("command");
    public static final PermissionTree BYPASS  = ROOT.branch("bypass");

    public static final Permission COMMAND_REPORT = COMMAND.permission("report");
    public static final Permission COMMAND_REPORT_STATUS = COMMAND.permission("report.status");
    public static final Permission COMMAND_REPORT_DUPLICATE = COMMAND.permission("report.duplicate");

    public static final Permission COMMAND_REPORTS = COMMAND.permission("reports");

    public static final Permission COMMAND_REPORT_CLAIM = COMMAND.permission("report.claim");
    public static final Permission COMMAND_REPORT_RESOLVE = COMMAND.permission("report.resolve");
    public static final Permission COMMAND_REPORT_DENY = COMMAND.permission("report.deny");
    public static final Permission COMMAND_REPORT_NOTE = COMMAND.permission("report.note");
    public static final Permission COMMAND_REPORT_TELEPORT = COMMAND.permission("report.teleport");
    public static final Permission COMMAND_REPORT_TELEPORT_BYPASS_WARMUP = COMMAND
            .permission("report.teleport.bypass-warmup");
    public static final Permission COMMAND_REPORT_DELETE = COMMAND.permission("report.delete");

    public static final Permission NOTIFY = ROOT.permission("notify");
    public static final Permission EXEMPT = ROOT.permission("exempt");

    public static final Permission BYPASS_COOLDOWN = BYPASS.permission("cooldown");
    public static final Permission BYPASS_COST = BYPASS.permission("cost");

    public static final Permission ADMIN = ROOT.permission("admin");

    /**
     * Per-category permissions are read from each category's own {@code Permission} field at check
     * time rather than declared here, because the set of categories is user-defined and a static
     * tree cannot describe it.
     */
    public static String categoryPermission(String id) {
        return "sunlight.reports.category." + id;
    }

    private ReportsPerms() {
    }
}
