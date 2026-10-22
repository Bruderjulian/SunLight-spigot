package su.nightexpress.sunlight.moduleImpl.profiles.warmup;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.nightcore.bridge.scheduler.AdaptedTask;

import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfilesModule;
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesLang;

/**
 * Stand-still warmup for profile switches. Moving, taking damage, dying or
 * leaving cancels the pending switch. Completion delegates back to
 * {@link ProfilesModule#executeSwitch}, which re-validates every guard.
 */
public class ProfileWarmupManager {

    private final SunLightPlugin plugin;
    private final ProfilesModule module;
    private final Map<UUID, PendingSwitch> pending = new ConcurrentHashMap<>();

    private record PendingSwitch(String profileId, Location origin, AdaptedTask task) {
    }

    public ProfileWarmupManager(@NotNull SunLightPlugin plugin, @NotNull ProfilesModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    public void shutdown() {
        this.pending.values().forEach(entry -> {
            try {
                entry.task().cancel();
            } catch (Exception ignored) {
            }
        });
        this.pending.clear();
    }

    public boolean hasPending(@NotNull Player player) {
        return this.pending.containsKey(player.getUniqueId());
    }

    public void request(@NotNull Player player, @NotNull String profileId, int seconds) {
        this.cancel(player, true);
        if (seconds <= 0) {
            if (!player.isOnline() || player.isDead()) return;
            this.module.executeSwitch(player, profileId);
            return;
        }

        Location origin = player.getLocation().clone();
        this.module.sendPrefixed(ProfilesLang.WARMUP_START, player,
            builder -> builder.with(su.nightexpress.sunlight.SLPlaceholders.GENERIC_TIME, () -> seconds + "s"));

        AdaptedTask task = this.plugin.scheduler().runTaskLater(() -> {
            PendingSwitch current = this.pending.remove(player.getUniqueId());
            if (current == null) return;
            if (!player.isOnline() || player.isDead()) return;
            try {
                this.module.executeSwitch(player, profileId);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }, seconds * 20L);

        this.pending.put(player.getUniqueId(), new PendingSwitch(profileId, origin, task));
    }

    public boolean cancelOnMove(@NotNull Player player) {
        PendingSwitch current = this.pending.get(player.getUniqueId());
        if (current == null) return false;
        Location origin = current.origin();
        Location now = player.getLocation();
        if (origin.getBlockX() == now.getBlockX()
            && origin.getBlockY() == now.getBlockY()
            && origin.getBlockZ() == now.getBlockZ()) return false;
        this.cancel(player, false);
        this.module.sendPrefixed(ProfilesLang.WARMUP_CANCEL_MOVE, player);
        return true;
    }

    public void cancelOnDamage(@NotNull Player player) {
        if (!this.hasPending(player)) return;
        this.cancel(player, false);
        this.module.sendPrefixed(ProfilesLang.WARMUP_CANCEL_DAMAGE, player);
    }

    public void cancel(@NotNull Player player, boolean silent) {
        PendingSwitch current = this.pending.remove(player.getUniqueId());
        if (current == null) return;
        try {
            current.task().cancel();
        } catch (Exception ignored) {
        }
    }
}
