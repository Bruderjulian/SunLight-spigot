package su.nightexpress.sunlight.moduleImpl.rtp.config;

import su.nightexpress.sunlight.utils.Utils;

public class RTPPerms {

    public static final String MODULE  = "sunlight.rtp";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String COMMAND_RTP        = COMMAND + ".rtp";
    public static final String COMMAND_RTP_OTHERS = COMMAND + ".rtp.others";

    public static final String BYPASS_COST     = BYPASS + ".cost";
    public static final String BYPASS_COOLDOWN = BYPASS + ".cooldown";

    /**
     * Per-world permission of the {@code /rtp} command.
     */
    public static String world(final String worldName) {
        return COMMAND_RTP + "." + Utils.lowercase(worldName);
    }

    private RTPPerms() {
    }
}
