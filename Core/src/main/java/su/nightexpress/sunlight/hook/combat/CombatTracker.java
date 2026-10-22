package su.nightexpress.sunlight.hook.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.config.Config;

/**
 * Shared combat-tag source for all modules. Construct one per feature
 * (like {@code ProtectionManager}), pick the hook via
 * {@code setConfiguredHook}, then query {@code isInCombat}.
 * <p>
 * The hook id comes from the global {@code Combat.Hook} setting:
 * {@code auto} tries PvPManager first, then CombatLogX; a concrete id
 * pins one source; {@code none} disables tagging.
 */
public class CombatTracker {

    private final List<CombatHook> hooks = new ArrayList<>();
    private String configured = "auto";

    public CombatTracker() {
        this.register(new PvPManagerHook());
        this.register(new CombatLogXHook());
    }

    /**
     * Creates a tracker pre-configured from the global {@code Combat.Hook}
     * setting. This is the one-liner other modules should use.
     */
    public static @NotNull CombatTracker withGlobalConfig() {
        CombatTracker tracker = new CombatTracker();
        try {
            tracker.setConfiguredHook(Config.COMBAT_HOOK.get());
        } catch (Exception ignored) {
        }
        return tracker;
    }

    public void register(CombatHook hook) {
        if (hook != null) this.hooks.add(hook);
    }

    public List<CombatHook> getHooks() {
        return List.copyOf(this.hooks);
    }

    public void setConfiguredHook(String raw) {
        this.configured = raw == null || raw.isBlank() ? "auto" : raw.trim().toLowerCase(Locale.ROOT);
    }

    public @NotNull CombatHook getActiveHook() {
        if (!this.configured.equals("auto") && !this.configured.equals("none")) {
            for (CombatHook hook : this.hooks) {
                if (hook.getId().equals(this.configured) && hook.isAvailable()) return hook;
            }
            return NoCombatHook.INSTANCE;
        }
        for (CombatHook hook : this.hooks) {
            try {
                if (hook.isAvailable()) return hook;
            } catch (Exception ignored) {
            }
        }
        return NoCombatHook.INSTANCE;
    }

    public boolean isHooked() {
        return !(this.getActiveHook() instanceof NoCombatHook);
    }

    public boolean isInCombat(@NotNull Player player) {
        try {
            return this.getActiveHook().isInCombat(player);
        } catch (Exception exception) {
            // Fail-closed: a broken hook must not let players escape combat-tag restrictions.
            exception.printStackTrace();
            return true;
        }
    }

    private enum NoCombatHook implements CombatHook {
        INSTANCE;

        @Override
        public @NotNull String getId() {
            return "none";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public boolean isInCombat(@NotNull Player player) {
            return false;
        }
    }
}
