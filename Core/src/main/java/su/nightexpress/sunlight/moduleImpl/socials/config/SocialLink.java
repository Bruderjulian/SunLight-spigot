package su.nightexpress.sunlight.moduleImpl.socials.config;

import org.bukkit.Material;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;

import java.util.Locale;

public record SocialLink(String display, String url, String permission, Material material) implements Writeable {

    public static SocialLink read(FileConfig config, String path) {
        String display = config.getString(path + ".Display", "Link");
        String url = config.getString(path + ".URL", "https://example.com");
        String permission = config.getString(path + ".Permission", "");

        Material material;
        try {
            material = Material.valueOf(config.getString(path + ".Material", "PAPER").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            material = Material.PAPER;
        }

        return new SocialLink(display, url, permission, material);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Display", this.display);
        config.set(path + ".URL", this.url);
        config.set(path + ".Permission", this.permission);
        config.set(path + ".Material", this.material.name());
    }

    public boolean requiresPermission() {
        return this.permission != null && !this.permission.isBlank();
    }
}
