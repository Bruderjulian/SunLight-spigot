package su.nightexpress.sunlight.moduleImpl.freeze.config;

import org.bukkit.permissions.Permission;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;

public class FreezePerms {

    public static final PermissionTree MODULE = Perms.detached("freeze");
    public static final PermissionTree COMMAND = MODULE.branch("command");
    public static final PermissionTree BYPASS = MODULE.branch("bypass");

    public static final Permission COMMAND_FREEZE = COMMAND.permission("freeze");
    public static final Permission COMMAND_FREEZE_OTHERS = COMMAND.permission("freeze.others");

    public static final Permission BYPASS_IMMUNE = BYPASS.permission("immune");
}
