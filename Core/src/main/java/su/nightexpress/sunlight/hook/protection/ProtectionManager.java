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

    public ProtectionManager(@NotNull BooleanSupplier ignoreGlobalRegion) {
        this.hooks = new ArrayList<>();
        this.register(new WorldGuardHook(ignoreGlobalRegion));
        this.register(new GriefPreventionHook());
    }

    public void register(@NotNull ProtectionHook hook) {
        this.hooks.add(hook);
    }

    @NotNull
    public List<ProtectionHook> getHooks() {
        return this.hooks;
    }

    public boolean isProtected(@NotNull Location location, @NotNull Set<String> ignoredHooks) {
        for (ProtectionHook hook : this.hooks) {
            if (!hook.isEnabled()) continue;
            if (ignoredHooks.contains(LowerCase.INTERNAL.apply(hook.getPluginName()))) continue;

            try {
                if (hook.isProtected(location)) return true;
            }
            catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return false;
    }
}
