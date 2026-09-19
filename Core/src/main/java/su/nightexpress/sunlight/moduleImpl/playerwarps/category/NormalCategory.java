package su.nightexpress.sunlight.moduleImpl.playerwarps.category;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolvable;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import su.nightexpress.sunlight.moduleImpl.playerwarps.PlayerWarp;
import su.nightexpress.sunlight.moduleImpl.playerwarps.PlayerWarpsPlaceholders;
import su.nightexpress.sunlight.utils.Utils;

import java.util.List;

public class NormalCategory implements WarpCategory, PlaceholderResolvable, Writeable {

    private final String id;
    private final String name;
    private final boolean primary;
    private final List<String> description;
    private final NightItem icon;

    public NormalCategory(String id,
            String name,
            boolean primary,
            List<String> description,
            NightItem icon) {
        this.id = id;
        this.name = name;
        this.primary = primary;
        this.description = description;
        this.icon = icon;
    }

    public static NormalCategory read(FileConfig config, String path) {
        String id = Utils.lowercase(config.getString(path + ".Id", "null"));
        String name = config.getString(path + ".Name", id);
        boolean primary = config.getBoolean(path + ".Primary");
        List<String> description = config.getStringList(path + ".Description");
        NightItem icon = config.getCosmeticItem(path + ".Icon");

        return new NormalCategory(id, name, primary, description, icon);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Id", this.id);
        config.set(path + ".Name", this.name);
        config.set(path + ".Primary", this.primary);
        config.set(path + ".Description", this.description);
        config.set(path + ".Icon", this.icon);
    }

    @Override

    public PlaceholderResolver placeholders() {
        return PlayerWarpsPlaceholders.CATEGORY.resolver(this);
    }

    public boolean isWarpOfThis(PlayerWarp warp) {
        return warp.getCategoryId().equalsIgnoreCase(this.id);
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public boolean primary() {
        return this.primary;
    }

    public List<String> description() {
        return List.copyOf(this.description);
    }

    public NightItem icon() {
        return this.icon.copy();
    }
}
