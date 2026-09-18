package su.nightexpress.sunlight.moduleImpl.rtp.config;

import org.bukkit.permissions.Permission;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;

public class RTPPerms {

    public static final PermissionTree MODULE = Perms.detached("rtp");
    public static final PermissionTree COMMAND = MODULE.branch("command");
    public static final PermissionTree BYPASS = MODULE.branch("bypass");

    public static final Permission COMMAND_RTP = COMMAND.permission("rtp");

    public static final Permission BYPASS_COST = BYPASS.permission("cost");
    public static final Permission BYPASS_COOLDOWN = BYPASS.permission("cooldown");
}
