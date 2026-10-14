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

    public static double getBalance(Player player) {
        return EconomyBridge.api().queryBalance(player);
    }

    public static void withdraw(Player player, double cost) {
        EconomyBridge.api().withdraw(player, cost);
    }

    /** Gives money back, for a refunded cost or a withdrawn vote. */
    public static void deposit(Player player, double amount) {
        if (amount <= 0D) {
            return;
        }
        EconomyBridge.api().deposit(player, amount);
    }

    /**
     * Offline-capable refund path. The {@link Player} overload only works while the player is
     * online; this one pays by UUID so a refund owed to somebody who has since logged off is not
     * silently dropped. Used by every refund in the voting module, where the ballot rows outlive
     * the player's session.
     */
    public static void deposit(java.util.UUID playerId, double amount) {
        if (playerId == null || amount <= 0D) {
            return;
        }
        EconomyBridge.api().deposit(playerId, amount);
    }

    public static String format(double cost) {
        return EconomyBridge.api().vaultCurrency().map(currency -> currency.format(cost))
                .orElseGet(() -> NumberUtil.format(cost));
    }
}
