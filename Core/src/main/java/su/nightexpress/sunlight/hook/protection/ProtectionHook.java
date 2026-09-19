package su.nightexpress.sunlight.hook.protection;

import org.bukkit.Location;

import su.nightexpress.sunlight.utils.Utils;

public interface ProtectionHook {

    String getPluginName();

    default boolean isEnabled() {
        return Utils.isInstalled(this.getPluginName());
    }

    boolean isProtected(Location location);
}
