package su.nightexpress.sunlight.moduleImpl.glow;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigType;
import su.nightexpress.nightcore.configuration.ConfigTypes;

import java.util.Map;

public class GlowSettings extends AbstractConfig {

    private static final ConfigType<GlowEffect> GLOW_EFFECT_TYPE = ConfigType.of(GlowEffect::read, (config, path, value) -> value.write(config, path));

    private final ConfigProperty<Long> updateInterval = this.addProperty(ConfigTypes.LONG,
            "Animation.Update_Interval",
            5L,
            "How often (in ticks) the animation task runs. One packet batch per animated glow at most.",
            "[1 second = 20 ticks]",
            "[Lower = smoother animation, but more packets.]");

    private final ConfigProperty<Boolean> restoreOnJoin = this.addProperty(ConfigTypes.BOOLEAN,
            "Restore_On_Join",
            true,
            "Sets whether a player's glow should be re-applied when they join.");

    private final ConfigProperty<Map<String, GlowEffect>> effects = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(GLOW_EFFECT_TYPE),
            "Effects",
            GlowDefaults.getDefaultEffects(),
            "Available glow effects players can choose from.",
            "",
            "[ SETTINGS DESCRIPTION ]",
            "├── Name: Display name shown in '/glow list'.",
            "├── Type: STATIC, CYCLE, GRADIENT, RAINBOW or FLASH.",
            "│     -> STATIC: single color, first entry of 'Colors' is used.",
            "│     -> CYCLE: loops through 'Colors' in order.",
            "│     -> GRADIENT: goes through 'Colors' forth and back (smooth).",
            "│     -> RAINBOW: cycles through the rainbow palette.",
            "│     -> FLASH: alternates between the entries of 'Colors'.",
            "├── Colors: Bukkit color names, e.g. RED, GOLD, YELLOW, GREEN, AQUA, BLUE, etc.",
            "└── Interval: Frame duration in ticks (20 = 1 second). Should be >= 'Update_Interval'.",
            "",
            "Glow colors are sent as client-side (packet) teams and never touch the server scoreboard.");

    public void load(FileConfig config) {
        super.load(config);
    }

    public long getUpdateInterval() {
        return Math.max(1L, this.updateInterval.get());
    }

    public boolean isRestoreOnJoin() {
        return this.restoreOnJoin.get();
    }

    public Map<String, GlowEffect> getEffects() {
        return this.effects.get();
    }
}
