package su.nightexpress.sunlight.utils;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import su.nightexpress.nightcore.integration.currency.EconomyBridge;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.sunlight.config.Perms;
import su.nightexpress.sunlight.module.Module;

public class EconomyUtils {

    public static boolean hasCurrency() {
        return EconomyBridge.api().hasVaultCurrency();
    }

    public static boolean hasBypass(Player player, Permission bypassPermission) {
        if (player.hasPermission(Perms.BYPASS_COST))
            return true;

        return bypassPermission != null && player.hasPermission(bypassPermission);
    }

    public static boolean hasBypass(Player player, Module module) {
        if (player.hasPermission(Perms.BYPASS_COST))
            return true;

        return module != null && player.hasPermission(moduleBypassNode(module, "bypass.cost"));
    }

    public static boolean hasCooldownBypass(Player player, Permission bypassPermission) {
        if (player.hasPermission(Perms.BYPASS_COOLDOWN))
            return true;

        return bypassPermission != null && player.hasPermission(bypassPermission);
    }

    public static boolean hasCooldownBypass(Player player, Module module) {
        if (player.hasPermission(Perms.BYPASS_COOLDOWN))
            return true;

        return module != null && player.hasPermission(moduleBypassNode(module, "bypass.cooldown"));
    }

    private static String moduleBypassNode(Module module, String child) {
        return Perms.detached(module.getPermissionNamespace()).childrenNode(child);
    }

    public static boolean canAfford(Player player, double cost) {
        return EconomyBridge.api().queryBalance(player) >= cost;
    }

    public static void withdraw(Player player, double cost) {
        EconomyBridge.api().withdraw(player, cost);
    }

    public static String format(double cost) {
        return EconomyBridge.api().vaultCurrency().map(currency -> currency.format(cost))
                .orElseGet(() -> NumberUtil.format(cost));
    }
}