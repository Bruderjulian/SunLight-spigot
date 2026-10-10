package su.nightexpress.sunlight.moduleImpl.warmups;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.warmups.config.WarmupsConfig;
import su.nightexpress.sunlight.moduleImpl.warmups.config.WarmupsLang;
import su.nightexpress.sunlight.moduleImpl.warmups.config.WarmupsPerms;
import su.nightexpress.sunlight.moduleImpl.warmups.impl.TeleportWarmup;
import su.nightexpress.sunlight.moduleImpl.warmups.impl.Warmup;
import su.nightexpress.sunlight.moduleImpl.warmups.listener.WarmupsListener;
import su.nightexpress.sunlight.teleport.TeleportContext;
import su.nightexpress.sunlight.teleport.TeleportManager;
import su.nightexpress.sunlight.teleport.TeleportType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarmupsModule extends Module {

    private final TeleportManager teleportManager;

    private final Map<UUID, Warmup> warmupByIdMap;

    public WarmupsModule(ModuleDefinition<WarmupsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.teleportManager = plugin.teleportManager();
        this.warmupByIdMap = new ConcurrentHashMap<>();
    }

    @Override
    protected void loadModule(FileConfig config) {
        config.initializeOptions(WarmupsConfig.class);
        this.plugin.injectLang(WarmupsLang.class);

        this.addListener(new WarmupsListener(this.plugin, this));
        // Must run on the main thread: tick touches Player#getLocation,
        // BossBar and particles which are not thread-safe.
        this.addTask(this::tickWarmups, Math.max(1L, WarmupsConfig.WARMUP_TICK_INTERVAL.get()));
    }

    @Override
    protected void unloadModule() {
        // Fail-safe: notify + refund is handled by Warmup#onCancel implementations;
        // never silently drop pending teleports here.
        new HashSet<>(this.warmupByIdMap.values()).forEach(warmup -> {
            try {
                warmup.cancel(false);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
        this.warmupByIdMap.clear();
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {

    }

    private void tickWarmups() {
        if (this.warmupByIdMap.isEmpty()) return;
        this.warmupByIdMap.values().removeIf(warmup -> {
            try {
                this.tickWarmup(warmup);
            } catch (Exception exception) {
                exception.printStackTrace();
                return true;
            }
            return warmup.isCompleted();
        });
    }

    private void tickWarmup(Warmup warmup) {
        if (!warmup.getPlayer().isOnline()) {
            this.warmupByIdMap.remove(warmup.getPlayer().getUniqueId());
            try {
                warmup.cancel(false);
            } catch (Exception ignored) {
            }
            return;
        }
        if (WarmupsConfig.WARMUP_CANCEL_ON_MOVE.get() && warmup.isMoved()) {
            this.cancelWarmup(warmup.getPlayer());
            return;
        }

        warmup.onTick();

        if (warmup.isCompleted()) {
            warmup.complete(); // Already on the main thread.
            this.warmupByIdMap.remove(warmup.getPlayer().getUniqueId());
        }
    }

    public Warmup getWarmup(Player player) {
        return this.warmupByIdMap.get(player.getUniqueId());
    }

    public boolean hasWarmup(Player player) {
        return this.getWarmup(player) != null;
    }

    public Set<Warmup> getWarmups() {
        return new HashSet<>(this.warmupByIdMap.values());
    }

    public boolean canHandleTeleport(TeleportType type) {
        return WarmupsConfig.TELEPORT_HANDLED_TYPES.get().contains(type);
    }

    public boolean canHandleTeleport(Player player, TeleportType type) {
        return !player.hasPermission(WarmupsPerms.BYPASS_TELEPORT) && this.canHandleTeleport(type);
    }

    public void cancelWarmup(Player player) {
        this.cancelWarmup(player, false);
    }

    public void cancelWarmup(Player player, boolean silent) {
        Warmup warmup = this.warmupByIdMap.remove(player.getUniqueId());
        if (warmup == null)
            return;

        warmup.cancel(silent);
    }

    public void addWarmup(Player player, Warmup warmup) {
        if (warmup.getValue() <= 0) {
            warmup.complete();
            return;
        }

        this.cancelWarmup(player, true);

        warmup.init();

        this.warmupByIdMap.put(player.getUniqueId(), warmup);
    }

    public void handleTeleport(TeleportContext context, TeleportType type) {
        Player player = context.getTarget();
        Location location = context.getDestination();
        int value = WarmupsConfig.TELEPORT_WARMUPS_BY_RANK.get().getSmallest(player);
        Runnable callback = () -> this.teleportManager.move(context); // Simply pass the same context into next
                                                                      // teleportation "stage" after a delay is passed.

        Warmup warmup = new TeleportWarmup(this, player, value, location, callback);

        this.addWarmup(player, warmup);
    }
}
