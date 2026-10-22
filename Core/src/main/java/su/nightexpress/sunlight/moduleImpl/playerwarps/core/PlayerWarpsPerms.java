package su.nightexpress.sunlight.moduleImpl.playerwarps.core;

public class PlayerWarpsPerms {

    public static final String MODULE  = "sunlight.playerwarps";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";
    public static final String OPTION  = MODULE + ".option";

    public static final String COMMAND_WARPS_ROOT        = COMMAND + ".warps.root";
    public static final String COMMAND_WARPS_CREATE      = COMMAND + ".warps.create";
    public static final String COMMAND_WARPS_UPDATE      = COMMAND + ".warps.update";
    public static final String COMMAND_WARPS_DELETE      = COMMAND + ".warps.delete";
    public static final String COMMAND_WARPS_JUMP        = COMMAND + ".warps.teleport";
    public static final String COMMAND_WARPS_JUMP_OTHERS = COMMAND + ".warps.teleport.others";
    public static final String COMMAND_WARPS_LIST        = COMMAND + ".warps.list";
    public static final String COMMAND_WARPS_LIST_OTHERS = COMMAND + ".warps.list.others";

    public static final String OPTION_PRICE = OPTION + ".price";

    public static final String BYPASS_CREATION_WORLD = BYPASS + ".creation.world";
    public static final String BYPASS_PRICE          = BYPASS + ".price";
    public static final String BYPASS_OWNERSHIP      = BYPASS + ".ownership";
    public static final String BYPASS_COST           = BYPASS + ".cost";
    public static final String BYPASS_COOLDOWN       = BYPASS + ".cooldown";

    private PlayerWarpsPerms() {
    }
}
