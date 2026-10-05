package su.nightexpress.sunlight.moduleImpl.profiles.config;

public class ProfilesPerms {

    public static final String MODULE  = "sunlight.profiles";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";
    public static final String ADMIN   = MODULE + ".admin";

    public static final String COMMAND_PROFILE        = COMMAND + ".profile";
    public static final String COMMAND_SWITCH         = COMMAND + ".switch";
    public static final String COMMAND_CREATE         = COMMAND + ".create";
    public static final String COMMAND_RENAME         = COMMAND + ".rename";
    public static final String COMMAND_DELETE         = COMMAND + ".delete";
    public static final String COMMAND_INFO           = COMMAND + ".info";
    public static final String COMMAND_LIST           = COMMAND + ".list";

    public static final String BYPASS_COOLDOWN = BYPASS + ".cooldown";
    public static final String BYPASS_SAFETY   = BYPASS + ".safety";

    private ProfilesPerms() {
    }
}
