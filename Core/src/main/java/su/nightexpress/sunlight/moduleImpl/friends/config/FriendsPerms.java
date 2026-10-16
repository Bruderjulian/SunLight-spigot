package su.nightexpress.sunlight.moduleImpl.friends.config;

public class FriendsPerms {

    public static final String MODULE  = "sunlight.friends";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_FRIENDS_ROOT      = COMMAND + ".friends.root";
    public static final String COMMAND_FRIENDS_ADD       = COMMAND + ".friends.add";
    public static final String COMMAND_FRIENDS_ADD_OTHERS = COMMAND + ".friends.add.others";
    public static final String COMMAND_FRIENDS_REMOVE    = COMMAND + ".friends.remove";
    public static final String COMMAND_FRIENDS_REMOVE_OTHERS = COMMAND + ".friends.remove.others";
    public static final String COMMAND_FRIENDS_ACCEPT    = COMMAND + ".friends.accept";
    public static final String COMMAND_FRIENDS_DECLINE   = COMMAND + ".friends.decline";
    public static final String COMMAND_FRIENDS_CANCEL    = COMMAND + ".friends.cancel";
    public static final String COMMAND_FRIENDS_LIST      = COMMAND + ".friends.list";
    public static final String COMMAND_FRIENDS_LIST_OTHERS = COMMAND + ".friends.list.others";
    public static final String COMMAND_FRIENDS_PENDING   = COMMAND + ".friends.pending";
    public static final String COMMAND_FRIENDS_BLOCKED   = COMMAND + ".friends.blocked";
    public static final String COMMAND_FRIENDS_BLOCK     = COMMAND + ".friends.block";
    public static final String COMMAND_FRIENDS_UNBLOCK   = COMMAND + ".friends.unblock";
    public static final String COMMAND_FRIENDS_TOGGLE    = COMMAND + ".friends.toggle";
    public static final String COMMAND_FRIENDS_TOGGLE_OTHERS = COMMAND + ".friends.toggle.others";
    public static final String COMMAND_FRIENDS_JUMP      = COMMAND + ".friends.jump";
    public static final String COMMAND_FRIENDS_MESSAGE   = COMMAND + ".friends.message";
    public static final String COMMAND_FRIENDS_FAVORITE  = COMMAND + ".friends.favorite";

    public static final String COMMAND_FRIENDS_ADMIN_ROOT = COMMAND + ".friends.admin";

    public static final String BYPASS_REQUESTS_DISABLED = BYPASS + ".requests.disabled";
    public static final String BYPASS_LIMITS            = BYPASS + ".limits";
    public static final String BYPASS_COST              = BYPASS + ".cost";
    public static final String BYPASS_COOLDOWN          = BYPASS + ".cooldown";
    public static final String BYPASS_WORLD_BLACKLIST   = BYPASS + ".worlds";

    private FriendsPerms() {
    }
}