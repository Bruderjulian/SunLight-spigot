package su.nightexpress.sunlight.hook;

import su.nightexpress.nightcore.util.Plugins;

public class HookId {

    public static final String PROTOCOL_LIB = "ProtocolLib";
    public static final String PACKET_EVENTS = "packetevents";
    public static final String DISCORD_SRV = "DiscordSRV";
    public static final String WORLD_GUARD = "WorldGuard";
    public static final String GRIEF_PREVENTION = "GriefPrevention";

    public static boolean hasDiscordSRV() {
        return Plugins.isInstalled(DISCORD_SRV);
    }

    public static boolean hasWorldGuard() {
        return Plugins.isInstalled(WORLD_GUARD);
    }

    public static boolean hasGriefPrevention() {
        return Plugins.isInstalled(GRIEF_PREVENTION);
    }
}
