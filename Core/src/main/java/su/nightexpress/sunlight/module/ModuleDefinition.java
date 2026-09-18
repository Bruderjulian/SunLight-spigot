package su.nightexpress.sunlight.module;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;

import java.util.Locale;

public record ModuleDefinition(boolean enabled, String name, String prefix) implements Writeable {

    public static ModuleDefinition named(String name) {
        return new ModuleDefinition(true, name, defaultPrefix(name));
    }

    public static ModuleDefinition read(FileConfig config, String path) {
        boolean enabled = config.get(ConfigTypes.BOOLEAN, path + ".Enabled", true);
        String name = config.get(ConfigTypes.STRING, path + ".Name", "null");
        String prefix = config.get(ConfigTypes.STRING, path + ".Prefix", defaultPrefix(name));

        return new ModuleDefinition(enabled, name, prefix);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Enabled", this.enabled);
        config.set(path + ".Name", this.name);
    }

    private static String defaultPrefix(String name) {
        return TagWrappers.GRADIENT_3.with("#FFAA00", "#FF8833", "#FF5500")
                .wrap(TagWrappers.BOLD.wrap(name.toUpperCase(Locale.ROOT))) + TagWrappers.DARK_GRAY.wrap(" » ");
    }
}
