package su.nightexpress.sunlight.config;

import org.bukkit.permissions.Permission;

public class Perms {

    public static final PermissionTree ROOT = PermissionTree.root("sunlight");
    public static final PermissionTree ROOT_COMMAND = ROOT.branch("command");
    public static final PermissionTree ROOT_BYPASS = ROOT.branch("bypass");

    public static final Permission COMMAND_RELOAD = ROOT_COMMAND.permission("reload");

    public static final Permission BYPASS_COST = ROOT_BYPASS.permission("cost");
    public static final Permission BYPASS_COOLDOWN = ROOT_BYPASS.permission("cooldown");

    public static PermissionTree detached(String name) {
        return ROOT.detached(name);
    }
}
