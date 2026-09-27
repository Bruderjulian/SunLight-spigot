package su.nightexpress.sunlight.moduleImpl.links.config;

import org.bukkit.permissions.Permission;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;

public class LinksPerms {

    public static final PermissionTree MODULE  = Perms.detached("links");
    public static final PermissionTree COMMAND = MODULE.branch("command");

    public static final Permission COMMAND_LINKS_ROOT = COMMAND.permission("links");
    public static final Permission COMMAND_LINKS_ALL  = COMMAND.permission("links.all");
    public static final Permission COMMAND_LINK        = COMMAND.permission("link");
    public static final Permission COMMAND_LINKS_STATS = COMMAND.permission("links.stats");

    public static final Permission BYPASS_COOLDOWN = MODULE.branch("bypass").permission("cooldown");
    public static final Permission BYPASS_COST     = MODULE.branch("bypass").permission("cost");

    public static final Permission COMMAND_ADMIN_ROOT           = COMMAND.permission("admin");
    public static final Permission COMMAND_ADMIN_CREATE         = COMMAND.permission("admin.create");
    public static final Permission COMMAND_ADMIN_DELETE         = COMMAND.permission("admin.delete");
    public static final Permission COMMAND_ADMIN_EDIT           = COMMAND.permission("admin.edit");
    public static final Permission COMMAND_ADMIN_SET_URL        = COMMAND.permission("admin.set_url");
    public static final Permission COMMAND_ADMIN_SET_NAME       = COMMAND.permission("admin.set_name");
    public static final Permission COMMAND_ADMIN_SET_COMMAND    = COMMAND.permission("admin.set_command");
    public static final Permission COMMAND_ADMIN_SET_EXECUTOR   = COMMAND.permission("admin.set_executor");
    public static final Permission COMMAND_ADMIN_SET_PERMISSION = COMMAND.permission("admin.set_permission");
    public static final Permission COMMAND_ADMIN_SET_USE_PERMISSION = COMMAND.permission("admin.set_use_permission");
    public static final Permission COMMAND_ADMIN_SET_COOLDOWN = COMMAND.permission("admin.set_cooldown");
    public static final Permission COMMAND_ADMIN_SET_COST     = COMMAND.permission("admin.set_cost");
    public static final Permission COMMAND_ADMIN_SET_REWARD   = COMMAND.permission("admin.set_reward");
    public static final Permission COMMAND_ADMIN_SET_SOUND    = COMMAND.permission("admin.set_sound");
    public static final Permission COMMAND_ADMIN_SET_ACTIONBAR = COMMAND.permission("admin.set_actionbar");
    public static final Permission COMMAND_ADMIN_SET_PARTICLE = COMMAND.permission("admin.set_particle");
    public static final Permission COMMAND_ADMIN_SET_ICON       = COMMAND.permission("admin.set_icon");
    public static final Permission COMMAND_ADMIN_SET_PRIORITY   = COMMAND.permission("admin.set_priority");
    public static final Permission COMMAND_ADMIN_TOGGLE         = COMMAND.permission("admin.toggle");
    public static final Permission COMMAND_ADMIN_RESET_CLICKS   = COMMAND.permission("admin.reset_clicks");
}
