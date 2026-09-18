package su.nightexpress.sunlight.module.chat.format;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.module.chat.ChatDefaults;

import java.util.Set;

public class FormatDefinition implements Writeable {

    private final int priority;
    private final String format;
    private final Set<String> applicableRanks;

    public FormatDefinition(int priority, String format, Set<String> applicableRanks) {
        this.priority = priority;
        this.format = format;
        this.applicableRanks = applicableRanks;
    }

    public static FormatDefinition read(FileConfig config, String path) {
        int priority = config.getInt(path + ".Priority");
        String format = config.getString(path + ".Format", ChatDefaults.DEFAULT_USER_FORMAT);
        Set<String> applicableRanks = Lists.modify(config.getStringSet(path + ".Ranks"), LowerCase.INTERNAL::apply);

        return new FormatDefinition(priority, format, applicableRanks);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Priority", this.priority);
        config.set(path + ".Format", this.format);
        config.set(path + ".Ranks", this.applicableRanks);
    }

    public boolean isApplicable(Player player) {
        if (this.applicableRanks.isEmpty())
            return false;
        if (this.applicableRanks.contains(SLPlaceholders.WILDCARD))
            return true;

        Set<String> groups = Players.getInheritanceGroups(player);
        return this.applicableRanks.stream().anyMatch(groups::contains);
    }

    public int getPriority() {
        return this.priority;
    }

    public String getFormat() {
        return this.format;
    }

    public Set<String> getApplicableRanks() {
        return this.applicableRanks;
    }
}
