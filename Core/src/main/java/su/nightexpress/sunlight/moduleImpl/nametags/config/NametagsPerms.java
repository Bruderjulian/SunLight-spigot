package su.nightexpress.sunlight.moduleImpl.nametags.config;

import org.bukkit.command.CommandSender;

import su.nightexpress.sunlight.utils.PermissionUtils;

public class NametagsPerms {

    public static final String MODULE  = "sunlight.nametags";
    public static final String COMMAND = MODULE + ".command";
    public static final String RANK    = MODULE + ".rank";
    public static final String TAG     = MODULE + ".tag";
    public static final String PROFILE = MODULE + ".profile";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_TAGS           = COMMAND + ".tags";
    public static final String COMMAND_TAG_SELECT     = COMMAND + ".tag.select";
    public static final String COMMAND_PROFILE_SELECT = COMMAND + ".profile.select";
    public static final String COMMAND_PROFILES       = COMMAND + ".profiles";
    public static final String COMMAND_NAMETAG_TOGGLE = COMMAND + ".rank.toggle";
    public static final String COMMAND_TAG_TOGGLE     = COMMAND + ".tag.toggle";
    public static final String COMMAND_TEAM_TOGGLE    = COMMAND + ".team.toggle";
    public static final String COMMAND_HIDE           = COMMAND + ".hide";
    public static final String COMMAND_NAMETAG_SET    = COMMAND + ".set";
    public static final String COMMAND_GRANT          = COMMAND + ".grant";
    public static final String COMMAND_RELOAD         = COMMAND + ".reload";

    public static final String ADMIN = MODULE + ".admin";

    public static final String BYPASS_COST   = BYPASS + ".cost";
    public static final String BYPASS_ACCESS = BYPASS + ".access";

    public static boolean hasRankAccess(CommandSender sender, String rankId) {
        return PermissionUtils.hasChildAccess(sender, RANK, rankId);
    }

    public static boolean hasTagAccess(CommandSender sender, String tagId) {
        return PermissionUtils.hasChildAccess(sender, TAG, tagId);
    }

    public static boolean hasProfileAccess(CommandSender sender, String profileId) {
        return PermissionUtils.hasChildAccess(sender, PROFILE, profileId);
    }

    private NametagsPerms() {
    }
}
