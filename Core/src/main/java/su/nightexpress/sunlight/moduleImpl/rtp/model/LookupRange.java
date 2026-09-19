package su.nightexpress.sunlight.moduleImpl.rtp.model;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.Enums;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class LookupRange implements Writeable {

    private static final Set<Material> DEFAULT_BLOCKED_BLOCKS = Set.of(Material.CACTUS, Material.MAGMA_BLOCK);

    private final int startX;
    private final int startZ;
    private final int distanceMin;
    private final int distanceMax;
    private final Set<BlockFace> directions;
    private final int yMin;
    private final int yMax;
    private final Set<Material> blockedBlocks;
    private final Set<String> biomeBlacklist;
    private final Set<String> biomeWhitelist;
    private final int maxAttempts;

    public LookupRange(int startX, int startZ, int distanceMin, int distanceMax, Set<BlockFace> directions) {
        this(startX, startZ, distanceMin, distanceMax, directions, 0, 0, DEFAULT_BLOCKED_BLOCKS, Set.of(), Set.of(), 0);
    }

    public LookupRange(int startX, int startZ, int distanceMin, int distanceMax, Set<BlockFace> directions,
            int yMin, int yMax, Set<Material> blockedBlocks, Set<String> biomeBlacklist, Set<String> biomeWhitelist,
            int maxAttempts) {
        this.startX = startX;
        this.startZ = startZ;
        this.distanceMin = Math.abs(distanceMin);
        this.distanceMax = Math.abs(distanceMax);
        this.directions = directions;
        this.yMin = yMin;
        this.yMax = yMax;
        this.blockedBlocks = blockedBlocks;
        this.biomeBlacklist = biomeBlacklist;
        this.biomeWhitelist = biomeWhitelist;
        this.maxAttempts = maxAttempts;
    }

    public static LookupRange read(FileConfig config, String path) {
        int startX = config.getInt(path + ".Start_X");
        int startZ = config.getInt(path + ".Start_Z");
        int distanceMin = config.getInt(path + ".Distance_Min");
        int distanceMax = config.getInt(path + ".Distance_Max");
        Set<BlockFace> directions = config.getStringSet(path + ".Directions").stream()
                .map(string -> Enums.get(string, BlockFace.class))
                .filter(Objects::nonNull).filter(BlockFace::isCartesian).collect(Collectors.toSet());

        int yMin = config.getInt(path + ".Y.Min", 0);
        int yMax = config.getInt(path + ".Y.Max", 0);

        Set<Material> blockedBlocks = readBlocks(config, path + ".Blocked_Blocks");
        Set<String> biomeBlacklist = readKeys(config, path + ".Biomes.Blacklist");
        Set<String> biomeWhitelist = readKeys(config, path + ".Biomes.Whitelist");
        int maxAttempts = config.getInt(path + ".Max_Attempts", 0);

        return new LookupRange(startX, startZ, distanceMin, distanceMax, directions,
                yMin, yMax, blockedBlocks, biomeBlacklist, biomeWhitelist, maxAttempts);
    }

    private static Set<Material> readBlocks(FileConfig config, String path) {
        if (config.get(path) == null)
            return DEFAULT_BLOCKED_BLOCKS;

        return config.getStringSet(path).stream()
                .map(string -> Enums.get(string, Material.class))
                .filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
    }

    private static Set<String> readKeys(FileConfig config, String path) {
        return config.getStringSet(path).stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Start_X", this.startX);
        config.set(path + ".Start_Z", this.startZ);
        config.set(path + ".Distance_Min", this.getDistanceMin());
        config.set(path + ".Distance_Max", this.getDistanceMax());
        config.set(path + ".Directions", this.getDirections().stream().map(Enum::name).toList());

        config.set(path + ".Y.Min", this.yMin);
        config.set(path + ".Y.Max", this.yMax);
        config.set(path + ".Blocked_Blocks", this.blockedBlocks.stream().map(Enum::name).toList());
        config.set(path + ".Biomes.Blacklist", this.biomeBlacklist);
        config.set(path + ".Biomes.Whitelist", this.biomeWhitelist);
        config.set(path + ".Max_Attempts", this.maxAttempts);
    }

    public int getStartX() {
        return this.startX;
    }

    public int getStartZ() {
        return this.startZ;
    }

    public int getDistanceMin() {
        return this.distanceMin;
    }

    public int getDistanceMax() {
        return this.distanceMax;
    }

    public Set<BlockFace> getDirections() {
        return this.directions;
    }

    public int getYMin() {
        return this.yMin;
    }

    public int getYMax() {
        return this.yMax;
    }

    public Set<Material> getBlockedBlocks() {
        return this.blockedBlocks;
    }

    public Set<String> getBiomeBlacklist() {
        return this.biomeBlacklist;
    }

    public Set<String> getBiomeWhitelist() {
        return this.biomeWhitelist;
    }

    public int getMaxAttempts() {
        return this.maxAttempts;
    }
}