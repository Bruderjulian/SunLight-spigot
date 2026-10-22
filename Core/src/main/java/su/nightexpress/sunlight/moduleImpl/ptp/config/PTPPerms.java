package su.nightexpress.sunlight.moduleImpl.ptp.config;

public class PTPPerms {

    public static final String MODULE  = "sunlight.ptp";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_ROOT            = COMMAND + ".root";
    public static final String COMMAND_ACCEPT          = COMMAND + ".accept";
    public static final String COMMAND_DECLINE         = COMMAND + ".decline";
    public static final String COMMAND_INVITE          = COMMAND + ".invite";
    public static final String COMMAND_REQUEST         = COMMAND + ".request";
    public static final String COMMAND_REQUESTS        = COMMAND + ".toggle";
    public static final String COMMAND_REQUESTS_OTHERS = COMMAND + ".toggle.others";

    public static final String BYPASS_REQUESTS_DISABLED = BYPASS + ".requests.disabled";
    public static final String BYPASS_COST              = BYPASS + ".cost";
    public static final String BYPASS_COOLDOWN          = BYPASS + ".cooldown";

    private PTPPerms() {
    }
}
