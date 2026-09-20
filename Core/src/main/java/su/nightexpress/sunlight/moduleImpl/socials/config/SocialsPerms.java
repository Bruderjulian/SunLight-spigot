package su.nightexpress.sunlight.moduleImpl.socials.config;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;

public class SocialsPerms {

    public static final PermissionTree MODULE = Perms.detached("socials");
    public static final PermissionTree COMMAND = MODULE.branch("command");

    public static final Permission COMMAND_SOCIALS = COMMAND.permission("socials");
    public static final Permission COMMAND_LINK = COMMAND.permission("link");

    public static final Permission ADMIN = MODULE.permission("admin");

    public static boolean hasLinkAccess(CommandSender sender, String linkPermission, String id) {
        if (linkPermission != null && !linkPermission.isBlank() && !sender.hasPermission(linkPermission)) {
            return false;
        }
        return sender.hasPermission(COMMAND_LINK) || sender.hasPermission(COMMAND.childrenNode("link." + id));
    }
}
