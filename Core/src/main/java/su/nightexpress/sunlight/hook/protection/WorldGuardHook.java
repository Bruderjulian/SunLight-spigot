package su.nightexpress.sunlight.hook.protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;

import su.nightexpress.sunlight.hook.HookId;

import java.util.function.BooleanSupplier;

public class WorldGuardHook implements ProtectionHook {

    private static final String GLOBAL_REGION_ID = "__global__";

    private final BooleanSupplier ignoreGlobalRegion;

    public WorldGuardHook(BooleanSupplier ignoreGlobalRegion) {
        this.ignoreGlobalRegion = ignoreGlobalRegion;
    }

    @Override

    public String getPluginName() {
        return HookId.WORLD_GUARD;
    }

    @Override
    public boolean isProtected(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        ApplicableRegionSet set = container.createQuery().getApplicableRegions(BukkitAdapter.adapt(location));

        if (set.size() == 0)
            return false;

        if (this.ignoreGlobalRegion.getAsBoolean() && set.size() == 1) {
            ProtectedRegion region = set.getRegions().iterator().next();
            if (GLOBAL_REGION_ID.equalsIgnoreCase(region.getId()))
                return false;
        }

        return true;
    }
}
