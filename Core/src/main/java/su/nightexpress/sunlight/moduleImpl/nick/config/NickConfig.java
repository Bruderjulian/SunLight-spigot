package su.nightexpress.sunlight.moduleImpl.nick.config;

import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.util.Lists;

import java.util.List;

public class NickConfig {

    public static final ConfigValue<Integer> MIN_LENGTH = ConfigValue.create("Nick.Length.Min",
        3,
        "Sets the minimum nickname length (stripped of colors/formatting, counted in Unicode code points)."
    );

    public static final ConfigValue<Integer> MAX_LENGTH = ConfigValue.create("Nick.Length.Max",
        16,
        "Sets the maximum nickname length (stripped of colors/formatting, counted in Unicode code points)."
    );

    public static final ConfigValue<List<String>> BANNED_WORDS = ConfigValue.create("Nick.Banned-Words",
        Lists.newList("admin", "ass", "shit"),
        "Nicknames containing these words (case-insensitive) will be rejected.",
        "Nicknames matching an online player's name or current nickname will be rejected as well.",
        "With 'Nick.Banned-Words-Exact' enabled, only whole-word matches are rejected."
    );

    public static final ConfigValue<Boolean> BANNED_WORDS_EXACT = ConfigValue.create("Nick.Banned-Words-Exact",
        false,
        "When enabled, banned words are matched as whole words only.",
        "This prevents false positives like 'ass' matching the word 'Classic'."
    );

    public static final ConfigValue<String> REGEX_PATTERN = ConfigValue.create("Nick.Regex-Pattern",
        "[a-zA-Zа-яА-Я0-9_\\s]*",
        "Sets the regex pattern nicknames (stripped of colors/formatting) must match.",
        "If the pattern is invalid, the regex check is disabled and a warning is printed on module load."
    );

    public static final ConfigValue<Boolean> APPLY_TO_TABLIST = ConfigValue.create("Nick.Tablist.Enabled",
        true,
        "Sets whether nicknames should also be shown in the player (tab) list."
    );

    public static final ConfigValue<Integer> CHANGE_COOLDOWN = ConfigValue.create("Nick.Change.Cooldown",
        0,
        "Sets the cooldown (in seconds) a player must wait between nickname changes.",
        "Set to -1 to make the nickname change one-time only. Set to 0 to disable.",
        "Affects the '/changenick' command only; the cooldown persists across sessions."
    );

    public static final ConfigValue<Double> CHANGE_COST = ConfigValue.create("Nick.Change.Cost",
        0D,
        "Sets how much it costs (in Vault currency) to change the nickname.",
        "Set to 0 to disable.",
        "The money is only withdrawn if the nickname change is actually applied.",
        "Affects the '/changenick' command only."
    );
}
