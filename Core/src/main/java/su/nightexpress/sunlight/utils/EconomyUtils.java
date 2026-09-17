package su.nightexpress.sunlight.utils;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import su.nightexpress.nightcore.integration.currency.EconomyBridge;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.sunlight.config.Perms;
import su.nightexpress.sunlight.module.Module;

public class EconomyUtils {

    public static boolean hasCurrency() {
        return EconomyBridge.api().hasVaultCurrency();
    }

    public static boolean hasBypass(@NonNull Player player, @Nullable Permission bypassPermission) {
        if (player.hasPermission(Perms.BYPASS_COST)) return true;

        return bypassPermission != null && player.hasPermission(bypassPermission);
    }

    public static boolean hasBypass(@NonNull Player player, @Nullable Module module) {
        if (player.hasPermission(Perms.BYPASS_COST)) return true;

        return module != null && player.hasPermission(moduleBypassNode(module, "bypass.cost"));
    }

    public static boolean hasCooldownBypass(@NonNull Player player, @Nullable Permission bypassPermission) {
        if (player.hasPermission(Perms.BYPASS_COOLDOWN)) return true;

        return bypassPermission != null && player.hasPermission(bypassPermission);
    }

    public static boolean hasCooldownBypass(@NonNull Player player, @Nullable Module module) {
        if (player.hasPermission(Perms.BYPASS_COOLDOWN)) return true;

        return module != null && player.hasPermission(moduleBypassNode(module, "bypass.cooldown"));
    }

    @NonNull
    private static String moduleBypassNode(@NonNull Module module, @NonNull String child) {
        return Perms.detached(module.getPermissionNamespace()).childrenNode(child);
    }

    public static boolean canAfford(@NonNull Player player, double cost) {
        return EconomyBridge.api().queryBalance(player) >= cost;
    }

    public static void withdraw(@NonNull Player player, double cost) {
        EconomyBridge.api().withdraw(player, cost);
    }

    @NonNull
    public static String format(double cost) {
        return EconomyBridge.api().vaultCurrency().map(currency -> currency.format(cost)).orElseGet(() -> NumberUtil.format(cost));
    }
}