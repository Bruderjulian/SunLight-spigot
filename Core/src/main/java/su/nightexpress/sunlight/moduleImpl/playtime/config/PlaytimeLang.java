package su.nightexpress.sunlight.moduleImpl.playtime.config;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class PlaytimeLang implements LangContainer {

    public static final TextLocale COMMAND_PLAYTIME_DESC = LangEntry.builder("Command.Playtime.Root.Desc")
        .text("Your playtime statistics.");

    public static final TextLocale COMMAND_STATS_DESC = LangEntry.builder("Command.Playtime.Stats.Desc")
        .text("Your playtime for a specific period.");

    public static final TextLocale COMMAND_TOP_DESC = LangEntry.builder("Command.Playtime.Top.Desc")
        .text("Playtime leaderboard.");

    public static final TextLocale COMMAND_GOAL_DESC = LangEntry.builder("Command.Playtime.Goal.Desc")
        .text("View or change your playtime goals.");

    public static final MessageLocale STATS_HEADER = LangEntry.builder("Playtime.Stats.Header")
        .chatMessage(
            DARK_GRAY.wrap("[" + GOLD.wrap("Playtime") + "] ") + WHITE.wrap(PLAYER_NAME));

    public static final MessageLocale STATS_TOTAL = LangEntry.builder("Playtime.Stats.Total")
        .chatMessage(
            GRAY.wrap("Total: ") + WHITE.wrap(GENERIC_TIME));

    public static final MessageLocale STATS_PERIOD = LangEntry.builder("Playtime.Stats.Period")
        .chatMessage(
            GRAY.wrap("▸ ") + GENERIC_NAME + GRAY.wrap(": ") + WHITE.wrap(GENERIC_TIME));

    public static final MessageLocale STATS_STREAK_DAILY = LangEntry.builder("Playtime.Stats.StreakDaily")
        .chatMessage(
            GRAY.wrap("Streak (daily): ") + WHITE.wrap(GENERIC_VALUE) + DARK_GRAY.wrap(" days"));

    public static final MessageLocale STATS_STREAK_WEEKLY = LangEntry.builder("Playtime.Stats.StreakWeekly")
        .chatMessage(
            GRAY.wrap("Streak (weekly): ") + WHITE.wrap(GENERIC_VALUE) + DARK_GRAY.wrap(" weeks"));

    public static final MessageLocale GOAL_HEADER = LangEntry.builder("Playtime.Goal.Header")
        .chatMessage(
            DARK_GRAY.wrap("[" + GOLD.wrap("Goals") + "]"));

    public static final MessageLocale GOAL_LINE = LangEntry.builder("Playtime.Goal.Line")
        .chatMessage(
            GRAY.wrap("▸ ") + GENERIC_NAME + GRAY.wrap(": ") + WHITE.wrap(GENERIC_VALUE) + GRAY.wrap("% (")
                + WHITE.wrap(GENERIC_CURRENT) + GRAY.wrap(" / ") + WHITE.wrap(GENERIC_MAX) + GRAY.wrap(")"));

    public static final MessageLocale GOAL_LINE_INACTIVE = LangEntry.builder("Playtime.Goal.LineInactive")
        .chatMessage(
            GRAY.wrap("▸ ") + GENERIC_NAME + GRAY.wrap(": ") + SOFT_RED.wrap("No goal set"));

    public static final MessageLocale GOAL_SET = LangEntry.builder("Playtime.Goal.Set")
        .chatMessage(
            GREEN.wrap(GENERIC_NAME) + GREEN.wrap(" goal is now ") + WHITE.wrap(GENERIC_TIME) + GREEN.wrap("."));

    public static final MessageLocale GOAL_RESET = LangEntry.builder("Playtime.Goal.Reset")
        .chatMessage(
            GREEN.wrap(GENERIC_NAME) + GREEN.wrap(" goal reset to the server default."));

    public static final MessageLocale GOAL_ERROR_RANGE = LangEntry.builder("Playtime.Goal.Error.Range")
        .chatMessage(
            SOFT_RED.wrap("Allowed minutes range: ") + WHITE.wrap(GENERIC_MIN) + SOFT_RED.wrap(" – ")
                + WHITE.wrap(GENERIC_MAX) + SOFT_RED.wrap(". Use 0 to reset."));

    public static final MessageLocale REWARD_GOAL = LangEntry.builder("Playtime.Goal.Reward")
        .chatMessage(
            GREEN.wrap("You reached the ") + GENERIC_NAME + GREEN.wrap(" goal and received a reward!"));

    public static final MessageLocale REMINDER_GOAL = LangEntry.builder("Playtime.Goal.Reminder")
        .chatMessage(
            GOLD.wrap("You need only ") + WHITE.wrap(GENERIC_TIME) + GOLD.wrap(" more to reach the ")
                + GENERIC_NAME + GOLD.wrap(" goal!"));

    public static final MessageLocale TOP_TITLE = LangEntry.builder("Playtime.Top.Title")
        .chatMessage(
            DARK_GRAY.wrap("[" + GOLD.wrap("Playtime Top") + "] ") + GENERIC_NAME);

    public static final MessageLocale TOP_ENTRY = LangEntry.builder("Playtime.Top.Entry")
        .chatMessage(
            GRAY.wrap("#") + WHITE.wrap(GENERIC_PAGE) + GRAY.wrap(" ") + WHITE.wrap(PLAYER_NAME) + GRAY.wrap(" – ")
                + WHITE.wrap(GENERIC_TOTAL));

    public static final MessageLocale ERROR_GOAL_NEGATIVE = LangEntry.builder("Playtime.Error.GoalNegative")
        .chatMessage(
            SOFT_RED.wrap("Minutes amount must not be negative."));
}