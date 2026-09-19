package su.nightexpress.sunlight.moduleImpl.nick.config;

import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.util.Lists;

import java.util.List;

public class NickConfig {

    public static final ConfigValue<Integer> MIN_LENGTH = ConfigValue.create("Nick.Length.Min",
        3,
        "Sets the minimum nickname length (stripped of colors/formatting)."
    );

    public static final ConfigValue<Integer> MAX_LENGTH = ConfigValue.create("Nick.Length.Max",
        16,
        "Sets the maximum nickname length (stripped of colors/formatting)."
    );

    public static final ConfigValue<List<String>> BANNED_WORDS = ConfigValue.create("Nick.Banned-Words",
        Lists.newList("admin", "ass", "shit"),
        "Nicknames containing these words (case-insensitive) will be rejected.",
        "Nicknames matching an online player's name will be rejected as well."
    );

    public static final ConfigValue<String> REGEX_PATTERN = ConfigValue.create("Nick.Regex-Pattern",
        "[a-zA-Zа-яА-Я0-9_\\s]*",
        "Sets the regex pattern nicknames (stripped of colors/formatting) must match."
    );

    public static final ConfigValue<Boolean> APPLY_TO_TABLIST = ConfigValue.create("Nick.Apply_To_Tablist",
        true,
        "Sets whether nicknames should also be shown in the player (tab) list."
    );
}
