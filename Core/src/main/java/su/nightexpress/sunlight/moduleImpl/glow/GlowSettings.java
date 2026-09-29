package su.nightexpress.sunlight.moduleImpl.glow;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigType;
import su.nightexpress.nightcore.configuration.ConfigTypes;

import java.util.List;
import java.util.Map;

public class GlowSettings extends AbstractConfig {

    private static final ConfigType<GlowEffect> GLOW_EFFECT_TYPE = ConfigType.of(GlowEffect::read,
            (config, path, value) -> value.write(config, path));

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
            "Available glow effects players can choose from. Global presets.",
            "",
            "[ SETTINGS DESCRIPTION ]",
            "├── Name: Display name shown in '/glow list'.",
            "├── Type: STATIC or PHASED.",
            "│     -> STATIC: single color, first entry of 'Phases' is used.",
            "│     -> PHASED: loops through 'Phases' in order, each with its own duration.",
            "├── Phases: List of 'COLOR@DURATION' tokens, e.g. 'RED@40', 'GOLD@20'.",
            "│     -> Colors: Bukkit color names, e.g. RED, GOLD, YELLOW, GREEN, AQUA, BLUE, etc.",
            "│     -> Duration: Per-phase frame duration in ticks (20 = 1 second, 1-1200).",
            "",
            "Glow colors are sent as client-side (packet) teams and never touch the server scoreboard.");

    private List<String> effectKeys;

    public void load(final FileConfig config) {
        super.load(config);
        effectKeys = this.effects.get().keySet().stream().sorted().toList();
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

    public List<String> getEffectsKeys() {
        return effectKeys;
    }
}
