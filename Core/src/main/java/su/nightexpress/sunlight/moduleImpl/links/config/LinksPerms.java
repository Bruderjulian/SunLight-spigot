package su.nightexpress.sunlight.moduleImpl.links.config;

public class LinksPerms {

    public static final String MODULE  = "sunlight.links";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_LINKS_ROOT = COMMAND + ".links";
    public static final String COMMAND_LINKS_ALL  = COMMAND + ".links.all";
    public static final String COMMAND_LINK        = COMMAND + ".link";
    public static final String COMMAND_LINKS_STATS = COMMAND + ".links.stats";

    public static final String COMMAND_ADMIN_ROOT              = COMMAND + ".admin";
    public static final String COMMAND_ADMIN_CREATE            = COMMAND + ".admin.create";
    public static final String COMMAND_ADMIN_DELETE            = COMMAND + ".admin.delete";
    public static final String COMMAND_ADMIN_EDIT              = COMMAND + ".admin.edit";
    public static final String COMMAND_ADMIN_SET_URL           = COMMAND + ".admin.set_url";
    public static final String COMMAND_ADMIN_SET_NAME          = COMMAND + ".admin.set_name";
    public static final String COMMAND_ADMIN_SET_COMMAND       = COMMAND + ".admin.set_command";
    public static final String COMMAND_ADMIN_SET_EXECUTOR      = COMMAND + ".admin.set_executor";
    public static final String COMMAND_ADMIN_SET_PERMISSION    = COMMAND + ".admin.set_permission";
    public static final String COMMAND_ADMIN_SET_USE_PERMISSION = COMMAND + ".admin.set_use_permission";
    public static final String COMMAND_ADMIN_SET_COOLDOWN      = COMMAND + ".admin.set_cooldown";
    public static final String COMMAND_ADMIN_SET_COST          = COMMAND + ".admin.set_cost";
    public static final String COMMAND_ADMIN_SET_REWARD        = COMMAND + ".admin.set_reward";
    public static final String COMMAND_ADMIN_SET_SOUND         = COMMAND + ".admin.set_sound";
    public static final String COMMAND_ADMIN_SET_ACTIONBAR     = COMMAND + ".admin.set_actionbar";
    public static final String COMMAND_ADMIN_SET_PARTICLE      = COMMAND + ".admin.set_particle";
    public static final String COMMAND_ADMIN_SET_ICON          = COMMAND + ".admin.set_icon";
    public static final String COMMAND_ADMIN_SET_PRIORITY      = COMMAND + ".admin.set_priority";
    public static final String COMMAND_ADMIN_TOGGLE            = COMMAND + ".admin.toggle";
    public static final String COMMAND_ADMIN_RESET_CLICKS      = COMMAND + ".admin.reset_clicks";

    public static final String BYPASS_COOLDOWN = BYPASS + ".cooldown";
    public static final String BYPASS_COST     = BYPASS + ".cost";

    private LinksPerms() {
    }
}
