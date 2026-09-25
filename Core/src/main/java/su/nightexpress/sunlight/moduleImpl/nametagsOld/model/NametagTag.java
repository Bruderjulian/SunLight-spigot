package su.nightexpress.sunlight.moduleImpl.nametagsOld.model;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.sunlight.utils.Utils;

import java.util.List;

/**
 * A per-player selectable tag. One tag may contribute an additional prefix,
 * suffix and name color which is combined with the rank format.
 */
public class NametagTag implements Writeable {

    private final String id;
    private final String display;
    private final String prefix;
    private final String suffix;
    private final String color;
    private final double cost;
    private final List<String> description;
    private final String permission;

    public NametagTag(String id, String display, String prefix, String suffix, String color, double cost,
            List<String> description, String permission) {
        this.id = Utils.lowercase(id);
        this.display = display;
        this.prefix = prefix;
        this.suffix = suffix;
        this.color = color;
        this.cost = Math.max(0D, cost);
        this.description = description;
        this.permission = permission;
    }

    public static NametagTag read(FileConfig config, String path) {
        String id = path.substring(path.lastIndexOf('.') + 1);
        String display = config.getString(path + ".Display", id);
        String prefix = config.getString(path + ".Prefix", "");
        String suffix = config.getString(path + ".Suffix", "");
        String color = config.getString(path + ".Color", "");
        double cost = Math.max(0D, config.getDouble(path + ".Cost"));
        List<String> description = config.getStringList(path + ".Description");
        String permission = config.getString(path + ".Permission", "");

        return new NametagTag(id, display, prefix, suffix, color, cost, description, permission);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Display", this.display);
        config.set(path + ".Prefix", this.prefix);
        config.set(path + ".Suffix", this.suffix);
        config.set(path + ".Color", this.color);
        config.set(path + ".Cost", this.cost);
        config.set(path + ".Description", this.description);
        config.set(path + ".Permission", this.permission);
    }

    public String getId() {
        return this.id;
    }

    public String getDisplay() {
        return this.display;
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

    public boolean hasColor() {
        return this.color != null && !this.color.isBlank();
    }

    public boolean hasCost() {
        return this.cost > 0D;
    }

    public double getCost() {
        return this.cost;
    }

    public List<String> getDescription() {
        return this.description;
    }

    /** Optional explicit permission; derived {@code nametags.tag.<id>} is always checked. */
    public String getPermission() {
        return this.permission;
    }
}