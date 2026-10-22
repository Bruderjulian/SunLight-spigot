package su.nightexpress.sunlight.moduleImpl.nametags.model;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsPerms;
import su.nightexpress.sunlight.utils.Utils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A rank (permission group) based nametag format. The format is resolved by the
 * highest {@code priority} among all ranks the player qualifies for.
 */
public class RankDefinition implements Writeable {

    private String id;
    private String display;
    private List<String> description;
    private String iconMaterial;
    private int priority;
    private final Set<String> ranks;
    private String prefix;
    private String suffix;
    private String color;
    private boolean isDefault;

    public RankDefinition(@NotNull String id) {
        this(id, id, List.of(), "NAME_TAG", 0, Set.of(), "", "", "", false);
    }

    public RankDefinition(@NotNull String id,
            @NotNull String display,
            @NotNull List<String> description,
            @NotNull String iconMaterial,
            int priority,
            @NotNull Set<String> ranks,
            @NotNull String prefix,
            @NotNull String suffix,
            @NotNull String color,
            boolean isDefault
    ) {
        this.id = Utils.lowercase(id);
        this.display = display;
        this.description = List.copyOf(description);
        this.iconMaterial = iconMaterial;
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
        rank.setDisplay(config.getString(path + ".Display", id));
        rank.setDescription(config.getStringList(path + ".Description"));
        rank.setIconMaterial(config.getString(path + ".Icon", "NAME_TAG"));
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
        config.set(path + ".Display", this.display);
        config.set(path + ".Description", this.description);
        config.set(path + ".Icon", this.iconMaterial);
        config.set(path + ".Priority", this.priority);
        config.set(path + ".Ranks", this.ranks.stream().map(Utils::lowercase).toList());
        config.set(path + ".Prefix", this.prefix);
        config.set(path + ".Suffix", this.suffix);
        config.set(path + ".Color", this.color);
        config.set(path + ".Default", this.isDefault);
    }

    /**
     * Whether the player qualifies for this rank, either through group inheritance or
     * through the {@code sunlight.nametags.rank.<group>} permission.
     */
    public boolean isAvailable(@NotNull Player player) {
        if (this.ranks.isEmpty()) return false;

        Set<String> playerRanks = Players.getInheritanceGroups(player);
        for (String rank : this.ranks) {
            if (rank.equals(SLPlaceholders.WILDCARD)) return true;
            if (playerRanks.contains(rank)) return true;
            if (NametagsPerms.hasRankAccess(player, rank)) return true;
        }
        return false;
    }

    public @NotNull String getId() {
        return this.id;
    }

    public void setId(@NotNull String id) {
        this.id = Utils.lowercase(id);
    }

    public @NotNull String getDisplay() {
        return this.display;
    }

    public void setDisplay(@NotNull String display) {
        this.display = display.isBlank() ? this.id : display;
    }

    public @NotNull List<String> getDescription() {
        return this.description;
    }

    public void setDescription(@NotNull List<String> description) {
        this.description = List.copyOf(description);
    }

    public @NotNull String getIconMaterial() {
        return this.iconMaterial;
    }

    public void setIconMaterial(@NotNull String iconMaterial) {
        this.iconMaterial = iconMaterial.isBlank() ? "NAME_TAG" : iconMaterial;
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
}
