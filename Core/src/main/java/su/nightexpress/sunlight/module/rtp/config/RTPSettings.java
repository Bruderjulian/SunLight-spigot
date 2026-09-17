package su.nightexpress.sunlight.module.rtp.config;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigType;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.sunlight.module.rtp.RTPDefaults;
import su.nightexpress.sunlight.module.rtp.model.LookupRange;

import java.util.Map;
import java.util.Set;

public class RTPSettings extends AbstractConfig {

    private static final ConfigType<LookupRange> RANGE_INFO_CONFIG_TYPE = ConfigType.of(
        LookupRange::read,
        FileConfig::set
    );

    private final ConfigProperty<Boolean> fallbackEnabled = this.addProperty(ConfigTypes.BOOLEAN, "World-Fallback.Enabled",
        true,
        "Controls whether to fallback to specified world if RTP is not configured for the player's world.");

    private final ConfigProperty<String> fallbackWorld = this.addProperty(ConfigTypes.STRING, "World-Fallback.World",
        "world",
        "World to fallback to if enabled.");

    private final ConfigProperty<Integer> lookupMaxAttempts = this.addProperty(ConfigTypes.INT, "Lookup.Max-Attempts",
        5,
        "Max amount of attempts to lookup for a valid location.");

    private final ConfigProperty<Boolean> lookupGeneratedChunksOnly = this.addProperty(ConfigTypes.BOOLEAN, "Lookup.Generated_Chunks_Only",
        true,
        "Sets whether to lookup generated chunks only.",
        "[*] Your world must be pre-generated for this feature to work."
    );

    private final ConfigProperty<Boolean> lookupLoadedChunksOnly = this.addProperty(ConfigTypes.BOOLEAN, "Lookup.Loaded_Chunks_Only",
        false,
        "Sets whether to lookup loaded chunks only.");

    private final ConfigProperty<Map<String, LookupRange>> lookupRanges = this.addProperty(ConfigTypes.forMapWithLowerKeys(RANGE_INFO_CONFIG_TYPE),
        "Lookup.Ranges",
        RTPDefaults.getDefaultRangeMap(),
        "Per-world RTP range configuration."
    );

    private final ConfigProperty<Boolean> protectionEnabled = this.addProperty(ConfigTypes.BOOLEAN, "Lookup.Protection.Enabled",
        true,
        "If enabled, RTP will not teleport players into protected or claimed land.",
        "Supported protection plugins (detected automatically): WorldGuard regions, GriefPrevention claims."
    );

    private final ConfigProperty<Set<String>> protectionIgnoredHooks = this.addProperty(ConfigTypes.STRING_SET_LOWER_CASE, "Lookup.Protection.Ignored_Hooks",
        Set.of(),
        "Protection hooks to ignore. Available: worldguard, griefprevention."
    );

    private final ConfigProperty<Boolean> protectionIgnoreGlobalRegion = this.addProperty(ConfigTypes.BOOLEAN, "Lookup.Protection.WorldGuard.Ignore_Global_Region",
        true,
        "If enabled, the global '__global__' WorldGuard region will not block RTP teleports."
    );

    private final ConfigProperty<Double> teleportCost = this.addProperty(ConfigTypes.DOUBLE, "Teleport.Cost",
        0D,
        "Sets how much it costs (in Vault currency) to teleport to a random location.",
        "Set to '0' to disable."
    );

    public boolean isFallbackEnabled() {
        return this.fallbackEnabled.get();
    }

    @NotNull
    public String getFallbackWorld() {
        return this.fallbackWorld.get();
    }

    public int getLookupMaxAttempts() {
        return this.lookupMaxAttempts.get();
    }

    public boolean isLookupGeneratedChunksOnly() {
        return this.lookupGeneratedChunksOnly.get();
    }

    public boolean isLookupLoadedChunksOnly() {
        return this.lookupLoadedChunksOnly.get();
    }

    @NotNull
    public Map<String, LookupRange> getLookupRangesMap() {
        return this.lookupRanges.get();
    }

    public boolean isProtectionEnabled() {
        return this.protectionEnabled.get();
    }

    @NotNull
    public Set<String> getProtectionIgnoredHooks() {
        return this.protectionIgnoredHooks.get();
    }

    public boolean isProtectionIgnoreGlobalRegion() {
        return this.protectionIgnoreGlobalRegion.get();
    }

    public double getTeleportCost() {
        return this.teleportCost.get();
    }
}
