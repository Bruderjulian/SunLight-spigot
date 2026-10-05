package su.nightexpress.sunlight.utils;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.integration.currency.EconomyBridge;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.utils.PermissionUtils;

public class EconomyUtils {

    private static final String BYPASS_COST     = "sunlight.bypass.cost";
    private static final String BYPASS_COOLDOWN = "sunlight.bypass.cooldown";

    public static boolean hasCurrency() {
        return EconomyBridge.api().hasVaultCurrency();
    }

    public static boolean hasBypass(Player player, String bypassPermission) {
        if (player.hasPermission(BYPASS_COST))
            return true;

        return bypassPermission != null && player.hasPermission(bypassPermission);
    }

    public static boolean hasBypass(Player player, Module module) {
        if (player.hasPermission(BYPASS_COST))
            return true;

        return module != null && player.hasPermission(moduleNode(module, "bypass.cost"));
    }

    public static boolean hasBypass(Player player) {
        return player.hasPermission(BYPASS_COST);
    }

    public static boolean hasCooldownBypass(Player player, String bypassPermission) {
        if (player.hasPermission(BYPASS_COOLDOWN))
            return true;

        return bypassPermission != null && player.hasPermission(bypassPermission);
    }

    public static boolean hasCooldownBypass(Player player, Module module) {
        if (player.hasPermission(BYPASS_COOLDOWN))
            return true;

        return module != null && player.hasPermission(moduleNode(module, "bypass.cooldown"));
    }
    
    public static boolean hasCooldownBypass(Player player) {
        return player.hasPermission(BYPASS_COOLDOWN);
    }

    private static String moduleNode(Module module, String child) {
        return PermissionUtils.node("sunlight." + module.getPermissionNamespace(), child);
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
