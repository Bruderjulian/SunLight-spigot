package su.nightexpress.sunlight.hook;

import su.nightexpress.sunlight.utils.Utils;

public class HookId {

    public static final String PROTOCOL_LIB = "ProtocolLib";
    public static final String PACKET_EVENTS = "packetevents";
    public static final String DISCORD_SRV = "DiscordSRV";
    public static final String WORLD_GUARD = "WorldGuard";
    public static final String GRIEF_PREVENTION = "GriefPrevention";

    public static final String TAB = "TAB";
    public static final String TAB_BRIDGE = "TAB-Bridge";
    public static final String ULTIMATE_TEAMS = "UltimateTeams";
    public static final String LUCKPERMS = "LuckPerms";

    public static boolean hasDiscordSRV() {
        return Utils.isInstalled(DISCORD_SRV);
    }

    public static boolean hasWorldGuard() {
        return Utils.isInstalled(WORLD_GUARD);
    }

    public static boolean hasGriefPrevention() {
        return Utils.isInstalled(GRIEF_PREVENTION);
    }

    public static boolean hasTAB() {
        return Utils.isLoaded(TAB);
    }

    /**
     * Whether TAB-Bridge is present, which means TAB is rendering nametags on the proxy
     * rather than here. The two are mutually exclusive in practice: Bridge only does
     * anything when there is a TAB on the proxy to talk to.
     */
    public static boolean hasTabBridge() {
        return Utils.isLoaded(TAB_BRIDGE);
    }

    public static boolean hasUltimateTeams() {
        return Utils.isLoaded(ULTIMATE_TEAMS);
    }

    public static boolean hasLuckPerms() {
        return Utils.isLoaded(LUCKPERMS);
    }
}
