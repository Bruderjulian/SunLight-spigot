package su.nightexpress.sunlight.moduleImpl.profiles.config;

public class ProfilesPerms {

    public static final String MODULE  = "sunlight.profiles";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";
    public static final String ADMIN   = MODULE + ".admin";

    public static final String COMMAND_PROFILE        = COMMAND + ".profile";
    public static final String COMMAND_SWITCH         = COMMAND + ".switch";
    public static final String COMMAND_CREATE         = COMMAND + ".create";
    public static final String COMMAND_CLONE          = COMMAND + ".clone";
    public static final String COMMAND_RENAME         = COMMAND + ".rename";
    public static final String COMMAND_ICON           = COMMAND + ".icon";
    public static final String COMMAND_DESCRIBE       = COMMAND + ".describe";
    public static final String COMMAND_DELETE         = COMMAND + ".delete";
    public static final String COMMAND_INFO           = COMMAND + ".info";
    public static final String COMMAND_LIST           = COMMAND + ".list";

    public static final String COMMAND_ADMIN          = COMMAND + ".admin";
    public static final String COMMAND_ADMIN_LIST     = COMMAND_ADMIN + ".list";
    public static final String COMMAND_ADMIN_SWITCH   = COMMAND_ADMIN + ".switch";
    public static final String COMMAND_ADMIN_DELETE   = COMMAND_ADMIN + ".delete";
    public static final String COMMAND_ADMIN_RESET    = COMMAND_ADMIN + ".reset";

    public static final String BYPASS_COOLDOWN = BYPASS + ".cooldown";
    public static final String BYPASS_SAFETY   = BYPASS + ".safety";
    public static final String BYPASS_WARMUP   = BYPASS + ".warmup";

    private ProfilesPerms() {
    }
}
