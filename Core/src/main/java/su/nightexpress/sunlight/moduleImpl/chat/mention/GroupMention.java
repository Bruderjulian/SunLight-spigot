package su.nightexpress.sunlight.moduleImpl.chat.mention;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.SLPlaceholders;

import java.util.Set;

public class GroupMention implements ChatMention, Writeable {

    private final String format;
    private final Set<String> ranks;

    public GroupMention(String format, Set<String> ranks) {
        this.format = format;
        this.ranks = ranks;
    }

    public static GroupMention read(FileConfig config, String path) {
        String format = config.getString(path + ".Format", "");
        Set<String> groups = Lists.modify(config.getStringSet(path + ".Included-Ranks"), String::toLowerCase);

        return new GroupMention(format, groups);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Format", this.format);
        config.set(path + ".Included-Ranks", this.ranks);
    }

    @Override
    public boolean isApplicable(Player player) {
        if (this.ranks.contains(SLPlaceholders.WILDCARD))
            return true;

        Set<String> groups = Players.getInheritanceGroups(player);
        return this.ranks.stream().anyMatch(groups::contains);
    }

    @Override

    public String getFormat() {
        return this.format;
    }

    public Set<String> getRanks() {
        return this.ranks;
    }
}
