package su.nightexpress.sunlight.moduleImpl.nametagsOld.model;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.config.NametagsPerms;

import java.util.Set;

/**
 * A rank (permission group) based nametag format. The format is resolved by the
 * highest `Priority` among all rules the player qualifies for.
 */
public class NametagRule implements Writeable {

    private final int priority;
    private final Set<String> ranks;
    private final String prefix;
    private final String suffix;
    private final String color;

    public NametagRule(int priority, Set<String> ranks, String prefix, String suffix, String color) {
        this.priority = priority;
        this.ranks = ranks;
        this.prefix = prefix;
        this.suffix = suffix;
        this.color = color;
    }

    public static NametagRule read(FileConfig config, String path) {
        int priority = config.getInt(path + ".Priority");
        Set<String> ranks = Lists.modify(config.getStringSet(path + ".Ranks"), LowerCase.INTERNAL::apply);
        String prefix = config.getString(path + ".Prefix", "");
        String suffix = config.getString(path + ".Suffix", "");
        String color = config.getString(path + ".Color", "white");

        return new NametagRule(priority, ranks, prefix, suffix, color);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Priority", this.priority);
        config.set(path + ".Ranks", this.ranks);
        config.set(path + ".Prefix", this.prefix);
        config.set(path + ".Suffix", this.suffix);
        config.set(path + ".Color", this.color);
    }

    public boolean isRankAvailable(Player player) {
        Set<String> playerRanks = Players.getInheritanceGroups(player);
        for (String rank : this.ranks) {
            if (rank.equals(SLPlaceholders.WILDCARD))
                return true;
            if (playerRanks.contains(rank) || NametagsPerms.hasRankAccess(player, rank))
                return true;
        }
        return false;
    }

    public int getPriority() {
        return this.priority;
    }

    public Set<String> getRanks() {
        return this.ranks;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public String getColor() {
        return this.color;
    }
}