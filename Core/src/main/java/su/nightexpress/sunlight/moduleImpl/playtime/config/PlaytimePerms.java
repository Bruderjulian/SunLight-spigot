package su.nightexpress.sunlight.moduleImpl.playtime.config;

public class PlaytimePerms {

    public static final String MODULE  = "sunlight.playtime";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_PLAYTIME        = COMMAND + ".playtime";
    public static final String COMMAND_STATS           = COMMAND + ".stats";
    public static final String COMMAND_STATS_OTHERS    = COMMAND + ".stats.others";
    public static final String COMMAND_TOP             = COMMAND + ".top";
    public static final String COMMAND_GOAL            = COMMAND + ".goal";
    public static final String COMMAND_GOAL_SET        = COMMAND + ".goal.set";
    public static final String COMMAND_GOAL_SET_OTHERS = COMMAND + ".goal.set.others";

    public static final String ADMIN = MODULE + ".admin";

    private PlaytimePerms() {
    }
}
