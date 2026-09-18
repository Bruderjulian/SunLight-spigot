package su.nightexpress.sunlight.hook.protection;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.LowerCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class ProtectionManager {

    private final List<ProtectionHook> hooks;

    public ProtectionManager(BooleanSupplier ignoreGlobalRegion) {
        this.hooks = new ArrayList<>();
        this.register(new WorldGuardHook(ignoreGlobalRegion));
        this.register(new GriefPreventionHook());
    }

    public void register(ProtectionHook hook) {
        this.hooks.add(hook);
    }

    public List<ProtectionHook> getHooks() {
        return this.hooks;
    }

    public boolean isProtected(Location location, Set<String> ignoredHooks) {
        for (ProtectionHook hook : this.hooks) {
            if (!hook.isEnabled())
                continue;
            if (ignoredHooks.contains(LowerCase.INTERNAL.apply(hook.getPluginName())))
                continue;

            try {
                if (hook.isProtected(location))
                    return true;
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return false;
    }
}
