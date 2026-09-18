package su.nightexpress.sunlight.moduleImpl.chat.channel;

import su.nightexpress.nightcore.util.LowerCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ChannelRepository {

    private final Map<String, ChatChannel> channelByIdMap;
    private final Map<Character, ChatChannel> channelByPrefixMap;

    private ChatChannel defaultChannel;

    public ChannelRepository() {
        this.channelByIdMap = new HashMap<>();
        this.channelByPrefixMap = new HashMap<>();
    }

    public boolean isEmpty() {
        return this.channelByIdMap.isEmpty();
    }

    public void clear() {
        this.channelByIdMap.clear();
        this.channelByPrefixMap.clear();
    }

    public void add(ChatChannel channel) {
        this.channelByIdMap.put(channel.getId(), channel);

        if (channel.hasPrefix()) {
            this.channelByPrefixMap.put(channel.getPrefixChar(), channel);
        }
    }

    public void remove(ChatChannel channel) {
        this.channelByIdMap.remove(channel.getId());

        if (channel.hasPrefix()) {
            this.channelByPrefixMap.remove(channel.getPrefixChar());
        }
    }

    public ChatChannel getById(String id) {
        return this.channelByIdMap.get(LowerCase.INTERNAL.apply(id));
    }

    public ChatChannel getByPrefix(char prefix) {
        return this.channelByPrefixMap.get(prefix);
    }

    public Set<ChatChannel> getChannels() {
        return Set.copyOf(this.channelByIdMap.values());
    }

    public ChatChannel getDefaultChannel() {
        return this.defaultChannel;
    }

    public void setDefaultChannel(ChatChannel defaultChannel) {
        this.defaultChannel = defaultChannel;
    }
}
