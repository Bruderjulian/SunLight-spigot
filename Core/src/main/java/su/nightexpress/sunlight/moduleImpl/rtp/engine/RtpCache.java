package su.nightexpress.sunlight.moduleImpl.rtp.engine;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import su.nightexpress.nightcore.bridge.scheduler.AdaptedTask;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.TimeUtil;
import su.nightexpress.sunlight.moduleImpl.rtp.RTPModule;

public class RtpCache {
  private final Map<String, Queue<Location>> locationCache;
  private final Set<String> refilling;
  private AdaptedTask cacheRefillTask;
  private final RTPModule module;

  public RtpCache(final RTPModule module) {
    this.module = module;
    this.locationCache = new ConcurrentHashMap<>();
    this.refilling = ConcurrentHashMap.newKeySet();
  }

  public void start() {
    shutdown();

    final int interval = module.getSettings().getCacheRefillInterval();
    if (!module.getSettings().isCacheEnabled() || interval <= 0)
      return;

    final long ticks = TimeUtil.secondsToTicks(interval);
    this.cacheRefillTask = this.module.plugin().scheduler()
        .runTaskTimer(this::refillCachedWorlds, ticks, ticks);
  }

  public void shutdown() {
    if (this.cacheRefillTask != null) {
      this.cacheRefillTask.cancel();
      this.cacheRefillTask = null;
    }

    this.locationCache.clear();
    this.refilling.clear();
  }

  public Location retrieveLocation(final World world, final LookupRange lookupRange, final String worldName) {
    final Queue<Location> queue = locationCache.get(worldName);
    if (queue != null) {
      Location location;
      while ((location = queue.poll()) != null) {
        if (location.getWorld() == null
            || module.getSettings().isProtectionEnabled() && module.getEngine().isProtected(location)) {
          continue;
        }
        if (queue.size() <= module.getSettings().getCacheRefillThreshold()) {
          Bukkit.getScheduler().runTaskAsynchronously(module.plugin(), () -> {
            refillCache(world, lookupRange);
          });
        }
        return location;
      }
    }

    Bukkit.getScheduler().runTaskAsynchronously(module.plugin(), () -> {
      refillCache(world, lookupRange);
    });
    return null;
  }

  public void refillCache(final World world, final LookupRange range) {
    final String key = LowerCase.INTERNAL.apply(world.getName());
    if (!this.refilling.add(key))
      return;

    final Queue<Location> queue = this.locationCache.computeIfAbsent(key, unused -> new ConcurrentLinkedQueue<>());
    final int needed = module.getSettings().getCacheSize() - queue.size();

    CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
    for (int index = 0; index < needed; index++) {
      chain = chain.thenCompose(ignored -> module.getEngine().findLocation(world, range))
          .thenAccept(found -> found.ifPresent(location -> {
            queue.add(location);
          }));
    }

    chain.whenComplete((ignored, error) -> {
      if (error != null)
        this.module.error("RTP location cache refill failed for world '" + world.getName() + "': "
            + error.getMessage());

      this.refilling.remove(key);
    });
  }

  private void refillCachedWorlds() {
    for (final String worldName : this.locationCache.keySet()) {
      final World world = this.module.plugin().getServer().getWorld(worldName);
      if (world == null)
        return;

      final LookupRange range = module.getEngine().getWorldRange(world.getName());
      if (range != null) {
        this.refillCache(world, range);
      }
    }
  }
}
