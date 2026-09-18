package su.nightexpress.sunlight.module.tab.format;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.SLPlaceholders;

import java.util.List;
import java.util.Set;

public class TabLayoutFormat implements Writeable {

    private final int priority;
    private final Set<String> worlds;
    private final Set<String> ranks;
    private final List<String> header;
    private final List<String> footer;

    public TabLayoutFormat(int priority,
            Set<String> worlds,
            Set<String> ranks,
            List<String> header,
            List<String> footer) {
        this.priority = priority;
        this.worlds = worlds;
        this.ranks = ranks;
        this.header = header;
        this.footer = footer;
    }

    public static TabLayoutFormat read(FileConfig config, String path) {
        int priority = config.getInt(path + ".Priority");
        Set<String> worlds = Lists.modify(config.getStringSet(path + ".Worlds"), String::toLowerCase);
        Set<String> ranks = Lists.modify(config.getStringSet(path + ".Groups"), String::toLowerCase);

        List<String> header = config.getStringList(path + ".Header");
        List<String> footer = config.getStringList(path + ".Footer");

        return new TabLayoutFormat(priority, worlds, ranks, header, footer);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Priority", this.priority);
        config.set(path + ".Worlds", this.worlds);
        config.set(path + ".Groups", this.ranks);
        config.set(path + ".Header", this.header);
        config.set(path + ".Footer", this.footer);
    }

    public boolean isAvailable(Player player) {
        return this.isAvailableForWorld(player) && this.isAvailableForRank(player);
    }

    public boolean isAvailableForRank(Player player) {
        if (this.ranks.contains(SLPlaceholders.WILDCARD))
            return true;

        Set<String> playerRanks = Players.getInheritanceGroups(player);
        return playerRanks.stream().anyMatch(this.ranks::contains);
    }

    public boolean isAvailableForWorld(Player player) {
        if (this.worlds.contains(SLPlaceholders.WILDCARD))
            return true;

        return this.worlds.contains(LowerCase.INTERNAL.apply(player.getWorld().getName()));
    }

    public int getPriority() {
        return this.priority;
    }

    public Set<String> getWorlds() {
        return this.worlds;
    }

    public Set<String> getRanks() {
        return this.ranks;
    }

    public List<String> getHeader() {
        return this.header;
    }

    public List<String> getFooter() {
        return this.footer;
    }
}
