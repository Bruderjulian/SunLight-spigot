package su.nightexpress.sunlight.moduleImpl.playtime.config;

import org.bukkit.permissions.Permission;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;

public class PlaytimePerms {

    public static final PermissionTree MODULE = Perms.detached("playtime");
    public static final PermissionTree COMMAND = MODULE.branch("command");

    public static final Permission COMMAND_PLAYTIME = COMMAND.permission("playtime");
    public static final Permission COMMAND_STATS = COMMAND.permission("stats");
    public static final Permission COMMAND_STATS_OTHERS = COMMAND.permission("stats.others");
    public static final Permission COMMAND_TOP = COMMAND.permission("top");
    public static final Permission COMMAND_GOAL = COMMAND.permission("goal");
    public static final Permission COMMAND_GOAL_SET = COMMAND.permission("goal.set");

    public static final Permission ADMIN = MODULE.permission("admin");
}