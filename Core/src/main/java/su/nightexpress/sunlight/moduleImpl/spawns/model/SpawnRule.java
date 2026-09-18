package su.nightexpress.sunlight.moduleImpl.spawns.model;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.SLPlaceholders;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SpawnRule implements Writeable {

    private boolean enabled;
    private Set<String> ranks;

    public SpawnRule(boolean enabled, Set<String> ranks) {
        this.setEnabled(enabled);
        this.setRanks(ranks);
    }

    public static SpawnRule read(FileConfig config, String path) {
        boolean enabled = config.getBoolean(path + ".Enabled");
        Set<String> ranks = Lists.modify(config.getStringSet(path + ".Ranks"), LowerCase.INTERNAL::apply);

        return new SpawnRule(enabled, ranks);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Enabled", this.enabled);
        config.set(path + ".Ranks", this.ranks);
    }

    public boolean isApplicable(Player player) {
        if (!this.enabled)
            return false;
        if (this.ranks.contains(SLPlaceholders.WILDCARD))
            return true;

        Set<String> playerRanks = Players.getInheritanceGroups(player);
        return playerRanks.stream().anyMatch(this.ranks::contains);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getRanks() {
        return this.ranks;
    }

    public void setRanks(Collection<String> ranks) {
        this.ranks = new HashSet<>(ranks);
    }
}
