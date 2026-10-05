package su.nightexpress.sunlight.moduleImpl.spawns.config;

public class SpawnsPerms {

    public static final String MODULE  = "sunlight.spawns";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";
    public static final String SPAWN   = MODULE + ".spawn";

    public static final String COMMAND_SPAWNS_CREATE          = COMMAND + ".spawns.create";
    public static final String COMMAND_SPAWNS_DELETE          = COMMAND + ".spawns.delete";
    public static final String COMMAND_SPAWNS_TELEPORT        = COMMAND + ".spawns.teleport";
    public static final String COMMAND_SPAWNS_TELEPORT_OTHERS = COMMAND + ".spawns.teleport.others";
    public static final String COMMAND_SPAWNS_EDITOR          = COMMAND + ".spawns.editor";

    public static final String BYPASS_COST     = BYPASS + ".cost";
    public static final String BYPASS_COOLDOWN = BYPASS + ".cooldown";

    private SpawnsPerms() {
    }
}
