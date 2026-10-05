package su.nightexpress.sunlight.moduleImpl.vanish.config;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import su.nightexpress.nightcore.config.ConfigValue;

import static su.nightexpress.nightcore.util.text.tag.Tags.*;

public class VanishConfig {

    public static final ConfigValue<Boolean> BAR_INDICATOR_ENABLED = ConfigValue.create("BarIndicator.Enabled",
        true,
        "Sets whether vanish boss bar indicator should be used."
    );

    public static final ConfigValue<String> BAR_INDICATOR_VANISHED_TITLE = ConfigValue.create("BarIndicator.Vanished.Title",
        WHITE.wrap("You're vanished!"),
        "Sets vanish indicator bar title."
    );

    public static final ConfigValue<BarColor> BAR_INDICATOR_VANISHED_COLOR = ConfigValue.create("BarIndicator.Vanished.Color",
        BarColor.class, BarColor.GREEN,
        "Sets vanish indicator bar color."
    );

    public static final ConfigValue<BarStyle> BAR_INDICATOR_VANISHED_STYLE = ConfigValue.create("BarIndicator.Vanished.Style",
        BarStyle.class, BarStyle.SEGMENTED_10,
        "Sets vanish indicator bar style."
    );

    public static final ConfigValue<String> METADATA_KEY = ConfigValue.create("Vanish.Metadata-Key",
        "vanished",
        "Metadata flag set on vanished players so other plugins can detect them.",
        "Empty value disables the metadata flag."
    );

    public static final ConfigValue<Boolean> SUPPRESS_JOIN_QUIT = ConfigValue.create("Vanish.Suppress-Join-Quit-Messages",
        true,
        "Sets whether real join/quit messages of vanished players should be hidden."
    );

    public static final ConfigValue<Boolean> FAKE_MESSAGES_ENABLED = ConfigValue.create("Vanish.Fake-Messages.Enabled",
        true,
        "Sets whether fake join/leave messages should be broadcast on vanish toggle.",
        "Fake messages are only sent to players without the see-bypass permission."
    );

    public static final ConfigValue<Boolean> FAKE_MESSAGE_ON_VANISH = ConfigValue.create("Vanish.Fake-Messages.On-Vanish",
        true,
        "Sets whether a fake leave message is broadcast when a player vanishes."
    );

    public static final ConfigValue<Boolean> FAKE_MESSAGE_ON_UNVANISH = ConfigValue.create("Vanish.Fake-Messages.On-Unvanish",
        true,
        "Sets whether a fake join message is broadcast when a player unvanishes."
    );
}
