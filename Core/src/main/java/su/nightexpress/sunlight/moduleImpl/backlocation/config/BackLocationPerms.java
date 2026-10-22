package su.nightexpress.sunlight.moduleImpl.backlocation.config;

public class BackLocationPerms {

    public static final String MODULE  = "sunlight.backlocation";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_BACK             = COMMAND + ".back";
    public static final String COMMAND_BACK_OTHERS      = COMMAND + ".back.others";
    public static final String COMMAND_DEATHBACK        = COMMAND + ".deathback";
    public static final String COMMAND_DEATHBACK_OTHERS = COMMAND + ".deathback.others";

    public static final String BYPASS_PREVIOUS_WORLDS = BYPASS + ".previous.worlds";
    public static final String BYPASS_PREVIOUS_CAUSES = BYPASS + ".previous.causes";
    public static final String BYPASS_DEATH_WORLDS    = BYPASS + ".death.worlds";
    public static final String BYPASS_COST            = BYPASS + ".cost";
    public static final String BYPASS_COOLDOWN        = BYPASS + ".cooldown";

    private BackLocationPerms() {
    }
}
