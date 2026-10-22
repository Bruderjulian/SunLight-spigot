package su.nightexpress.sunlight.moduleImpl.nick.config;

public class NickPerms {

    public static final String MODULE  = "sunlight.nick";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_NICK_ROOT          = COMMAND + ".nick.root";
    public static final String COMMAND_NICK_CHANGE        = COMMAND + ".nick.change";
    public static final String COMMAND_NICK_SET           = COMMAND + ".nick.set";
    public static final String COMMAND_NICK_CLEAR         = COMMAND + ".nick.clear";
    public static final String COMMAND_NICK_CLEAR_OTHERS  = COMMAND + ".nick.clear.others";
    public static final String COMMAND_NICK_COLORS        = COMMAND + ".nick.colors";

    public static final String BYPASS_NICK_WORDS    = BYPASS + ".nick.words";
    public static final String BYPASS_NICK_REGEX    = BYPASS + ".nick.regex";
    public static final String BYPASS_NICK_LENGTH   = BYPASS + ".nick.length";

    public static final String BYPASS_CHANGE_COST     = BYPASS + ".change.cost";
    public static final String BYPASS_CHANGE_COOLDOWN = BYPASS + ".change.cooldown";

    private NickPerms() {
    }
}
