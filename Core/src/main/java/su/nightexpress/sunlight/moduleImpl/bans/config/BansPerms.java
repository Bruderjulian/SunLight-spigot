package su.nightexpress.sunlight.moduleImpl.bans.config;

public class BansPerms {

    public static final String MODULE  = "sunlight.bans";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_ALTS         = COMMAND + ".alts";
    public static final String COMMAND_KICK         = COMMAND + ".kick";
    public static final String COMMAND_BAN          = COMMAND + ".ban";
    public static final String COMMAND_BAN_IP       = COMMAND + ".banip";
    public static final String COMMAND_MUTE         = COMMAND + ".mute";
    public static final String COMMAND_WARN         = COMMAND + ".warn";
    public static final String COMMAND_UNBAN        = COMMAND + ".unban";
    public static final String COMMAND_UNBAN_IP     = COMMAND + ".unbanip";
    public static final String COMMAND_UNMUTE       = COMMAND + ".unmute";
    public static final String COMMAND_UNWARN       = COMMAND + ".unwarn";
    public static final String COMMAND_BAN_HISTORY  = COMMAND + ".banhistory";
    public static final String COMMAND_MUTE_HISTORY = COMMAND + ".mutehistory";
    public static final String COMMAND_WARN_HISTORY = COMMAND + ".warnhistory";
    public static final String COMMAND_BAN_LIST     = COMMAND + ".banlist";
    public static final String COMMAND_MUTE_LIST    = COMMAND + ".mutelist";
    public static final String COMMAND_WARN_LIST    = COMMAND + ".warnlist";

    public static final String ALTS_NOTIFY = MODULE + ".alts.notify";

    public static final String PUNISHMENT_DELETE = MODULE + ".punishment.delete";
    public static final String PUNISHMENT_TOGGLE = MODULE + ".punishment.toggle";

    public static final String BYPASS_DURATION_LIMIT = BYPASS + ".duration.limit";
    public static final String BYPASS_ALT_DETECTION  = BYPASS + ".alts.detection";

    private BansPerms() {
    }
}
