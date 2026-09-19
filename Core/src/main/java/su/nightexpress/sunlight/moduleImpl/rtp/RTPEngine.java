package su.nightexpress.sunlight.moduleImpl.rtp;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.bridge.scheduler.AdaptedTask;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.TimeUtil;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.random.Rnd;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.hook.protection.ProtectionManager;
import su.nightexpress.sunlight.moduleImpl.rtp.config.LookupMode;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPLang;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPPerms;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPSettings;
import su.nightexpress.sunlight.moduleImpl.rtp.model.LookupRange;
import su.nightexpress.sunlight.teleport.TeleportContext;
import su.nightexpress.sunlight.teleport.TeleportFlag;
import su.nightexpress.sunlight.teleport.TeleportManager;
import su.nightexpress.sunlight.teleport.TeleportType;
import su.nightexpress.sunlight.utils.EconomyUtils;

public class RTPEngine {

    public record LastRTP(Location origin, Location destination) {

        public double distance() {
            double dx = this.origin.getX() - this.destination.getX();
            double dz = this.origin.getZ() - this.destination.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    private record Candidate(int locX, int locZ, int chunkX, int chunkZ, int bX, int bZ) {

        public static Candidate random(LookupRange range) {
            Set<BlockFace> directions = range.getDirections();
            Set<BlockFace> directionsX = Lists.newSet(BlockFace.EAST, BlockFace.WEST);
            Set<BlockFace> directionsZ = Lists.newSet(BlockFace.SOUTH, BlockFace.NORTH);
            directionsX.retainAll(directions);
            directionsZ.retainAll(directions);
            if (directionsX.isEmpty() && directionsZ.isEmpty())
                return null;

            int distanceX = Rnd.get(range.getDistanceMin(), range.getDistanceMax());
            int distanceZ = Rnd.get(range.getDistanceMin(), range.getDistanceMax());

            BlockFace directionX = directionsX.isEmpty() ? BlockFace.UP : Rnd.get(directionsX); // UP for zero modifier
            BlockFace directionZ = directionsZ.isEmpty() ? BlockFace.UP : Rnd.get(directionsZ); // UP for zero modifier

            int locX = range.getStartX() + (directionX.getModX() * distanceX);
            int locZ = range.getStartZ() + (directionZ.getModZ() * distanceZ);

            return new Candidate(locX, locZ, locX >> 4, locZ >> 4, locX & 0xF, locZ & 0xF);
        }
    }

    private final ProtectionManager protectionManager;
    private final TeleportManager teleportManager;
    private final RTPSettings settings;
    private final RTPModule module;

    private final Map<UUID, LastRTP> lastRTPMap;
    private final Map<String, Queue<Location>> locationCache;
    private final Set<String> refilling;
    private AdaptedTask cacheRefillTask;

    public RTPEngine(final RTPModule module, final TeleportManager teleportManager) {
        this.settings = new RTPSettings();
        this.module = module;
        this.teleportManager = teleportManager;
        this.protectionManager = new ProtectionManager(() -> this.settings.isProtectionIgnoreGlobalRegion());
        this.lastRTPMap = new ConcurrentHashMap<>();
        this.locationCache = new ConcurrentHashMap<>();
        this.refilling = ConcurrentHashMap.newKeySet();
    }

    public void load(final FileConfig config) {
        this.settings.load(config);
        this.clearCache();
        this.restartCacheRefill();
    }

    public void shutdown() {
        if (this.cacheRefillTask != null) {
            this.cacheRefillTask.cancel();
            this.cacheRefillTask = null;
        }

        this.locationCache.clear();
        this.refilling.clear();
        this.lastRTPMap.clear();
    }

    public LookupRange getWorldRange(final String name) {
        return this.settings.getLookupRangesMap().get(LowerCase.INTERNAL.apply(name));
    }

    public Optional<LastRTP> getLastRTP(final Player player) {
        return Optional.ofNullable(this.lastRTPMap.get(player.getUniqueId()));
    }

    public boolean teleportToRandomPlace(final Player player, World world) {
        world = world == null ? player.getWorld() : world;
        LookupRange lookupRange = this.getWorldRange(world.getName());

        if (lookupRange == null && this.settings.isFallbackEnabled()) {
            final World fallbackWorld = this.module.plugin().getServer().getWorld(this.settings.getFallbackWorld());
            if (fallbackWorld != null) {
                world = fallbackWorld;
                lookupRange = this.getWorldRange(fallbackWorld.getName());
            }
        }

        if (lookupRange == null) {
            this.module.sendPrefixed(RTPLang.TELEPORT_ERROR_INVALID_RANGE, player);
            return false;
        }

        if (!player.hasPermission(RTPPerms.COMMAND_RTP) && !player.hasPermission(RTPPerms.world(world.getName()))) {
            this.module.sendPrefixed(RTPLang.TELEPORT_ERROR_NO_WORLD_ACCESS, player);
            return false;
        }

        final int maxAttempts = lookupRange.getMaxAttempts() > 0 ? lookupRange.getMaxAttempts()
                : this.settings.getLookupMaxAttempts();
        final Location origin = player.getLocation().clone();

        if (this.settings.isCacheEnabled()) {
            final Location cached = this.pollCachedLocation(world.getName());
            if (cached != null) {
                this.completeTeleport(player, origin, Optional.of(cached));

                if (this.cachedLocationCount(world.getName()) <= this.settings.getCacheRefillThreshold())
                    this.refillCache(world, lookupRange, maxAttempts);

                return true;
            }

            this.refillCache(world, lookupRange, maxAttempts);
        }

        this.findLocation(world, lookupRange, maxAttempts)
                .thenAcceptAsync(location -> this.completeTeleport(player, origin, location),
                        this.module.plugin()::runTask);

        return true;
    }

    private void completeTeleport(final Player player, final Location origin, final Optional<Location> location) {
        if (location.isEmpty()) {
            this.module.sendPrefixed(RTPLang.RANDOM_LOCATION_TELEPORT_FAILURE, player);
            return;
        }

        final Location destination = location.get().clone();

        final double cost = this.settings.getTeleportCost();
        final boolean charge = cost > 0D && !EconomyUtils.hasBypass(player, RTPPerms.BYPASS_COST)
                && EconomyUtils.hasCurrency();

        if (charge && !EconomyUtils.canAfford(player, cost)) {
            this.module.sendPrefixed(Lang.COST_ERROR_NOT_ENOUGH_FUNDS, player, builder -> builder
                    .with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost)));
            return;
        }

        final LastRTP lastRTP = new LastRTP(origin, destination);

        final TeleportContext teleportContext = TeleportContext.builder(this.module, player, destination)
                .withFlag(TeleportFlag.CENTERED)
                .withFlag(TeleportFlag.KEEP_DIRECTION)
                .withFlag(TeleportFlag.AVOID_LAVA)
                .callback(() -> {
                    if (charge)
                        EconomyUtils.withdraw(player, cost);

                    this.lastRTPMap.put(player.getUniqueId(), lastRTP);
                    this.spawnParticles(player, destination);

                    this.module.sendPrefixed(RTPLang.RANDOM_LOCATION_TELEPORT_SUCCESS, player,
                            builder -> builder
                                    .with(CommonPlaceholders.LOCATION.resolver(destination))
                                    .with(SLPlaceholders.GENERIC_DISTANCE,
                                            () -> NumberUtil.format(lastRTP.distance())));
                })
                .build();

        this.teleportManager.teleport(teleportContext, TeleportType.RTP);
    }

    private void clearCache() {
        this.locationCache.clear();
        this.refilling.clear();
    }

    private void restartCacheRefill() {
        if (this.cacheRefillTask != null) {
            this.cacheRefillTask.cancel();
            this.cacheRefillTask = null;
        }

        final int interval = this.settings.getCacheRefillInterval();
        if (!this.settings.isCacheEnabled() || interval <= 0)
            return;

        final long ticks = TimeUtil.secondsToTicks(interval);
        this.cacheRefillTask = this.module.plugin().scheduler()
                .runTaskTimer(this::refillCachedWorlds, ticks, ticks);
    }

    private void refillCachedWorlds() {
        this.locationCache.keySet().forEach(this::refillCache);
    }

    private Location pollCachedLocation(final String worldName) {
        final Queue<Location> queue = this.locationCache.get(LowerCase.INTERNAL.apply(worldName));
        if (queue == null)
            return null;

        Location location;
        while ((location = queue.poll()) != null) {
            if (location.getWorld() == null)
                continue;
            if (this.settings.isProtectionEnabled() && this.isProtected(location))
                continue;

            return location;
        }

        return null;
    }

    private int cachedLocationCount(final String worldName) {
        final Queue<Location> queue = this.locationCache.get(LowerCase.INTERNAL.apply(worldName));
        return queue == null ? 0 : queue.size();
    }

    private void refillCache(final World world, final LookupRange range, final int maxAttempts) {
        final String key = LowerCase.INTERNAL.apply(world.getName());
        if (!this.refilling.add(key))
            return;

        this.refillToSize(key, world, range, maxAttempts).whenComplete((ignored, error) -> {
            if (error != null)
                this.module.error("RTP location cache refill failed for world '" + world.getName() + "': "
                        + error.getMessage());

            this.refilling.remove(key);
        });
    }

    private void refillCache(final String worldName) {
        final World world = this.module.plugin().getServer().getWorld(worldName);
        if (world == null)
            return;

        final LookupRange range = this.getWorldRange(world.getName());
        if (range == null)
            return;

        final int maxAttempts = range.getMaxAttempts() > 0 ? range.getMaxAttempts() : this.settings.getLookupMaxAttempts();
        this.refillCache(world, range, maxAttempts);
    }

    private CompletableFuture<Void> refillToSize(final String key, final World world, final LookupRange range,
            final int maxAttempts) {
        final Queue<Location> queue = this.locationCache.computeIfAbsent(key, unused -> new ConcurrentLinkedQueue<>());
        final int target = this.settings.getCacheSize();
        final int needed = target - queue.size();
        if (target <= 0 || needed <= 0)
            return CompletableFuture.completedFuture(null);

        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (int index = 0; index < needed; index++) {
            chain = chain.thenCompose(ignored -> this.findLocation(world, range, maxAttempts))
                    .thenAccept(found -> found.ifPresent(location -> {
                        if (this.settings.getCacheSize() > queue.size()) {
                            queue.add(location);
                        }
                    }));
        }

        return chain;
    }

    private void spawnParticles(final Player player, final Location location) {
        Optional<Particle> particle = this.settings.getTeleportParticle();
        if (particle.isEmpty() || location.getWorld() == null)
            return;

        int count = this.settings.getTeleportParticleCount();
        double offset = this.settings.getTeleportParticleOffset();
        double speed = this.settings.getTeleportParticleSpeed();

        location.getWorld().spawnParticle(particle.get(), location, count, offset, offset, offset, speed);
    }

    public boolean isProtected(final Location location) {
        if (!this.settings.isProtectionEnabled())
            return false;

        return this.protectionManager.isProtected(location, this.settings.getProtectionIgnoredHooks());
    }

    public CompletableFuture<Optional<Location>> findLocation(final World world, final LookupRange range,
            final int maxAttempts) {
        return this.findAttempt(world, range, maxAttempts, 0);
    }

    private CompletableFuture<Optional<Location>> findAttempt(final World world, final LookupRange range,
            final int maxAttempts, final int count) {
        if (count >= maxAttempts)
            return CompletableFuture.completedFuture(Optional.empty());

        Candidate candidate = Candidate.random(range);
        if (candidate == null)
            return this.findAttempt(world, range, maxAttempts, count + 1);

        if (this.settings.isLookupGeneratedChunksOnly()
                && !world.isChunkGenerated(candidate.chunkX(), candidate.chunkZ())) {
            return this.findAttempt(world, range, maxAttempts, count + 1);
        }

        if (this.settings.isLookupLoadedChunksOnly() && !world.isChunkLoaded(candidate.chunkX(), candidate.chunkZ())) {
            return this.findAttempt(world, range, maxAttempts, count + 1);
        }

        CompletableFuture<Chunk> chunkFuture;
        if (this.settings.getLookupMode() == LookupMode.SYNC
                || world.isChunkLoaded(candidate.chunkX(), candidate.chunkZ())) {
            chunkFuture = CompletableFuture.completedFuture(world.getChunkAt(candidate.chunkX(), candidate.chunkZ()));
        } else {
            chunkFuture = world.getChunkAtAsync(candidate.chunkX(), candidate.chunkZ());
        }

        return chunkFuture.handleAsync((chunk, error) -> {
            if (error != null || chunk == null) {
                this.module.error("RTP chunk lookup failed. Chunk: %s, %s"
                        .formatted(candidate.chunkX(), candidate.chunkZ()));
                return Optional.<Location>empty();
            }

            Optional<Location> found = this.validate(chunk, candidate, range);
            if (found.isEmpty() || this.isProtected(found.get()))
                return Optional.<Location>empty();

            return found;
        }, this.module.plugin()::runTask).thenCompose(found -> {
            if (found.isPresent())
                return CompletableFuture.completedFuture(found);

            return this.findAttempt(world, range, maxAttempts, count + 1);
        });
    }

    private Optional<Location> validate(final Chunk chunk, final Candidate candidate, final LookupRange range) {
        World world = chunk.getWorld();
        if (world == null)
            return Optional.empty();

        int locX = candidate.locX();
        int locZ = candidate.locZ();
        int bX = candidate.bX();
        int bZ = candidate.bZ();
        int bY;

        if (world.getEnvironment() == World.Environment.NETHER) {
            int start = Math.max(world.getMinHeight(), range.getYMin());
            int end = world.getHighestBlockYAt(locX, locZ);
            if (end < start)
                return Optional.empty();

            Integer found = null;
            boolean wasAir = false;

            for (int y = end; y > start; y--) {
                Material blockType = chunk.getBlock(bX, y, bZ).getType();
                if (wasAir && blockType.isBlock() && blockType.isSolid() && blockType != Material.BEDROCK) {
                    found = y;
                    break;
                }
                wasAir = blockType.isAir();
            }

            if (found == null)
                return Optional.empty();

            bY = found;
        } else {
            bY = world.getHighestBlockYAt(locX, locZ);
        }

        int minY = Math.max(world.getMinHeight(), range.getYMin());
        int maxY = range.getYMax() > 0 ? Math.min(world.getMaxHeight(), range.getYMax()) : world.getMaxHeight();
        if (bY < minY || bY > maxY)
            return Optional.empty();

        Material material = chunk.getBlock(bX, bY, bZ).getType();
        if (!material.isBlock() || !material.isSolid() || range.getBlockedBlocks().contains(material))
            return Optional.empty();

        Biome biome = world.getBiome(locX, bY, bZ);
        String biomeKey = biome.getKey().toString();

        if (!range.getBiomeWhitelist().isEmpty() && !range.getBiomeWhitelist().contains(biomeKey))
            return Optional.empty();
        if (range.getBiomeBlacklist().contains(biomeKey))
            return Optional.empty();

        return Optional.of(new Location(world, locX, bY + 1, locZ));
    }
}