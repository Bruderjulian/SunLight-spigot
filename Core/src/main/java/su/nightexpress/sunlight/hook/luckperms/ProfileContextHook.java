package su.nightexpress.sunlight.hook.luckperms;

import java.util.function.Function;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextCalculator;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.utils.Utils;

/**
 * Exposes the active game profile as the LuckPerms context
 * {@code profile=<id>}, so permissions, groups and tracks can be scoped
 * per profile (e.g. {@code /lp user <u> permission set kits.vip true profile=skyblock}).
 * <p>
 * LuckPerms is a compile-only dependency; every call is guarded so the
 * feature is simply inactive when LuckPerms is absent.
 */
public class ProfileContextHook {

    public static final String CONTEXT_KEY = "profile";

    private final Function<Player, String> activeIdProvider;
    private ContextCalculator<Player> calculator;

    public ProfileContextHook(@NotNull Function<Player, String> activeIdProvider) {
        this.activeIdProvider = activeIdProvider;
    }

    public boolean isAvailable() {
        return this.api() != null;
    }

    public synchronized void register() {
        if (this.calculator != null) return;
        LuckPerms api = this.api();
        if (api == null) return;
        try {
            ContextCalculator<Player> calculator = ContextCalculator.forSingleContext(CONTEXT_KEY, player -> {
                try {
                    String id = this.activeIdProvider.apply(player);
                    return id == null || id.isBlank() ? "main" : id;
                } catch (Exception exception) {
                    return "main";
                }
            });
            api.getContextManager().registerCalculator(calculator);
            this.calculator = calculator;
        } catch (Throwable throwable) {
            this.calculator = null;
        }
    }

    public synchronized void unregister() {
        if (this.calculator == null) return;
        try {
            LuckPerms api = this.api();
            if (api != null) api.getContextManager().unregisterCalculator(this.calculator);
        } catch (Throwable ignored) {
        }
        this.calculator = null;
    }

    /**
     * Recalculates the player's permissions immediately, e.g. after a
     * profile switch changed their active context value.
     */
    public void signalUpdate(@NotNull Player player) {
        if (this.calculator == null) return;
        try {
            LuckPerms api = this.api();
            if (api != null) api.getContextManager().signalContextUpdate(player);
        } catch (Throwable ignored) {
        }
    }

    private LuckPerms api() {
        if (!Utils.isLoaded(HookId.LUCKPERMS)) return null;
        try {
            return LuckPermsProvider.get();
        } catch (Throwable throwable) {
            return null;
        }
    }
}
