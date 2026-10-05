package su.nightexpress.sunlight.hook.combat;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.hook.HookId;

/**
 * CombatLogX combat status, resolved entirely through reflection
 * ({@code ICombatLogX#getCombatManager()#isInCombat(Player)}).
 * Any failure degrades to "not in combat".
 */
public class CombatLogXHook implements CombatHook {

    private Method managerGetter;
    private Method inCombat;
    private boolean resolved;

    @Override
    public @NotNull String getId() {
        return "combatlogx";
    }

    @Override
    public boolean isAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(HookId.COMBAT_LOG_X);
        return plugin != null && plugin.isEnabled() && this.resolve(plugin);
    }

    @Override
    public boolean isInCombat(@NotNull Player player) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(HookId.COMBAT_LOG_X);
        if (plugin == null || !plugin.isEnabled() || !this.resolve(plugin)) return false;
        try {
            Object manager = this.managerGetter.invoke(plugin);
            if (manager == null) return false;
            Object result = this.inCombat.invoke(manager, player);
            return result instanceof Boolean tagged && tagged;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private synchronized boolean resolve(Plugin plugin) {
        if (this.resolved) return this.managerGetter != null;
        this.resolved = true;
        try {
            Method getter = plugin.getClass().getMethod("getCombatManager");
            Method tagged = getter.getReturnType().getMethod("isInCombat", Player.class);
            this.managerGetter = getter;
            this.inCombat = tagged;
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
