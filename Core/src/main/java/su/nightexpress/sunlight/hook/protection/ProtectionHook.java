package su.nightexpress.sunlight.hook.protection;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.Plugins;

public interface ProtectionHook {

    @NotNull String getPluginName();

    default boolean isEnabled() {
        return Plugins.isInstalled(this.getPluginName());
    }

    boolean isProtected(@NotNull Location location);
}
