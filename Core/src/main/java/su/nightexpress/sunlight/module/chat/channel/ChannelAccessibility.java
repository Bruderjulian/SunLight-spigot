package su.nightexpress.sunlight.module.chat.channel;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.rankmap.IntRankMap;

public record ChannelAccessibility(boolean autoJoin, boolean permissionToListen, boolean permissionToSpeak, @NotNull IntRankMap messageCooldowns, @NotNull String cooldownMessage) {

}
