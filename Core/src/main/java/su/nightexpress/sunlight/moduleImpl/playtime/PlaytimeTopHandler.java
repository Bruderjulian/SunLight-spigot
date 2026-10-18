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

import su.nightexpress.sunlight.data.DataQueries;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeModule.PeriodKeys;
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

  public List<TopEntry> getTop(final PlaytimePeriod period) {
    this.topRequested = true;
    final PlaytimePeriod key = period == null ? PlaytimePeriod.ALLTIME : period;
    final List<TopEntry> cached = this.topCache.getOrDefault(key, List.of());
    final long refreshMs = Math.max(1000L, module.getSettings().getTopRefreshInterval() * 50L);
    if (System.currentTimeMillis() - this.topCacheTimestamp > refreshMs) {
      this.refreshTopCacheAsync();
    }
    // Already an immutable snapshot; callers must not mutate. Avoids copying
    // a 100-row list on every placeholder resolution.
    return cached;
  }

  public void refreshTopCacheAsync() {
    try {
      module.plugin().runTaskAsync(task -> this.refreshTopCache());
    } catch (final Exception exception) {
      this.refreshTopCache();
    }
  }

  /**
   * Rebuilds the leaderboard. Live users are always merged in (cheap); the
   * full database scan only runs when the board was actually requested and
   * the scan interval has passed, since playtime lives in the user table's
   * JSON properties blob and cannot be sorted in SQL. Runs off-thread, and
   * concurrent runs collapse into one via the in-flight guard.
   */
  public void refreshTopCache() {
    if (!this.topScanRunning.compareAndSet(false, true))
      return;
    try {
      this.refreshTopCacheUnsafe();
    } finally {
      this.topScanRunning.set(false);
    }
  }

  private void refreshTopCacheUnsafe() {
    PeriodKeys keys;
    try {
      keys = module.currentKeys();
    } catch (final Exception exception) {
      return;
    }

    final long now = System.currentTimeMillis();
    final boolean dbDue = now - this.lastDbScan > module.getSettings().getTopDbScanInterval() * 50L;
    final boolean wantDb = this.topRequested || this.lastDbScan == 0L;

    final Map<UUID, SunUser> merged = new LinkedHashMap<>();
    if (wantDb && dbDue) {
      final long scanStart = System.nanoTime();
      int rows = 0;
      try {
        for (final SunUser persisted : module.plugin().dataHandler().selectAny(
            module.plugin().dataHandler().getUsersTable(),
            DataQueries.SELECT_USER)) {
          if (persisted == null)
            continue;
          merged.put(persisted.getId(), persisted);
          rows++;
        }
        this.lastDbScan = now;
        this.topRequested = false;
        module.debug("Top database scan: " + rows + " users in " + ((System.nanoTime() - scanStart) / 1_000_000L)
            + "ms.");
      } catch (final Exception exception) {
        module.debug("Top refresh: database scan failed, falling back to cached users.");
      }
    }
    try {
      final Collection<SunUser> live = module.userManager().getAll();
      if (live != null) {
        for (final SunUser user : live) {
          if (user == null)
            continue;
          merged.put(user.getId(), user);
        }
      }
    } catch (final Exception exception) {
      // Live data is best-effort; persisted rows still produce a board.
    }

    this.rebuildTopBoards(merged, keys);
    this.topCacheTimestamp = System.currentTimeMillis();
  }

  private void rebuildTopBoards(final Map<UUID, SunUser> users, final PeriodKeys keys) {
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
    for (final SunUser user : users.values()) {
      if (user == null)
        continue;
      String name;
      UUID id;
      long total;
      long yearValue;
      long monthValue;
      long weekValue;
      long dayValue;
      try {
        name = user.getName();
        if (name == null || name.isBlank())
          continue;
        id = user.getId();
        if (id == null)
          continue;
        total = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
        yearValue = user.getPropertyOrDefault(PlaytimeProperties.YEAR_KEY) == keys.year()
            ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.YEAR))
            : 0L;
        monthValue = user.getPropertyOrDefault(PlaytimeProperties.MONTH_KEY) == keys.month()
            ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.MONTH))
            : 0L;
        weekValue = user.getPropertyOrDefault(PlaytimeProperties.WEEK_KEY) == keys.week()
            ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.WEEK))
            : 0L;
        dayValue = user.getPropertyOrDefault(PlaytimeProperties.DAY_KEY) == keys.day()
            ? Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.DAY))
            : 0L;
      } catch (final Exception exception) {
        continue;
      }
      if (total > 0L)
        alltime.add(new TopEntry(id, name, total));
      if (yearValue > 0L)
        year.add(new TopEntry(id, name, yearValue));
      if (monthValue > 0L)
        month.add(new TopEntry(id, name, monthValue));
      if (weekValue > 0L)
        week.add(new TopEntry(id, name, weekValue));
      if (dayValue > 0L)
        day.add(new TopEntry(id, name, dayValue));
    }

    final Comparator<TopEntry> order = Comparator.comparingLong(TopEntry::value).reversed();
    this.topCache.put(PlaytimePeriod.ALLTIME, topSnapshot(alltime, order));
    this.topCache.put(PlaytimePeriod.YEAR, topSnapshot(year, order));
    this.topCache.put(PlaytimePeriod.MONTH, topSnapshot(month, order));
    this.topCache.put(PlaytimePeriod.WEEK, topSnapshot(week, order));
    this.topCache.put(PlaytimePeriod.DAY, topSnapshot(day, order));
  }

  private static List<TopEntry> topSnapshot(List<TopEntry> entries, final Comparator<TopEntry> order) {
    if (entries.isEmpty())
      return List.of();
    entries.sort(order);
    if (entries.size() > PlaytimeModule.TOP_CACHE_LIMIT) {
      entries = new ArrayList<>(entries.subList(0, PlaytimeModule.TOP_CACHE_LIMIT));
    }
    return List.copyOf(entries);
  }
}
