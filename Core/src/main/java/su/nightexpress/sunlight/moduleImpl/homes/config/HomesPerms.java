package su.nightexpress.sunlight.moduleImpl.homes.config;

public class HomesPerms {

    public static final String MODULE  = "sunlight.homes";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_HOMES_ROOT          = COMMAND + ".homes.root";
    public static final String COMMAND_HOMES_DELETE        = COMMAND + ".homes.delete";
    public static final String COMMAND_HOMES_DELETE_OTHERS = COMMAND + ".homes.delete.others";
    public static final String COMMAND_HOMES_SET           = COMMAND + ".homes.set";
    public static final String COMMAND_HOMES_SET_OTHERS    = COMMAND + ".homes.set.others";
    public static final String COMMAND_HOMES_TELEPORT      = COMMAND + ".homes.teleport";
    public static final String COMMAND_HOMES_LIST          = COMMAND + ".homes.list";
    public static final String COMMAND_HOMES_LIST_OTHERS   = COMMAND + ".homes.list.others";
    public static final String COMMAND_HOMES_INVITE        = COMMAND + ".homes.invite";
    public static final String COMMAND_HOMES_VISIT         = COMMAND + ".homes.visit";
    public static final String COMMAND_HOMES_VISIT_ALL     = COMMAND + ".homes.visit.all";

    public static final String COMMAND_HOMES_ADMIN_ROOT = COMMAND + ".homes.admin";

    public static final String BYPASS_UNSAFE_LOCATION     = BYPASS + ".unsafe";
    public static final String BYPASS_CREATION_WORLDS     = BYPASS + ".creation.worlds";
    public static final String BYPASS_CREATION_PROTECTION = BYPASS + ".creation.protection";
    public static final String BYPASS_COST                = BYPASS + ".cost";
    public static final String BYPASS_COOLDOWN            = BYPASS + ".cooldown";

    private HomesPerms() {
    }
}
