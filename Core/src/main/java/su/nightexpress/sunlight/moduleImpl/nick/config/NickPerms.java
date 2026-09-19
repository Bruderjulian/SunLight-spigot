package su.nightexpress.sunlight.moduleImpl.nick.config;

import org.bukkit.permissions.Permission;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;

public class NickPerms {

    public static final PermissionTree MODULE = Perms.detached("nick");
    public static final PermissionTree COMMAND = MODULE.branch("command");
    public static final PermissionTree BYPASS = MODULE.branch("bypass");

    public static final Permission COMMAND_NICK_ROOT = COMMAND.permission("nick.root");
    public static final Permission COMMAND_NICK_CHANGE = COMMAND.permission("nick.change");
    public static final Permission COMMAND_NICK_SET = COMMAND.permission("nick.set");
    public static final Permission COMMAND_NICK_CLEAR = COMMAND.permission("nick.clear");
    public static final Permission COMMAND_NICK_CLEAR_OTHERS = COMMAND.permission("nick.clear.others");
    public static final Permission COMMAND_NICK_COLORS = COMMAND.permission("nick.colors");

    public static final Permission BYPASS_NICK_WORDS = BYPASS.permission("nick.words");
    public static final Permission BYPASS_NICK_REGEX = BYPASS.permission("nick.regex");
    public static final Permission BYPASS_NICK_LENGTH = BYPASS.permission("nick.length");
}
