package su.nightexpress.sunlight.moduleImpl.playtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;

import su.nightexpress.sunlight.data.DataQueries;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;
import su.nightexpress.sunlight.user.SunUser;

public class PlaytimeTopHandler {
  private final PlaytimeModule module;
  protected final Map<PlaytimePeriod, List<TopEntry>> topCache;

  private final AtomicBoolean topScanRunning = new AtomicBoolean(false);
  private volatile long topCacheTimestamp;
  private volatile long lastDbScan;
  protected volatile boolean topRequested;

  public PlaytimeTopHandler(final PlaytimeModule module) {
    this.module = module;
    this.topCache = new ConcurrentHashMap<>();
    this.topCacheTimestamp = 0L;
    this.lastDbScan = 0L;
    this.topRequested = false;
  }

  public void unload() {
    this.topCache.clear();
    this.topCacheTimestamp = 0L;
    this.lastDbScan = 0L;
    this.topRequested = false;
  }

  public record TopEntry(UUID id, String name, long value) {
  }

  public List<TopEntry> getTop(PlaytimePeriod period) {
    this.topRequested = true;
    period = period == null ? PlaytimePeriod.ALLTIME : period;
    final List<TopEntry> cached = this.topCache.getOrDefault(period, List.of());
    if (System.currentTimeMillis() - this.topCacheTimestamp > module.getSettings().getWeekReminderWindowMs()) {
      this.refreshTopCacheAsync();
    }
    return cached;
  }

  public void refreshTopCacheAsync() {
    try {
      Bukkit.getScheduler().runTaskAsynchronously(module.plugin(), task -> {
        refreshTopCache();
      });
    } catch (final Exception exception) {
      refreshTopCache();
    }
  }

  private void refreshTopCache() {
    if (!this.topScanRunning.compareAndSet(false, true))
      return;
    try {
      this.refreshTopCacheUnsafe();
    } finally {
      this.topScanRunning.set(false);
    }
  }

  private void refreshTopCacheUnsafe() {
    final long now = System.currentTimeMillis();
    final boolean useDB = (this.topRequested || this.lastDbScan == 0L)
        && now - this.lastDbScan > module.getSettings().getTopDbScanInterval() * 50L;

    final Map<UUID, SunUser> merged = new LinkedHashMap<>();
    Collection<SunUser> users = null;

    if (useDB) {
      final long scanStart = System.nanoTime();
      try {
        users = module.plugin().dataHandler().selectAny(
            module.plugin().dataHandler().getUsersTable(),
            DataQueries.SELECT_USER);
      } catch (final Exception exception) {
        module.debug("Top refresh: database scan failed, falling back to cached users.");
      }
      for (final SunUser persisted : users) {
        if (persisted == null) {
          continue;
        }
        merged.put(persisted.getId(), persisted);
      }
      this.lastDbScan = now;
      this.topRequested = false;
      module.debug("Top database scan: " + merged.size() + "/" + users.size() + " users in "
          + ((System.nanoTime() - scanStart) / 1_000_000L)
          + "ms.");
    }
    try {
      final Collection<SunUser> live = module.userManager().getAll();
      if (live == null) {
        return;
      }
      for (final SunUser user : live) {
        if (user == null) {
          continue;
        }
        merged.put(user.getId(), user);
      }
    } catch (final Exception exception) {
      // Live data is best-effort; persisted rows still produce a board.
    }

    this.rebuildTopBoards(merged);
    this.topCacheTimestamp = System.currentTimeMillis();
  }

  private void rebuildTopBoards(final Map<UUID, SunUser> users) {
    if (users.isEmpty()) {
      for (final PlaytimePeriod period : PlaytimePeriod.values()) {
        this.topCache.put(period, List.of());
      }
      return;
    }

    // Single pass over users instead of one pass per period: each user's five
    // bucket values are resolved once, then appended to the matching boards.
    final List<TopEntry> alltime = new ArrayList<>();
    final List<TopEntry> year = new ArrayList<>();
    final List<TopEntry> month = new ArrayList<>();
    final List<TopEntry> week = new ArrayList<>();
    final List<TopEntry> day = new ArrayList<>();
    String name;
    UUID id;
    long value;
    for (final SunUser user : users.values()) {
      if (user == null) {
        continue;
      }
      name = user.getName();
      if (name == null || name.isBlank()) {
        continue;
      }
      id = user.getId();
      if (id == null) {
        continue;
      }
      for (Map.Entry<PlaytimePeriod, List<TopEntry>> entry : topCache.entrySet()) {
        value = module.getEffectivePlaytime(user, entry.getKey());
        if (value > 0L) {
          alltime.add(new TopEntry(id, name, value));
        }
      }
    }

    final Comparator<TopEntry> order = (c1, c2) -> -Long.compare(c2.value, c1.value);
    this.topCache.put(PlaytimePeriod.ALLTIME, topSnapshot(alltime, order));
    this.topCache.put(PlaytimePeriod.YEAR, topSnapshot(year, order));
    this.topCache.put(PlaytimePeriod.MONTH, topSnapshot(month, order));
    this.topCache.put(PlaytimePeriod.WEEK, topSnapshot(week, order));
    this.topCache.put(PlaytimePeriod.DAY, topSnapshot(day, order));
  }

  private static List<TopEntry> topSnapshot(List<TopEntry> entries, final Comparator<TopEntry> order) {
    if (entries.isEmpty()) {
      return entries;
    }
    entries.sort(order);
    if (entries.size() > PlaytimeModule.TOP_CACHE_LIMIT) {
      entries = entries.subList(0, PlaytimeModule.TOP_CACHE_LIMIT);
    }
    return List.copyOf(entries);
  }
}
