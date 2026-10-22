package su.nightexpress.sunlight.hook.combat;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.hook.HookId;

/**
 * PvPManager combat status, resolved entirely through reflection.
 * <p>
 * Supports v4 ({@code getPlayerManager()}) and v3 ({@code getPlayerHandler()})
 * plugin entry points, plus the static {@code CombatPlayer.get(player)}
 * shortcut. Every lookup is resolved once and cached; any failure degrades
 * to "not in combat" instead of breaking the server.
 */
public class PvPManagerHook implements CombatHook {

    private static final String[] COMBAT_PLAYER_CLASSES = {
        "me.chancesd.pvpmanager.player.CombatPlayer",
        "me.chancesd.pvpmanager.CombatPlayer",
        "me.NoChance.PvPManager.PvPlayer"
    };

    private Method staticGet;
    private Method managerGetter;
    private Method playerGetter;
    private Method inCombat;
    private boolean resolved;

    @Override
    public @NotNull String getId() {
        return "pvpmanager";
    }

    @Override
    public boolean isAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(HookId.PVP_MANAGER);
        return plugin != null && plugin.isEnabled() && this.resolve(plugin);
    }

    @Override
    public boolean isInCombat(@NotNull Player player) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(HookId.PVP_MANAGER);
        if (plugin == null || !plugin.isEnabled() || !this.resolve(plugin)) return false;
        try {
            Object combatPlayer;
            if (this.staticGet != null) {
                combatPlayer = this.staticGet.invoke(null, player);
            } else {
                Object manager = this.managerGetter.invoke(plugin);
                if (manager == null) return false;
                combatPlayer = this.playerGetter.invoke(manager, player);
            }
            if (combatPlayer == null) return false;
            Object result = this.inCombat.invoke(combatPlayer);
            return result instanceof Boolean tagged && tagged;
        } catch (Throwable throwable) {
            return false;
        }
    }

    private synchronized boolean resolve(Plugin plugin) {
        if (this.resolved) return this.staticGet != null || this.managerGetter != null;
        this.resolved = true;

        // Preferred: static CombatPlayer.get(player) -> isInCombat().
        for (String className : COMBAT_PLAYER_CLASSES) {
            try {
                Class<?> combatPlayer = Class.forName(className);
                Method get = combatPlayer.getMethod("get", Player.class);
                Method tagged = combatPlayer.getMethod("isInCombat");
                if (!java.lang.reflect.Modifier.isStatic(get.getModifiers())) continue;
                this.staticGet = get;
                this.inCombat = tagged;
                return true;
            } catch (Throwable ignored) {
            }
        }

        // Fallback: plugin.getPlayerManager()/getPlayerHandler() -> get(player).
        for (String managerMethod : new String[] {"getPlayerManager", "getPlayerHandler"}) {
            try {
                Method getter = plugin.getClass().getMethod(managerMethod);
                Object manager = getter.invoke(plugin);
                if (manager == null) continue;
                Method byPlayer = this.findPlayerGetter(manager);
                if (byPlayer == null) continue;
                Method tagged = byPlayer.getReturnType().getMethod("isInCombat");
                this.managerGetter = getter;
                this.playerGetter = byPlayer;
                this.inCombat = tagged;
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    private Method findPlayerGetter(Object manager) {
        for (Method method : manager.getClass().getMethods()) {
            if (!method.getName().equals("get")) continue;
            if (method.getParameterCount() != 1) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(Player.class)
                && !Player.class.isAssignableFrom(method.getParameterTypes()[0])) continue;
            return method;
        }
        return null;
    }
}
