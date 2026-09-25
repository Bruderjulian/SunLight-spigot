package su.nightexpress.sunlight.moduleImpl.nametags.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.sunlight.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * An admin-defined, named set of nametag defaults. A player picks a profile; the
 * profile decides the rank and tag used in every world it applies to.
 */
public class Profile implements Writeable {

    private String id;
    private String display;
    private final List<String> description;
    private String iconMaterial;
    /** Empty list means "every world". */
    private final List<String> worlds;

    /** Higher wins when several profiles match a player. */
    private int priority;
    /** {@code null} means "inherit from rank resolution". */
    private String rankId;
    /** {@code null} means "inherit from player selection". */
    private String tagId;

    public Profile(@NotNull String id) {
        this(id, id, List.of(), "NAME_TAG", List.of(), 0, null, null);
    }

    public Profile(@NotNull String id,
            @NotNull String display,
            @NotNull List<String> description,
            @NotNull String iconMaterial,
            @NotNull List<String> worlds,
            int priority,
            @Nullable String rankId,
            @Nullable String tagId
    ) {
        this.id = Utils.lowercase(id);
        this.display = display;
        this.description = new ArrayList<>(description);
        this.iconMaterial = iconMaterial;
        this.worlds = new ArrayList<>(worlds);
        this.priority = priority;
        this.rankId = Utils.lowercase(rankId);
        this.tagId = Utils.lowercase(tagId);
    }

    public static Profile read(FileConfig config, String path) {
        String id = path.substring(path.lastIndexOf('.') + 1);
        Profile profile = new Profile(id);
        profile.setDisplay(config.getString(path + ".Display", id));
        profile.setDescription(config.getStringList(path + ".Description"));
        profile.setIconMaterial(config.getString(path + ".Icon", "NAME_TAG"));
        profile.worlds.clear();
        profile.worlds.addAll(Utils.lowercaseAll(config.getStringList(path + ".Worlds")));
        profile.setPriority(config.getInt(path + ".Priority"));
        profile.setRankId(config.getString(path + ".Rank", null));
        profile.setTagId(config.getString(path + ".Tag", null));
        return profile;
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Display", this.display);
        config.set(path + ".Description", this.description);
        config.set(path + ".Icon", this.iconMaterial);
        config.set(path + ".Worlds", this.worlds);
        config.set(path + ".Priority", this.priority);
        config.set(path + ".Rank", this.rankId);
        config.set(path + ".Tag", this.tagId);
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
        this.description.clear();
        this.description.addAll(description);
    }

    public @NotNull String getIconMaterial() {
        return this.iconMaterial;
    }

    public void setIconMaterial(@NotNull String iconMaterial) {
        this.iconMaterial = iconMaterial.isBlank() ? "NAME_TAG" : iconMaterial;
    }

    public @NotNull List<String> getWorlds() {
        return this.worlds;
    }

    public void setWorlds(@NotNull List<String> worlds) {
        this.worlds.clear();
        this.worlds.addAll(Utils.lowercaseAll(worlds));
    }

    public boolean appliesTo(@NotNull String worldName) {
        if (this.worlds.isEmpty()) return true;
        return this.worlds.contains(Utils.lowercase(worldName));
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public @Nullable String getRankId() {
        return this.rankId;
    }

    public void setRankId(@Nullable String rankId) {
        this.rankId = Utils.lowercase(rankId);
    }

    public @Nullable String getTagId() {
        return this.tagId;
    }

    public void setTagId(@Nullable String tagId) {
        this.tagId = Utils.lowercase(tagId);
    }

    public boolean hasRank() {
        return this.rankId != null && !this.rankId.isBlank();
    }

    public boolean hasTag() {
        return this.tagId != null && !this.tagId.isBlank();
    }
}
