package su.nightexpress.sunlight.moduleImpl.rtp.config;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigType;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.util.Enums;
import su.nightexpress.sunlight.moduleImpl.rtp.engine.LookupRange;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Particle;
import org.bukkit.block.BlockFace;

public class RTPSettings extends AbstractConfig {

    private static final ConfigType<LookupRange> RANGE_INFO_CONFIG_TYPE = ConfigType.of(
            LookupRange::read,
            FileConfig::set);

    private final ConfigProperty<Boolean> fallbackEnabled = this.addProperty(ConfigTypes.BOOLEAN,
            "World-Fallback.Enabled",
            true,
            "Controls whether to fallback to specified world if RTP is not configured for the player's world.");

    private final ConfigProperty<String> fallbackWorld = this.addProperty(ConfigTypes.STRING, "World-Fallback.World",
            "world",
            "World to fallback to if enabled.");

    private final ConfigProperty<LookupMode> lookupMode = this.addProperty(ConfigTypes.forEnum(LookupMode.class),
            "Lookup.Mode",
            LookupMode.AUTO,
            "Sets how the location lookup loads chunks.",
            "AUTO - Uses async chunk loading when supported, otherwise sync.",
            "SYNC - Loads chunks on the main thread. Most compatible, but may cause small lag.",
            "ASYNC - Always loads chunks asynchronously (Paper only).");

    private final ConfigProperty<Integer> lookupMaxAttempts = this.addProperty(ConfigTypes.INT, "Lookup.Max-Attempts",
            5,
            "Max amount of attempts to lookup for a valid location.");

    private final ConfigProperty<Boolean> lookupGeneratedChunksOnly = this.addProperty(ConfigTypes.BOOLEAN,
            "Lookup.Generated_Chunks_Only",
            true,
            "Sets whether to lookup generated chunks only.",
            "[*] Your world must be pre-generated for this feature to work.");

    private final ConfigProperty<Boolean> lookupLoadedChunksOnly = this.addProperty(ConfigTypes.BOOLEAN,
            "Lookup.Loaded_Chunks_Only",
            false,
            "Sets whether to lookup loaded chunks only.");

    private final ConfigProperty<Boolean> cacheEnabled = this.addProperty(ConfigTypes.BOOLEAN,
            "Lookup.Cache.Enabled",
            true,
            "Enables caching of pre-validated random locations.",
            "When enabled, valid locations are looked up in advance and stored in memory,",
            "so teleports can instantly use a cached location instead of searching on the spot.",
            "The cache is refilled in the background as locations are consumed.");

    private final ConfigProperty<Integer> cacheSize = this.addProperty(ConfigTypes.INT,
            "Lookup.Cache.Size",
            5,
            "Amount of RTP locations to keep cached for each world.");

    private final ConfigProperty<Integer> cacheRefillThreshold = this.addProperty(ConfigTypes.INT,
            "Lookup.Cache.Refill_Threshold",
            2,
            "Amount of cached locations remaining that will trigger an immediate background refill.");

    private final ConfigProperty<Integer> cacheRefillInterval = this.addProperty(ConfigTypes.INT,
            "Lookup.Cache.Refill_Interval",
            30,
            "Interval (in seconds) to periodically refill caches for worlds that were used before.",
            "Set to '0' to refill only on demand.");

    private final ConfigProperty<Map<String, LookupRange>> lookupRanges = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(RANGE_INFO_CONFIG_TYPE),
            "Lookup.Ranges",
            getDefaultRangeMap(),
            "Per-world RTP range configuration.");

    private final ConfigProperty<Boolean> protectionEnabled = this.addProperty(ConfigTypes.BOOLEAN,
            "Lookup.Protection.Enabled",
            true,
            "If enabled, RTP will not teleport players into protected or claimed land.",
            "Supported protection plugins (detected automatically): WorldGuard regions, GriefPrevention claims.");

    private final ConfigProperty<Set<String>> protectionIgnoredHooks = this.addProperty(
            ConfigTypes.STRING_SET_LOWER_CASE, "Lookup.Protection.Ignored_Hooks",
            Set.of(),
            "Protection hooks to ignore. Available: worldguard, griefprevention.");

    private final ConfigProperty<Boolean> protectionIgnoreGlobalRegion = this.addProperty(ConfigTypes.BOOLEAN,
            "Lookup.Protection.WorldGuard.Ignore_Global_Region",
            true,
            "If enabled, the global '__global__' WorldGuard region will not block RTP teleports.");

    private final ConfigProperty<Double> teleportCost = this.addProperty(ConfigTypes.DOUBLE, "Teleport.Cost",
            0D,
            "Sets how much it costs (in Vault currency) to teleport to a random location.",
            "Set to '0' to disable.");

    private final ConfigProperty<String> teleportParticleType = this.addProperty(ConfigTypes.STRING,
            "Teleport.Particles.Type",
            "NONE",
            "Sets the particle shown at the destination on teleport.",
            "Set to 'NONE' to disable.");

    private final ConfigProperty<Integer> teleportParticleCount = this.addProperty(ConfigTypes.INT,
            "Teleport.Particles.Count",
            30,
            "Amount of particles to spawn.");

    private final ConfigProperty<Double> teleportParticleOffset = this.addProperty(ConfigTypes.DOUBLE,
            "Teleport.Particles.Offset",
            0.5D,
            "Random offset of the spawned particles.");

    private final ConfigProperty<Double> teleportParticleSpeed = this.addProperty(ConfigTypes.DOUBLE,
            "Teleport.Particles.Speed",
            0.05D,
            "Particle speed/extra data.");

    public boolean isFallbackEnabled() {
        return this.fallbackEnabled.get();
    }

    public String getFallbackWorld() {
        return this.fallbackWorld.get();
    }

    public LookupMode getLookupMode() {
        return this.lookupMode.get();
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

    public boolean isCacheEnabled() {
        return this.cacheEnabled.get();
    }

    public int getCacheSize() {
        return this.cacheSize.get();
    }

    public int getCacheRefillThreshold() {
        return this.cacheRefillThreshold.get();
    }

    public int getCacheRefillInterval() {
        return this.cacheRefillInterval.get();
    }

    public Map<String, LookupRange> getLookupRangesMap() {
        return this.lookupRanges.get();
    }

    public boolean isProtectionEnabled() {
        return this.protectionEnabled.get();
    }

    public Set<String> getProtectionIgnoredHooks() {
        return this.protectionIgnoredHooks.get();
    }

    public boolean isProtectionIgnoreGlobalRegion() {
        return this.protectionIgnoreGlobalRegion.get();
    }

    public double getTeleportCost() {
        return this.teleportCost.get();
    }

    public Optional<Particle> getTeleportParticle() {
        return Optional.ofNullable(Enums.get(this.teleportParticleType.get(), Particle.class));
    }

    public int getTeleportParticleCount() {
        return this.teleportParticleCount.get();
    }

    public double getTeleportParticleOffset() {
        return this.teleportParticleOffset.get();
    }

    public double getTeleportParticleSpeed() {
        return this.teleportParticleSpeed.get();
    }

    private static final Set<BlockFace> DIRECTIONS = Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST,
            BlockFace.SOUTH);

    public static Map<String, LookupRange> getDefaultRangeMap() {
        Map<String, LookupRange> map = new LinkedHashMap<>();

        map.put("world", new LookupRange(0, 0, 1000, 10_000, DIRECTIONS));
        map.put("world_nether", new LookupRange(0, 0, 1000, 5000, DIRECTIONS));
        map.put("world_the_end", new LookupRange(0, 0, 1000, 5000, DIRECTIONS));

        return map;
    }
}
