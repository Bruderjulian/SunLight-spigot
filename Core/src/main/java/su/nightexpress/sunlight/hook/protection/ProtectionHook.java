package su.nightexpress.sunlight.hook.protection;

import org.bukkit.Location;

import su.nightexpress.nightcore.util.Plugins;

public interface ProtectionHook {

    String getPluginName();

    default boolean isEnabled() {
        return Plugins.isInstalled(this.getPluginName());
    }

    boolean isProtected(Location location);
}
