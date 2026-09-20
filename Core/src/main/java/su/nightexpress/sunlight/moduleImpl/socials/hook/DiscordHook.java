package su.nightexpress.sunlight.moduleImpl.socials.hook;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import su.nightexpress.nightcore.util.text.NightMessage;
import su.nightexpress.sunlight.hook.HookId;

public class DiscordHook {

    private final String defaultChannelId;

    public DiscordHook(String defaultChannelId) {
        this.defaultChannelId = defaultChannelId == null ? "" : defaultChannelId;
    }

    public boolean available() {
        return HookId.hasDiscordSRV();
    }

    public static String strip(String text) {
        return text == null ? "" : NightMessage.stripTags(text);
    }

    public void sendToChannel(String channelIdOrEmpty, String plainText) {
        if (plainText == null || plainText.isBlank()) return;
        if (!this.available()) return;
        try {
            this.sendInternal(channelIdOrEmpty, plainText);
        } catch (NoClassDefFoundError | Exception exception) {
            exception.printStackTrace();
        }
    }

    private void sendInternal(String channelIdOrEmpty, String plainText) {
        if (!DiscordSRV.isReady) return;

        TextChannel channel = null;

        String id = (channelIdOrEmpty == null || channelIdOrEmpty.isBlank()) ? this.defaultChannelId : channelIdOrEmpty;
        if (!id.isBlank()) {
            try {
                if (DiscordUtil.getJda() != null) {
                    channel = DiscordUtil.getJda().getTextChannelById(id);
                }
            } catch (Exception ignored) {
            }
        }
        if (channel == null) {
            channel = DiscordSRV.getPlugin().getMainTextChannel();
        }
        if (channel == null) return;

        DiscordUtil.sendMessage(channel, plainText);
    }
}
