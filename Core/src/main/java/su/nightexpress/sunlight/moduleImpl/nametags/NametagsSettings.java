package su.nightexpress.sunlight.moduleImpl.nametags;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigType;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;
import su.nightexpress.sunlight.moduleImpl.nametags.model.RankDefinition;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;

import java.util.Map;

public class NametagsSettings extends AbstractConfig {

    private static final ConfigType<RankDefinition> RANK_TYPE = ConfigType.of(
            RankDefinition::read, (config, path, value) -> value.write(config, path));

    private static final ConfigType<TagDefinition> TAG_TYPE = ConfigType.of(
            TagDefinition::read, (config, path, value) -> value.write(config, path));

    private static final ConfigType<Profile> PROFILE_TYPE = ConfigType.of(
            Profile::read, (config, path, value) -> value.write(config, path));

    private final ConfigProperty<Long> updateInterval = this.addProperty(ConfigTypes.LONG,
            "Nametags.Update_Interval",
            100L,
            "How often (in ticks) nameplates are recomputed for online players.",
            "[1 second = 20 ticks]");

    private final ConfigProperty<Long> expiryCheckInterval = this.addProperty(ConfigTypes.LONG,
            "Nametags.Expiry_Check_Interval",
            1200L,
            "How often (in ticks) expired subscriptions are swept.",
            "[1 minute = 1200 ticks]");

    private final ConfigProperty<Boolean> tagColorOverridesRank = this.addProperty(ConfigTypes.BOOLEAN,
            "Nametags.Tag-Color-Overrides-Rank",
            true,
            "Whether a selected tag's name colour takes priority over the rank colour.",
            "Glow always wins, because it tracks the player's active glow effect.");

    private final ConfigProperty<Boolean> useUltimateTeams = this.addProperty(ConfigTypes.BOOLEAN,
            "Nametags.Ultimate-Teams.Enabled",
            true,
            "Whether to add the player's UltimateTeams team to their nameplate.",
            "Has no effect when UltimateTeams is not installed.");

    private final ConfigProperty<Boolean> useLuckPerms = this.addProperty(ConfigTypes.BOOLEAN,
            "Nametags.Luck-Perms.Enabled",
            true,
            "Whether to resolve ranks from LuckPerms groups.",
            "Falls back to the server's permission groups when LuckPerms is not installed.");

    private final ConfigProperty<Boolean> resolveGlowColor = this.addProperty(ConfigTypes.BOOLEAN,
            "Nametags.Glow-Color.Enabled",
            true,
            "Whether the player's glow colour is applied as the final name colour.",
            "Requires the glow module. Tags with 'Glow' enabled keep their own colour otherwise.");

    private final ConfigProperty<Map<String, RankDefinition>> ranks = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(RANK_TYPE),
            "Nametags.Ranks",
            NametagsDefaults.getDefaultRanks(),
            "Rank based nametag formats. The available rank with the highest 'Priority' wins.",
            "A rank matches when the player inherits one of its groups, or has 'nametags.rank.<group>'.",
            "",
            "[ SETTINGS DESCRIPTION ]",
            "├── Display/Description/Icon: How the rank is shown in the admin GUI.",
            "├── Priority: Highest matching rank wins.",
            "├── Ranks: Rank groups to match, or '*' for everyone.",
            "├── Default: Used when nothing else matches.",
            "├── Prefix/Suffix: MiniMessage text placed around the player name.",
            "├── Color: Name colour, e.g. white, red, gold, aqua or #ffaa00.",
            "└── Placeholders: Common placeholders and PlaceholderAPI.");

    private final ConfigProperty<Map<String, TagDefinition>> tags = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(TAG_TYPE),
            "Nametags.Tags",
            NametagsDefaults.getDefaultTags(),
            "Selectable tags. A tag adds its own prefix/suffix on top of the rank format.",
            "",
            "[ SETTINGS DESCRIPTION ]",
            "├── Access-Mode: FREE, PERMISSION, PURCHASE or SUBSCRIPTION.",
            "├── Price: Cost for PURCHASE and SUBSCRIPTION tags.",
            "├── Price-Mode: INTERNAL (SunLight economy) or EXTERNAL (owned by another plugin).",
            "├── Subscription-Period: WEEKLY, MONTHLY, QUARTERLY or YEARLY.",
            "├── Permission: Extra permission on top of 'nametags.tag.<id>'.",
            "├── Glow: Whether the glow module may colour this tag.",
            "├── Icon: Material used in the GUI.",
            "├── Sort-Priority: Higher sorts first in the GUI.",
            "└── Prefix/Suffix/Color: Text and colour contributed to the nameplate.");

    private final ConfigProperty<Map<String, Profile>> profiles = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(PROFILE_TYPE),
            "Nametags.Profiles",
            NametagsDefaults.getDefaultProfiles(),
            "Named bundles of defaults that players can select.",
            "",
            "[ SETTINGS DESCRIPTION ]",
            "├── Worlds: Worlds this profile applies to. Empty means every world.",
            "├── Priority: Higher wins when several profiles match.",
            "├── Rank: Forces this rank, overriding the rank resolution.",
            "└── Tag: Forces this tag, overriding the player's own selection.");

    public void load(FileConfig config) {
        super.load(config);
    }

    /**
     * Writes the current ranks, tags and profiles back to the config file.
     * Only these three maps are written, so the generated comments for the other settings
     * survive an admin edit from the GUI.
     */
    public void save(FileConfig config,
            Map<String, RankDefinition> ranks,
            Map<String, TagDefinition> tags,
            Map<String, Profile> profiles
    ) {
        this.ranks.writeValue(config, new java.util.LinkedHashMap<>(ranks));
        this.tags.writeValue(config, new java.util.LinkedHashMap<>(tags));
        this.profiles.writeValue(config, new java.util.LinkedHashMap<>(profiles));
    }

    public long getUpdateInterval() {
        return Math.max(20L, this.updateInterval.get());
    }

    public long getExpiryCheckInterval() {
        return Math.max(200L, this.expiryCheckInterval.get());
    }

    public boolean isTagColorOverridesRank() {
        return this.tagColorOverridesRank.get();
    }

    public boolean isUseUltimateTeams() {
        return this.useUltimateTeams.get();
    }

    public boolean isUseLuckPerms() {
        return this.useLuckPerms.get();
    }

    public boolean isResolveGlowColor() {
        return this.resolveGlowColor.get();
    }

    public Map<String, RankDefinition> getRanks() {
        return this.ranks.get();
    }

    public Map<String, TagDefinition> getTags() {
        return this.tags.get();
    }

    public Map<String, Profile> getProfiles() {
        return this.profiles.get();
    }
}
