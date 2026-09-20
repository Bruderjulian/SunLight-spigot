package su.nightexpress.sunlight.moduleImpl.glow.config;

import org.bukkit.permissions.Permission;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;

public class GlowPerms {

    public static final PermissionTree MODULE = Perms.detached("glow");
    public static final PermissionTree COMMAND = MODULE.branch("command");
    public static final PermissionTree COLOR = MODULE.branch("color");
    public static final PermissionTree BYPASS = MODULE.branch("bypass");

    public static final Permission COMMAND_GLOW_ROOT = COMMAND.permission("glow.root");
    public static final Permission COMMAND_GLOW_SET = COMMAND.permission("glow.set");
    public static final Permission COMMAND_GLOW_SET_OTHERS = COMMAND.permission("glow.set.others");
    public static final Permission COMMAND_GLOW_CLEAR = COMMAND.permission("glow.clear");
    public static final Permission COMMAND_GLOW_CLEAR_OTHERS = COMMAND.permission("glow.clear.others");
    public static final Permission COMMAND_GLOW_LIST = COMMAND.permission("glow.list");

    public static final Permission BYPASS_COLOR = BYPASS.permission("color");

    public static String colorNode(String effectId) {
        return COLOR.childrenNode(effectId);
    }

    public static boolean hasColorAccess(org.bukkit.command.CommandSender sender, String effectId) {
        if (sender.hasPermission(BYPASS_COLOR)) return true;
        return COLOR.hasChildAccess(sender, effectId) || sender.hasPermission(COLOR.childrenNode("*"));
    }
}
