package su.nightexpress.sunlight.moduleImpl.warps.core;

public class WarpsPerms {

    public static final String MODULE  = "sunlight.warps";
    public static final String COMMAND = MODULE + ".command";
    public static final String WARP    = MODULE + ".warp";
    public static final String BYPASS  = MODULE + ".bypass";

    public static final String EDITOR = MODULE + ".editor";

    public static final String COMMAND_WARPS_ROOT        = COMMAND + ".warps.root";
    public static final String COMMAND_WARPS_CREATE      = COMMAND + ".warps.create";
    public static final String COMMAND_WARPS_UPDATE      = COMMAND + ".warps.update";
    public static final String COMMAND_WARPS_DELETE      = COMMAND + ".warps.delete";
    public static final String COMMAND_WARPS_JUMP        = COMMAND + ".warps.teleport";
    public static final String COMMAND_WARPS_JUMP_OTHERS = COMMAND + ".warps.teleport.others";
    public static final String COMMAND_WARPS_LIST        = COMMAND + ".warps.list";
    public static final String COMMAND_WARPS_LIST_OTHERS = COMMAND + ".warps.list.others";
    public static final String COMMAND_WARPS_EDIT        = COMMAND + ".warps.edit";

    public static final String BYPASS_COST     = BYPASS + ".cost";
    public static final String BYPASS_COOLDOWN = BYPASS + ".cooldown";

    private WarpsPerms() {
    }
}
