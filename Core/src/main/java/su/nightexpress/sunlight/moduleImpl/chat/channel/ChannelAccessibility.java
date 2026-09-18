package su.nightexpress.sunlight.moduleImpl.chat.channel;

import su.nightexpress.nightcore.util.rankmap.IntRankMap;

public record ChannelAccessibility(boolean autoJoin, boolean permissionToListen, boolean permissionToSpeak,
        IntRankMap messageCooldowns, String cooldownMessage) {

}
