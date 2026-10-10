package su.nightexpress.sunlight.hook.protection;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;

import su.nightexpress.sunlight.hook.HookId;

public class GriefPreventionHook implements ProtectionHook {

    @Override

    public String getPluginName() {
        return HookId.GRIEF_PREVENTION;
    }

    @Override
    public boolean isProtected(Location location) {
        try {
            if (location == null || location.getWorld() == null) return false;
            if (GriefPrevention.instance == null || GriefPrevention.instance.dataStore == null) return false;
            return GriefPrevention.instance.dataStore.getClaimAt(location, false, null) != null;
        } catch (Exception | NoClassDefFoundError exception) {
            return false;
        }
    }
}
