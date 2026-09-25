package su.nightexpress.sunlight.moduleImpl.reports;

import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportCategory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ReportsConfig {

    public static final ConfigValue<String> DATA_TABLE_PREFIX = ConfigValue.create("Reports.Data.Table-Prefix",
            "sunlight_reports",
            "Prefix for the reports and report notes tables."
    );

    public static final ConfigValue<Integer> COOLDOWN_SUBMIT = ConfigValue.create("Reports.Cooldown.Submit-Seconds",
            60,
            "Seconds a player must wait between two reports.",
            "Stored in the player's command cooldown data, so it survives relog.",
            "Use 0 to disable. Staff holding 'sunlight.reports.bypass.cooldown' skip it.",
            "This is separate from the per-command 'Cooldown' value in commands/reports-submit.yml,",
            "which must stay 0 so that only one cooldown is ever in play."
    );

    public static final ConfigValue<Integer> LIMIT_MAX_OPEN = ConfigValue.create("Reports.Limits.Max-Open-Per-Player",
            3,
            "How many not-yet-concluded reports (OPEN or CLAIMED) a player may have at once.",
            "Use -1 for unlimited.",
            "This is the guard that actually bounds abuse volume; the cooldown only rate-limits it."
    );

    public static final ConfigValue<Boolean> LIMIT_DETAILS_REQUIRED = ConfigValue.create("Reports.Limits.Details-Required",
            true,
            "When true, a report must include a written explanation.",
            "A category's default text is not a substitute for this."
    );

    public static final ConfigValue<Integer> LIMIT_DETAILS_MIN = ConfigValue.create("Reports.Limits.Details-Min-Length",
            10,
            "Minimum length of the written explanation, after trimming."
    );

    public static final ConfigValue<Integer> LIMIT_DETAILS_MAX = ConfigValue.create("Reports.Limits.Details-Max-Length",
            300,
            "Maximum length of the written explanation."
    );

    public static final ConfigValue<Boolean> NOTIFY_ENABLED = ConfigValue.create("Reports.Notify.Enabled",
            true,
            "When enabled, staff holding the notify permission are told about new reports."
    );

    public static final ConfigValue<String> NOTIFY_PERMISSION = ConfigValue.create("Reports.Notify.Permission",
            "sunlight.reports.notify",
            "Permission required to receive report notifications."
    );

    public static final ConfigValue<String> NOTIFY_SOUND = ConfigValue.create("Reports.Notify.Sound",
            "BLOCK_NOTE_BLOCK_PLING",
            "Sound played to notified staff. Leave empty to disable."
    );

    public static final ConfigValue<Boolean> NOTIFY_TARGET_ENABLED = ConfigValue.create("Reports.Notify.Target.Enabled",
            false,
            "When enabled, the reported player is told that a report was filed against them.",
            "Off by default, because it leaks that somebody spoke up.",
            "The message never names the reporter and never shows the written explanation."
    );

    public static final ConfigValue<String> NOTIFY_TARGET_MESSAGE = ConfigValue.create("Reports.Notify.Target.Message",
            "<gray>You have been reported. Staff will review it.",
            "Message sent to the reported player when target notification is enabled."
    );

    public static final ConfigValue<Set<String>> BLACKLIST_WORLDS = ConfigValue.create("Reports.Blacklist.Worlds",
            Set.of(),
            "Reporters in these worlds cannot use /report at all."
    );

    public static final ConfigValue<Set<String>> BLACKLIST_EXEMPT_PERMISSIONS = ConfigValue
            .create("Reports.Blacklist.Exempt-Permissions",
                    Set.of("sunlight.reports.exempt"),
                    "Reporters holding any of these permissions are exempt from the whole module."
            );

    public static final ConfigValue<Integer> NOTES_MAX_LENGTH = ConfigValue.create("Reports.Notes.Max-Length",
            500,
            "Maximum length of a single staff note."
    );

    public static final ConfigValue<Boolean> NOTES_VISIBLE_TO_REPORTER = ConfigValue.create(
            "Reports.Notes.Visible-To-Reporter",
            false,
            "When true, the reporter can read the staff note trail on their own report.",
            "Notes often contain internal discussion, so this is off by default."
    );

    public static final ConfigValue<Boolean> REWARDS_ENABLED = ConfigValue.create("Reports.Rewards.Enabled",
            true,
            "When enabled, a report concluded as RESOLVED pays the reporter a reward."
    );

    public static final ConfigValue<List<String>> REWARDS_COMMANDS = ConfigValue.create(
            "Reports.Rewards.Commands",
            Lists.newList("eco give %reporter_name% 500"),
            "Console commands run when a report is concluded as RESOLVED.",
            "Placeholders: %reporter%, %reporter_name%, %target%, %target_name%, %category%,",
            "%details%, %report_id%, %date%.",
            "These run as the console even when the reporter is offline, so they must be",
            "offline-safe: 'eco give' is, 'give' is not.",
            "A reward is paid at most once per report, even when a report is both resolved by",
            "staff and closed by a punishment."
    );

    public static final ConfigValue<Boolean> REWARDS_NOTIFY = ConfigValue.create("Reports.Rewards.Notify-Rewarded",
            true,
            "When enabled, the reporter is told when they get rewarded."
    );

    public static final ConfigValue<Integer> REWARDS_MIN_PLAYTIME_HOURS = ConfigValue.create(
            "Reports.Rewards.Minimum-Reporter-Playtime-Hours",
            0,
            "Minimum total playtime a reporter needs before a reward pays out.",
            "Guards against alt-boosting. Use 0 for no requirement.",
            "Requires the Playtime module; the requirement is skipped when it is not enabled."
    );

    public static final ConfigValue<Set<String>> REWARDS_REQUIRE_PUNISHMENT_TYPE = ConfigValue.create(
            "Reports.Rewards.Require-Punishment-Type",
            Set.of("BAN", "MUTE"),
            "Which punishment types close a report as justified.",
            "A WARN closes the report regardless, it just does not pay a reward."
    );

    public static Map<String, ReportCategory> readCategories(FileConfig config) {
        Map<String, ReportCategory> defaults = defaultCategories();

        defaults.forEach((id, category) -> {
            String path = "Reports.Categories." + id;
            if (!config.contains(path)) {
                category.write(config, path);
            }
        });

        Map<String, ReportCategory> map = new TreeMap<>();
        for (String id : config.getSection("Reports.Categories")) {
            map.put(id.toLowerCase(Locale.ROOT), ReportCategory.read(config, "Reports.Categories." + id));
        }
        if (map.isEmpty()) {
            return new LinkedHashMap<>(defaults);
        }
        return new LinkedHashMap<>(map);
    }

    private static Map<String, ReportCategory> defaultCategories() {
        Map<String, ReportCategory> map = new LinkedHashMap<>();
        map.put("griefing", new ReportCategory("<green>Griefing", "DIRT", "",
                "They broke blocks that were not theirs."));
        map.put("cheating", new ReportCategory("<red>Cheating", "DIAMOND_SWORD", "", ""));
        map.put("harassment", new ReportCategory("<yellow>Harassment", "PAPER",
                "sunlight.reports.category.harassment", ""));
        return map;
    }
}
