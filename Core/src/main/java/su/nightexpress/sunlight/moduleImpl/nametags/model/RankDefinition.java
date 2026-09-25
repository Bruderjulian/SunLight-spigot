package su.nightexpress.sunlight.moduleImpl.nametags.model;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.utils.Utils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A rank (permission group) based nametag format. The format is resolved by the
 * highest {@code priority} among all ranks the player qualifies for.
 */
public class RankDefinition implements Writeable {

    private String id;
    private int priority;
    private final Set<String> ranks;
    private String prefix;
    private String suffix;
    private String color;
    private boolean isDefault;

    public RankDefinition(@NotNull String id) {
        this(id, 0, Set.of(), "", "", "", false);
    }

    public RankDefinition(@NotNull String id,
            int priority,
            @NotNull Set<String> ranks,
            @NotNull String prefix,
            @NotNull String suffix,
            @NotNull String color,
            boolean isDefault
    ) {
        this.id = Utils.lowercase(id);
        this.priority = priority;
        this.ranks = new LinkedHashSet<>(ranks);
        this.prefix = prefix;
        this.suffix = suffix;
        this.color = color;
        this.isDefault = isDefault;
    }

    public static RankDefinition read(FileConfig config, String path) {
        String id = path.substring(path.lastIndexOf('.') + 1);
        RankDefinition rank = new RankDefinition(id);
        rank.setPriority(config.getInt(path + ".Priority"));
        rank.ranks.clear();
        for (String group : config.getStringSet(path + ".Ranks")) {
            rank.ranks.add(Utils.lowercase(group));
        }
        rank.setPrefix(config.getString(path + ".Prefix", ""));
        rank.setSuffix(config.getString(path + ".Suffix", ""));
        rank.setColor(config.getString(path + ".Color", ""));
        rank.setDefault(config.getBoolean(path + ".Default", false));
        return rank;
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Priority", this.priority);
        config.set(path + ".Ranks", this.ranks.stream().map(Utils::lowercase).toList());
        config.set(path + ".Prefix", this.prefix);
        config.set(path + ".Suffix", this.suffix);
        config.set(path + ".Color", this.color);
        config.set(path + ".Default", this.isDefault);
    }

    /**
     * Whether the player qualifies for this rank, either through group inheritance or
     * through the {@code nametags.rank.<group>} permission.
     */
    public boolean isAvailable(@NotNull Player player) {
        if (this.ranks.isEmpty()) return false;

        Set<String> playerRanks = Players.getInheritanceGroups(player);
        for (String rank : this.ranks) {
            if (rank.equals(SLPlaceholders.WILDCARD)) return true;
            if (playerRanks.contains(rank)) return true;
            if (player.hasPermission("nametags.rank." + rank)) return true;
        }
        return false;
    }

    public @NotNull String getId() {
        return this.id;
    }

    public void setId(@NotNull String id) {
        this.id = Utils.lowercase(id);
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public @NotNull Set<String> getRanks() {
        return this.ranks;
    }

    public void setRanks(@NotNull Set<String> ranks) {
        this.ranks.clear();
        ranks.forEach(rank -> this.ranks.add(Utils.lowercase(rank)));
    }

    public @NotNull String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(@NotNull String prefix) {
        this.prefix = prefix;
    }

    public @NotNull String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(@NotNull String suffix) {
        this.suffix = suffix;
    }

    public @NotNull String getColor() {
        return this.color;
    }

    public void setColor(@NotNull String color) {
        this.color = color;
    }

    public boolean hasColor() {
        return !this.color.isBlank();
    }

    /** The fallback rank used when nothing else matches. */
    public boolean isDefault() {
        return this.isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /** Resolves the best matching rank for a player, or {@code null} if none applies. */
    public @Nullable RankDefinition resolve(@NotNull Iterable<RankDefinition> definitions, @NotNull Player player) {
        RankDefinition best = null;
        for (RankDefinition definition : definitions) {
            if (definition.isDefault()) {
                if (best == null) best = definition;
                continue;
            }
            if (!definition.isAvailable(player)) continue;
            if (best == null || definition.priority > best.priority) {
                best = definition;
            }
        }
        return best;
    }
}
