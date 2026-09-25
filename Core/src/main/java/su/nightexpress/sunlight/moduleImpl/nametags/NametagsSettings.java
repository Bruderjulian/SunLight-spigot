package su.nightexpress.sunlight.moduleImpl.nametags;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigType;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.sunlight.moduleImpl.nametags.model.NametagRule;
import su.nightexpress.sunlight.moduleImpl.nametags.model.NametagTag;

import java.util.Map;

public class NametagsSettings extends AbstractConfig {

    private static final ConfigType<NametagRule> RULE_TYPE = ConfigType.of(
            NametagRule::read, (config, path, value) -> value.write(config, path));

    private static final ConfigType<NametagTag> TAG_TYPE = ConfigType.of(
            NametagTag::read, (config, path, value) -> value.write(config, path));

    private final ConfigProperty<Long> updateInterval = this.addProperty(ConfigTypes.LONG,
            "Nametags.Update_Interval",
            100L,
            "Sets how often (in game ticks) all nametag teams are refreshed.",
            "[Packet-Based, Async] [1 second = 20 ticks]");

    private final ConfigProperty<Boolean> tablistEnabled = this.addProperty(ConfigTypes.BOOLEAN,
            "Nametags.Tablist.Enabled",
            true,
            "Sets whether the combined prefix/suffix is also applied to the tab list.",
            "Players with an active Nick will keep their custom nick in the tab list instead.");

    private final ConfigProperty<Boolean> tagColorOverridesRank = this.addProperty(ConfigTypes.BOOLEAN,
            "Nametags.Tag-Color-Overrides-Rank",
            true,
            "Sets whether a selected tag's name color takes priority over the rank color.");

    private final ConfigProperty<Map<String, NametagRule>> rules = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(RULE_TYPE),
            "Nametags.Ranks",
            NametagsDefaults.getDefaultRules(),
            "Rank-based nametag formats. The format with the highest 'Priority' the player",
            "qualifies for is used. A rule is available when the player inherits the rank",
            "group (permissions plugin) or has the 'nametags.rank.<id>' permission.",
            "",
            "[ SETTINGS DESCRIPTION ]",
            "├── Priority: Highest available format wins.",
            "├── Ranks: List of rank groups, or '*' to allow everyone.",
            "├── Prefix/Suffix: MiniMessage text shown around the player name.",
            "├── Color: Name color, e.g. white, red, gold, aqua.",
            "└── Placeholders: %player_name%, %player_display_name%, %player_prefix%, %player_suffix% and PlaceholderAPI.");

    private final ConfigProperty<Map<String, NametagTag>> tags = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(TAG_TYPE),
            "Nametags.Tags",
            NametagsDefaults.getDefaultTags(),
            "Per-player selectable tags. Players need the 'nametags.tag.<id>' permission.",
            "The selected tag is combined with the rank format (rank prefix/suffix + tag prefix/suffix).",
            "",
            "[ SETTINGS DESCRIPTION ]",
            "├── Display: Name shown in the '/tags' GUI.",
            "├── Prefix/Suffix: MiniMessage text added around the rank format parts.",
            "├── Color: Optional name color override (see 'Tag-Color-Overrides-Rank').",
            "├── Cost: Optional economy cost to select the tag.",
            "├── Description: Lore shown in the '/tags' GUI.",
            "└── Permission: Optional extra permission on top of 'nametags.tag.<id>'.");

    public void load(FileConfig config) {
        super.load(config);
    }

    public long getUpdateInterval() {
        return Math.max(20L, this.updateInterval.get());
    }

    public boolean isTablistEnabled() {
        return this.tablistEnabled.get();
    }

    public boolean isTagColorOverridesRank() {
        return this.tagColorOverridesRank.get();
    }

    public Map<String, NametagRule> getRules() {
        return this.rules.get();
    }

    public Map<String, NametagTag> getTags() {
        return this.tags.get();
    }
}