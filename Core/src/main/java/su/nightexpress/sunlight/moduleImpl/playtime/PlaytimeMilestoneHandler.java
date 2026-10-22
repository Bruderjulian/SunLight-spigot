package su.nightexpress.sunlight.moduleImpl.playtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.api.event.PlaytimeGoalReachedEvent;
import su.nightexpress.sunlight.api.event.PlaytimeMilestoneEvent;
import su.nightexpress.sunlight.moduleImpl.playtime.config.PlaytimeLang;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimeMilestone;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;

public class PlaytimeMilestoneHandler {
  private final PlaytimeModule module;
  public volatile Map<String, PlaytimeMilestone> milestones;
  public volatile List<PlaytimeMilestone> milestoneList;

  public PlaytimeMilestoneHandler(final PlaytimeModule module) {
    this.module = module;
    this.milestones = Map.of();
    this.milestoneList = List.of();
  }

  public void unload() {
    this.milestones = Map.of();
    this.milestoneList = List.of();
  }

  public Map<String, PlaytimeMilestone> getMilestones() {
    return Map.copyOf(this.milestones);
  }

  public void loadMilestones(final FileConfig config) {
    if (config.getConfigurationSection("Playtime.Milestones") == null) {
      config.addMissing("Playtime.Milestones.starter.Name", "Starter");
      config.addMissing("Playtime.Milestones.starter.Minutes", 60L);
      config.addMissing("Playtime.Milestones.starter.Repeatable", false);
      config.addMissing("Playtime.Milestones.starter.Rewards", List.of("eco give %player_name% 500"));
      config.addMissing("Playtime.Milestones.veteran.Name", "Veteran");
      config.addMissing("Playtime.Milestones.veteran.Minutes", 600L);
      config.addMissing("Playtime.Milestones.veteran.Repeatable", false);
      config.addMissing("Playtime.Milestones.veteran.Rewards", List.of("eco give %player_name% 5000"));
      config.addMissing("Playtime.Milestones.grinder.Name", "Grinder");
      config.addMissing("Playtime.Milestones.grinder.Minutes", 120L);
      config.addMissing("Playtime.Milestones.grinder.Repeatable", true);
      config.addMissing("Playtime.Milestones.grinder.Rewards", List.of("eco give %player_name% 250"));
    }

    final ConfigurationSection section = config.getConfigurationSection("Playtime.Milestones");
    if (section == null) {
      this.milestones = Map.of();
      this.milestoneList = List.of();
      return;
    }

    final Map<String, PlaytimeMilestone> parsed = new LinkedHashMap<>();
    for (final String id : section.getKeys(false)) {
      final String key = Utils.lowercase(id);
      if (key == null || key.isBlank())
        continue;
      final String base = "Playtime.Milestones." + id + ".";
      final long minutes = Math.max(0L, config.getLong(base + "Minutes", 0L));
      if (minutes <= 0L)
        continue;
      String name = config.getString(base + "Name", id);
      if (name == null || name.isBlank())
        name = id;
      final boolean repeatable = config.getBoolean(base + "Repeatable", false);
      final List<String> rewards = config.getStringList(base + "Rewards");
      parsed.put(key, new PlaytimeMilestone(key, name, minutes * 60_000L, repeatable, List.copyOf(rewards)));
    }

    final List<PlaytimeMilestone> ordered = new ArrayList<>(parsed.values());
    ordered.sort(Comparator.comparingLong(PlaytimeMilestone::thresholdMs));
    this.milestones = Map.copyOf(parsed);
    this.milestoneList = List.copyOf(ordered);
  }

  public void checkMilestones(final Player player, final SunUser user) {
    final List<PlaytimeMilestone> ordered = this.milestoneList;
    if (ordered.isEmpty())
      return;

    final long total = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.TOTAL));
    if (total <= 0L)
      return;

    // Read-only fast path: no map copy unless something actually triggers.
    final Map<String, Integer> progress = user.getPropertyOrDefault(PlaytimeProperties.MILESTONE_PROGRESS);
    Map<String, Integer> updated = null;

    for (final PlaytimeMilestone milestone : ordered) {
      if (milestone.thresholdMs() <= 0L || total < milestone.thresholdMs())
        continue;

      if (milestone.repeatable()) {
        final int reached = (int) Math.min(Integer.MAX_VALUE, total / milestone.thresholdMs());
        final int claimed = Math.max(0, progress.getOrDefault(milestone.id(), 0));
        if (reached > claimed) {
          if (updated == null)
            updated = new HashMap<>(progress);
          updated.put(milestone.id(), reached);
          module.sendPrefixed(PlaytimeLang.MILESTONE_REWARD, player, b -> b
              .with(SLPlaceholders.GENERIC_NAME, milestone::name));
          module.dispatchRewardCommands(player, milestone.rewards());
          module.plugin().getPluginManager().callEvent(new PlaytimeMilestoneEvent(player, milestone, reached));
        }
      } else if (!progress.containsKey(milestone.id())
          && (updated == null || !updated.containsKey(milestone.id()))) {
        if (updated == null)
          updated = new HashMap<>(progress);
        updated.put(milestone.id(), 1);
        module.sendPrefixed(PlaytimeLang.MILESTONE_REWARD, player, b -> b
            .with(SLPlaceholders.GENERIC_NAME, milestone::name));
        module.dispatchRewardCommands(player, milestone.rewards());
        module.plugin().getPluginManager().callEvent(new PlaytimeMilestoneEvent(player, milestone, 1));
      }
    }

    if (updated != null) {
      user.setProperty(PlaytimeProperties.MILESTONE_PROGRESS, updated);
    }
  }

  public int getMilestonesClaimed(final Player player, final boolean total) {
    if (player == null) {
      return 0;
    }

    final Map<String, PlaytimeMilestone> all = module.milestoneHandler.getMilestones();
    if (all.isEmpty()) {
      return 0;
    }
    if (total) {
      int claimed = 0;
      for (final PlaytimeMilestone milestone : all.values()) {
        if (!milestone.repeatable())
          claimed++;
      }
      return claimed;
    }

    final SunUser user = module.userManager().getOrFetch(player);
    if (user == null) {
      return 0;
    }
    final Map<String, Integer> progress = user.getPropertyOrDefault(PlaytimeProperties.MILESTONE_PROGRESS);
    int claimed = 0;
    for (final PlaytimeMilestone milestone : all.values()) {
      if (!milestone.repeatable() && progress.getOrDefault(milestone.id(), 0) > 0) {
        claimed++;
      }
    }
    return claimed;
  }

  public PlaytimeMilestone nextMilestone(final SunUser user) {
    final Map<String, Integer> progress = user.getPropertyOrDefault(PlaytimeProperties.MILESTONE_PROGRESS);
    for (final PlaytimeMilestone milestone : module.milestoneHandler.milestoneList) {
      if (!milestone.repeatable() && progress.getOrDefault(milestone.id(), 0) <= 0) {
        return milestone;
      }
    }
    return null;
  }

  public long getEffectiveGoalMs(final SunUser user, final boolean day, final boolean week, final boolean month) {
    if (day) {
      final long override = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.GOAL_DAY));
      return override > 0L ? override : Math.max(0L, module.getSettings().getDailyGoalMs());
    }
    if (week) {
      final long override = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.GOAL_WEEK));
      return override > 0L ? override : Math.max(0L, module.getSettings().getWeeklyGoalMs());
    }
    final long override = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.GOAL_MONTH));
    return override > 0L ? override : Math.max(0L, module.getSettings().getMonthlyGoalMs());
  }

  public void checkGoals(final Player player, final SunUser user) {
    final long day = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.DAY));
    final long week = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.WEEK));
    final long month = Math.max(0L, user.getPropertyOrDefault(PlaytimeProperties.MONTH));

    final long dayKey = user.getPropertyOrDefault(PlaytimeProperties.DAY_KEY);
    final long weekKey = user.getPropertyOrDefault(PlaytimeProperties.WEEK_KEY);
    final long monthKey = user.getPropertyOrDefault(PlaytimeProperties.MONTH_KEY);

    final long dayGoal = this.getEffectiveGoalMs(user, true, false, false);
    final long weekGoal = this.getEffectiveGoalMs(user, false, true, false);
    final long monthGoal = this.getEffectiveGoalMs(user, false, false, true);

    if (dayGoal > 0L) {
      if (day >= dayGoal) {
        if (user.getPropertyOrDefault(PlaytimeProperties.REWARDED_DAY_KEY) != dayKey) {
          user.setProperty(PlaytimeProperties.REWARDED_DAY_KEY, dayKey);
          this.giveGoalReward(player, module.getSettings().getDailyRewards(), "Daily", day);
        }
      } else if (module.getSettings().isRemindersEnabled()) {
        final long remaining = dayGoal - day;
        if (remaining > 0L && remaining <= module.getSettings().getDayReminderWindowMs()
            && user.getPropertyOrDefault(PlaytimeProperties.REMINDED_DAY_KEY) != dayKey) {
          user.setProperty(PlaytimeProperties.REMINDED_DAY_KEY, dayKey);
          module.sendPrefixed(PlaytimeLang.REMINDER_GOAL, player, b -> b
              .with(SLPlaceholders.GENERIC_NAME, () -> "Daily")
              .with(SLPlaceholders.GENERIC_TIME, () -> TimeUtil.formatMillis(remaining)));
        }
      }
    }

    if (weekGoal > 0L) {
      if (week >= weekGoal) {
        if (user.getPropertyOrDefault(PlaytimeProperties.REWARDED_WEEK_KEY) != weekKey) {
          user.setProperty(PlaytimeProperties.REWARDED_WEEK_KEY, weekKey);
          this.giveGoalReward(player, module.getSettings().getWeeklyRewards(), "Weekly", week);
        }
      } else if (module.getSettings().isRemindersEnabled()) {
        final long remaining = weekGoal - week;
        if (remaining > 0L && remaining <= module.getSettings().getWeekReminderWindowMs()
            && user.getPropertyOrDefault(PlaytimeProperties.REMINDED_WEEK_KEY) != weekKey) {
          user.setProperty(PlaytimeProperties.REMINDED_WEEK_KEY, weekKey);
          module.sendPrefixed(PlaytimeLang.REMINDER_GOAL, player, b -> b
              .with(SLPlaceholders.GENERIC_NAME, () -> "Weekly")
              .with(SLPlaceholders.GENERIC_TIME, () -> TimeUtil.formatMillis(remaining)));
        }
      }
    }

    if (monthGoal > 0L) {
      if (month >= monthGoal) {
        if (user.getPropertyOrDefault(PlaytimeProperties.REWARDED_MONTH_KEY) != monthKey) {
          user.setProperty(PlaytimeProperties.REWARDED_MONTH_KEY, monthKey);
          this.giveGoalReward(player, module.getSettings().getMonthlyRewards(), "Monthly", month);
        }
      } else if (module.getSettings().isRemindersEnabled()) {
        final long remaining = monthGoal - month;
        if (remaining > 0L && remaining <= module.getSettings().getMonthReminderWindowMs()
            && user.getPropertyOrDefault(PlaytimeProperties.REMINDED_MONTH_KEY) != monthKey) {
          user.setProperty(PlaytimeProperties.REMINDED_MONTH_KEY, monthKey);
          module.sendPrefixed(PlaytimeLang.REMINDER_GOAL, player, b -> b
              .with(SLPlaceholders.GENERIC_NAME, () -> "Monthly")
              .with(SLPlaceholders.GENERIC_TIME, () -> TimeUtil.formatMillis(remaining)));
        }
      }
    }
  }

  private void giveGoalReward(final Player player, final List<String> commands, final String goalName,
      final long playtime) {
    module.sendPrefixed(PlaytimeLang.REWARD_GOAL, player, b -> b
        .with(SLPlaceholders.GENERIC_NAME, () -> goalName));
    module.dispatchRewardCommands(player, commands);
    module.plugin().getPluginManager().callEvent(new PlaytimeGoalReachedEvent(player, goalName, playtime, commands));
  }
}
