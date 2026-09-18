package su.nightexpress.sunlight.hook.protection;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.sunlight.hook.HookId;

public class GriefPreventionHook implements ProtectionHook {

    @Override

    public String getPluginName() {
        return HookId.GRIEF_PREVENTION;
    }

    @Override
    public boolean isProtected(Location location) {
        return GriefPrevention.instance.dataStore.getClaimAt(location, false, null) != null;
    }
}
