package su.nightexpress.sunlight.moduleImpl.playtime;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeTopHandler.TopEntry;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimeMilestone;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;

public class PlaytimePlaceholders {
  private final PlaytimeModule module;

  public PlaytimePlaceholders(final PlaytimeModule module) {
    this.module = module;
  }

  /**
   * All placeholders served by this module ({@code %sunlight_<key>%}).
   * <p>
   * Counters: {@code playtime_total|year|month|week|day|session}, each with a
   * {@code _formatted} variant. Streaks: {@code playtime_streak_daily},
   * {@code playtime_streak_weekly}.
   * <p>
   * Goals, for {@code day|week|month}: {@code playtime_goal_<scope>} (ms),
   * {@code playtime_goal_<scope>_formatted},
   * {@code playtime_goal_<scope>_remaining[_formatted]},
   * {@code playtime_goal_<scope>_progress} (0-100). Generic form:
   * {@code playtime_goal_<scope>[_metric]}, e.g.
   * {@code %sunlight_playtime_goal_day_remaining%}.
   * <p>
   * Milestones: {@code playtime_milestones} (one-shots claimed),
   * {@code playtime_milestones_total} (one-shots configured),
   * {@code playtime_milestone_next} (name),
   * {@code playtime_milestone_next_remaining[_formatted]},
   * {@code playtime_milestone_next_progress}.
   * <p>
   * Leaderboard: {@code playtime_top_<rank>_<name|time|formatted>} and
   * {@code playtime_top_<period>_<rank>_<field>}, e.g.
   * {@code %sunlight_playtime_top_day_1_name%}.
   * Personal rank (1-based, {@code 0} when unranked):
   * {@code playtime_rank[_<period>]}.
   * <p>
   * Reset countdowns: {@code playtime_reset[_<day|week|month|year>][_formatted]},
   * e.g. {@code %sunlight_playtime_reset_day_formatted%}.
   */
  public void registerPlaceholders(final PlaceholderRegistry registry) {
    registry.register("playtime_total",
        (player, payload) -> String.valueOf(getEffectivePlaytime(player, PlaytimePeriod.ALLTIME)));
    registry.register("playtime_total_formatted",
        (player, payload) -> TimeUtil.formatMillis(getEffectivePlaytime(player, PlaytimePeriod.ALLTIME)));
    registry.register("playtime_year",
        (player, payload) -> String.valueOf(getEffectivePlaytime(player, PlaytimePeriod.YEAR)));
    registry.register("playtime_year_formatted",
        (player, payload) -> TimeUtil.formatMillis(getEffectivePlaytime(player, PlaytimePeriod.YEAR)));
    registry.register("playtime_month",
        (player, payload) -> String.valueOf(getEffectivePlaytime(player, PlaytimePeriod.MONTH)));
    registry.register("playtime_month_formatted",
        (player, payload) -> TimeUtil.formatMillis(getEffectivePlaytime(player, PlaytimePeriod.MONTH)));
    registry.register("playtime_week",
        (player, payload) -> String.valueOf(getEffectivePlaytime(player, PlaytimePeriod.WEEK)));
    registry.register("playtime_week_formatted",
        (player, payload) -> TimeUtil.formatMillis(getEffectivePlaytime(player, PlaytimePeriod.WEEK)));
    registry.register("playtime_day",
        (player, payload) -> String.valueOf(getEffectivePlaytime(player, PlaytimePeriod.DAY)));
    registry.register("playtime_day_formatted",
        (player, payload) -> TimeUtil.formatMillis(getEffectivePlaytime(player, PlaytimePeriod.DAY)));
    registry.register("playtime_session", (player, payload) -> String.valueOf(getEffectivePlaytime(player,
        PlaytimePeriod.SESSION)));
    registry.register("playtime_session_formatted",
        (player, payload) -> TimeUtil.formatMillis(getEffectivePlaytime(player, PlaytimePeriod.SESSION)));
    registry.register("playtime_streak_daily", (player, payload) -> String.valueOf(getStreak(player, true)));
    registry.register("playtime_streak_weekly", (player, payload) -> String.valueOf(getStreak(player, false)));
    registry.register("playtime_goal_day", (player, payload) -> goalResponse(player, "day", payload));
    registry.register("playtime_goal_week", (player, payload) -> goalResponse(player, "week", payload));
    registry.register("playtime_goal_month", (player, payload) -> goalResponse(player, "month", payload));
    registry.register("playtime_goal", (player, payload) -> goalResponse(player, null, payload));
    registry.register("playtime_milestones", (player, payload) -> milestonesResponse(player, payload));
    registry.register("playtime_milestone_next", (player, payload) -> milestoneNextResponse(player, "name"));
    registry.register("playtime_milestone_next_remaining",
        (player, payload) -> milestoneNextResponse(player, "remaining"));
    registry.register("playtime_milestone_next_remaining_formatted",
        (player, payload) -> milestoneNextResponse(player, "remaining_formatted"));
    registry.register("playtime_milestone_next_progress",
        (player, payload) -> milestoneNextResponse(player, "progress"));
    registry.register("playtime_top", (player, payload) -> topResponse(payload));
    registry.register("playtime_rank", (player, payload) -> rankResponse(player, payload));
    registry.register("playtime_reset", (player, payload) -> resetResponse(payload));
  }

  private long getEffectivePlaytime(final Player player, final PlaytimePeriod period) {
    if (player == null)
      return 0L;
    final SunUser user = module.userManager().getOrFetch(player);
    if (user == null)
      return 0L;
    return module.getEffectivePlaytime(user, period);
  }

  private String goalResponse(final Player player, final String scopeOrNull, final String payload) {
    String scope = scopeOrNull;
    String metric = payload == null ? "" : Utils.lowercase(payload.trim());

    if (scope == null) {
      if (metric == null || metric.isBlank())
        return "";
      final int separator = metric.indexOf('_');
      if (separator < 0) {
        scope = metric;
        metric = "";
      } else {
        scope = metric.substring(0, separator);
        metric = metric.substring(separator + 1);
      }
    }
    if (scope == null || scope.isBlank())
      return "";
    if (player == null)
      return "0";

    final boolean day = scope.equals("day") || scope.equals("daily");
    final boolean week = scope.equals("week") || scope.equals("weekly");
    final boolean month = !day && !week && (scope.equals("month") || scope.equals("monthly"));
    if (!day && !week && !month)
      return "";

    final SunUser user = module.userManager().getOrFetch(player);
    if (user == null)
      return "0";

    final long goal = module.milestoneHandler.getEffectiveGoalMs(user, day, week, month);
    final PlaytimePeriod period = day ? PlaytimePeriod.DAY : week ? PlaytimePeriod.WEEK : PlaytimePeriod.MONTH;
    final long current = module.getEffectivePlaytime(user, period);

    return switch (metric == null ? "" : metric) {
      case "" -> String.valueOf(Math.max(0L, goal));
      case "formatted" -> TimeUtil.formatMillis(Math.max(0L, goal));
      case "remaining" -> String.valueOf(goal <= 0L ? 0L : Math.max(0L, goal - current));
      case "remaining_formatted" -> TimeUtil.formatMillis(goal <= 0L ? 0L : Math.max(0L, goal - current));
      case "progress" -> String.valueOf(goal <= 0L || current <= 0L ? 0L : Math.min(100L, current * 100L / goal));
      default -> "";
    };
  }

  private String milestonesResponse(final Player player, final String payload) {
    if (player == null) {
      return "0";
    }
    final String metric = payload == null ? "" : Utils.lowercase(payload.trim());
    if (!metric.isEmpty() && !metric.equals("total")) {
      return "";
    }

    return String.valueOf(module.milestoneHandler.getMilestonesClaimed(player, metric.equals("total")));
  }

  private String milestoneNextResponse(final Player player, final String metric) {
    if (player == null)
      return metric.equals("name") ? "" : "0";
    final SunUser user = module.userManager().getOrFetch(player);
    if (user == null)
      return metric.equals("name") ? "" : "0";

    final PlaytimeMilestone next = module.milestoneHandler.nextMilestone(user);
    if (next == null)
      return metric.equals("progress") ? "100" : metric.equals("name") ? "" : "0";

    final long total = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
    return switch (metric) {
      case "name" -> next.name();
      case "remaining" -> String.valueOf(Math.max(0L, next.thresholdMs() - total));
      case "remaining_formatted" -> TimeUtil.formatMillis(Math.max(0L, next.thresholdMs() - total));
      case "progress" -> String.valueOf(Math.min(100L, total * 100L / Math.max(1L, next.thresholdMs())));
      default -> "";
    };
  }

  private String topResponse(final String payload) {
    module.topHandler.topRequested = true;
    if (payload == null || payload.isBlank())
      return "";

    final String[] tokens = Utils.lowercase(payload.trim()).split("_");
    PlaytimePeriod period = PlaytimePeriod.ALLTIME;
    String rankRaw;
    String field;

    if (tokens.length == 2) {
      rankRaw = tokens[0];
      field = tokens[1];
    } else if (tokens.length == 3) {
      period = PlaytimePeriod.fromString(tokens[0]);
      rankRaw = tokens[1];
      field = tokens[2];
    } else if (tokens.length == 4 && (tokens[2].equals("time") && tokens[3].equals("formatted"))) {
      period = PlaytimePeriod.fromString(tokens[0]);
      rankRaw = tokens[1];
      field = "time_formatted";
    } else {
      return "";
    }

    int rank;
    try {
      rank = Integer.parseInt(rankRaw);
    } catch (final NumberFormatException exception) {
      return "";
    }
    if (rank < 1 || rank > PlaytimeModule.TOP_CACHE_LIMIT)
      return "";

    final List<TopEntry> board = module.topHandler.topCache.getOrDefault(period, List.of());
    if (rank > board.size())
      return "";
    final TopEntry entry = board.get(rank - 1);

    return switch (field) {
      case "name" -> entry.name();
      case "time" -> String.valueOf(Math.max(0L, entry.value()));
      case "formatted", "time_formatted" -> TimeUtil.formatMillis(Math.max(0L, entry.value()));
      default -> "";
    };
  }

  private String rankResponse(final Player player, final String payload) {
    module.topHandler.topRequested = true;
    if (player == null)
      return "0";

    final PlaytimePeriod period = payload == null || payload.isBlank()
        ? PlaytimePeriod.ALLTIME
        : PlaytimePeriod.fromString(payload.trim());

    final List<TopEntry> board = module.topHandler.topCache.getOrDefault(period, List.of());
    final UUID id = player.getUniqueId();
    for (int index = 0; index < board.size(); index++) {
      if (board.get(index).id().equals(id)) {
        return String.valueOf(index + 1);
      }
    }
    return "0";
  }

  private String resetResponse(final String payload) {
    String raw = payload == null ? "" : Utils.lowercase(payload.trim());
    final boolean formatted = raw.endsWith("_formatted");
    if (formatted)
      raw = raw.substring(0, raw.length() - "_formatted".length());

    final String period = raw.isBlank() ? "day" : raw;
    ZoneId zone = module.zoneId;
    if (zone == null)
      zone = ZoneId.systemDefault();

    ZonedDateTime now;
    ZonedDateTime next;
    try {
      now = ZonedDateTime.now(zone);
      next = switch (period) {
        case "day", "daily" -> now.toLocalDate().plusDays(1).atStartOfDay(zone);
        case "week", "weekly" ->
          now.toLocalDate().with(java.time.DayOfWeek.MONDAY).plusWeeks(1).atStartOfDay(zone);
        case "month", "monthly" -> now.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay(zone);
        case "year", "yearly" -> now.toLocalDate().withDayOfYear(1).plusYears(1).atStartOfDay(zone);
        default -> null;
      };
    } catch (final Exception exception) {
      return formatted ? TimeUtil.formatMillis(0L) : "0";
    }
    if (next == null)
      return formatted ? TimeUtil.formatMillis(0L) : "0";

    final long remaining = Math.max(0L, next.toInstant().toEpochMilli() - now.toInstant().toEpochMilli());
    return formatted ? TimeUtil.formatMillis(remaining) : String.valueOf(remaining);
  }

  private int getStreak(final Player player, final boolean daily) {
    if (player == null)
      return 0;
    final SunUser user = module.userManager().getOrFetch(player);
    if (user == null)
      return 0;
    return daily ? Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK))
        : Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK));
  }

}