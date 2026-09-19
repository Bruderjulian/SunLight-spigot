package su.nightexpress.sunlight.hook;

import su.nightexpress.sunlight.utils.Utils;

public class HookId {

    public static final String PROTOCOL_LIB = "ProtocolLib";
    public static final String PACKET_EVENTS = "packetevents";
    public static final String DISCORD_SRV = "DiscordSRV";
    public static final String WORLD_GUARD = "WorldGuard";
    public static final String GRIEF_PREVENTION = "GriefPrevention";

    public static boolean hasDiscordSRV() {
        return Utils.isInstalled(DISCORD_SRV);
    }

    public static boolean hasWorldGuard() {
        return Utils.isInstalled(WORLD_GUARD);
    }

    public static boolean hasGriefPrevention() {
        return Utils.isInstalled(GRIEF_PREVENTION);
    }
}
