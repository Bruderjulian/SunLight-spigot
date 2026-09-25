package su.nightexpress.sunlight.moduleImpl.nametags.config;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;

public class NametagsPerms {

    public static final PermissionTree MODULE = Perms.detached("nametags");
    public static final PermissionTree COMMAND = MODULE.branch("command");
    public static final PermissionTree RANK = MODULE.branch("rank");
    public static final PermissionTree TAG = MODULE.branch("tag");
    public static final PermissionTree PROFILE = MODULE.branch("profile");
    public static final PermissionTree BYPASS = MODULE.branch("bypass");

    public static final Permission COMMAND_TAGS = COMMAND.permission("tags");
    public static final Permission COMMAND_TAG_SELECT = COMMAND.permission("tag.select");
    public static final Permission COMMAND_PROFILE_SELECT = COMMAND.permission("profile.select");
    public static final Permission COMMAND_PROFILES = COMMAND.permission("profiles");
    public static final Permission COMMAND_NAMETAG_TOGGLE = COMMAND.permission("rank.toggle");
    public static final Permission COMMAND_TAG_TOGGLE = COMMAND.permission("tag.toggle");
    public static final Permission COMMAND_TEAM_TOGGLE = COMMAND.permission("team.toggle");
    public static final Permission COMMAND_HIDE = COMMAND.permission("hide");
    public static final Permission COMMAND_NAMETAG_SET = COMMAND.permission("set");
    public static final Permission COMMAND_GRANT = COMMAND.permission("grant");
    public static final Permission COMMAND_RELOAD = COMMAND.permission("reload");
    public static final Permission ADMIN = MODULE.permission("admin");

    public static final Permission BYPASS_COST = BYPASS.permission("cost");
    public static final Permission BYPASS_ACCESS = BYPASS.permission("access");

    public static boolean hasRankAccess(CommandSender sender, String rankId) {
        return RANK.hasChildAccess(sender, rankId);
    }

    public static boolean hasTagAccess(CommandSender sender, String tagId) {
        return TAG.hasChildAccess(sender, tagId);
    }

    public static boolean hasProfileAccess(CommandSender sender, String profileId) {
        return PROFILE.hasChildAccess(sender, profileId);
    }
}
