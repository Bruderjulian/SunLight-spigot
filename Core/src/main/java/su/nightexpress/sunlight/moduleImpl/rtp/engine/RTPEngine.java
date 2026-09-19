package su.nightexpress.sunlight.moduleImpl.rtp.engine;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.random.Rnd;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.hook.protection.ProtectionManager;
import su.nightexpress.sunlight.moduleImpl.rtp.RTPModule;
import su.nightexpress.sunlight.moduleImpl.rtp.config.LookupMode;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPLang;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPPerms;
import su.nightexpress.sunlight.teleport.TeleportContext;
import su.nightexpress.sunlight.teleport.TeleportFlag;
import su.nightexpress.sunlight.teleport.TeleportManager;
import su.nightexpress.sunlight.teleport.TeleportType;
import su.nightexpress.sunlight.utils.EconomyUtils;
import su.nightexpress.sunlight.utils.Utils;

public class RTPEngine {

    public record LastRTP(Location origin, Location destination) {

        public double distance() {
            final double dx = this.origin.getX() - this.destination.getX();
            final double dz = this.origin.getZ() - this.destination.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    private record Candidate(int locX, int locZ, int chunkX, int chunkZ, int bX, int bZ) {

        public static Candidate random(final LookupRange range) {
            final Set<BlockFace> directions = range.getDirections();
            final Set<BlockFace> directionsX = Lists.newSet(BlockFace.EAST, BlockFace.WEST);
            final Set<BlockFace> directionsZ = Lists.newSet(BlockFace.SOUTH, BlockFace.NORTH);
            directionsX.retainAll(directions);
            directionsZ.retainAll(directions);
            if (directionsX.isEmpty() && directionsZ.isEmpty())
                return null;

            final int distanceX = Rnd.get(range.getDistanceMin(), range.getDistanceMax());
            final int distanceZ = Rnd.get(range.getDistanceMin(), range.getDistanceMax());

            final BlockFace directionX = directionsX.isEmpty() ? BlockFace.UP : Rnd.get(directionsX); // UP for zero
                                                                                                      // modifier
            final BlockFace directionZ = directionsZ.isEmpty() ? BlockFace.UP : Rnd.get(directionsZ); // UP for zero
                                                                                                      // modifier

            final int locX = range.getStartX() + (directionX.getModX() * distanceX);
            final int locZ = range.getStartZ() + (directionZ.getModZ() * distanceZ);

            return new Candidate(locX, locZ, locX >> 4, locZ >> 4, locX & 0xF, locZ & 0xF);
        }
    }

    private final ProtectionManager protectionManager;
    private final TeleportManager teleportManager;
    private final RTPModule module;

    private final Map<UUID, LastRTP> lastRTPMap;
    private final RtpCache cache;
    private final int maxAttempts;

    public RTPEngine(final RTPModule module, final TeleportManager teleportManager) {
        this.module = module;
        this.teleportManager = teleportManager;
        this.protectionManager = new ProtectionManager(() -> module.getSettings().isProtectionIgnoreGlobalRegion());
        this.lastRTPMap = new ConcurrentHashMap<>();
        this.cache = new RtpCache(module);
        this.maxAttempts = module.getSettings().getLookupMaxAttempts();
    }

    public void load() {
        cache.start();
    }

    public void shutdown() {
        cache.shutdown();
        this.lastRTPMap.clear();
    }

    public LookupRange getWorldRange(final String name) {
        return module.getSettings().getLookupRangesMap().get(Utils.lowercase(name));
    }

    public Optional<LastRTP> getLastRTP(final Player player) {
        return Optional.ofNullable(this.lastRTPMap.get(player.getUniqueId()));
    }

    public boolean teleportToRandomPlace(final Player player, World world) {
        world = world == null ? player.getWorld() : world;
        String worldName = Utils.lowercase(world.getName());
        LookupRange lookupRange = this.getWorldRange(worldName);

        if (lookupRange == null && module.getSettings().isFallbackEnabled()) {
            final World fallbackWorld = this.module.plugin().getServer().getWorld(
                    module.getSettings().getFallbackWorld());
            if (fallbackWorld != null) {
                world = fallbackWorld;
                worldName = Utils.lowercase(fallbackWorld.getName());
                lookupRange = this.getWorldRange(fallbackWorld.getName());
            }
        }
        if (lookupRange == null) {
            this.module.sendPrefixed(RTPLang.TELEPORT_ERROR_INVALID_RANGE, player);
            return false;
        }

        if (!player.hasPermission(RTPPerms.COMMAND_RTP) && !player.hasPermission(RTPPerms.world(worldName))) {
            this.module.sendPrefixed(RTPLang.TELEPORT_ERROR_NO_WORLD_ACCESS, player);
            return false;
        }

        final Location origin = player.getLocation().clone();

        if (module.getSettings().isCacheEnabled()) {
            final Location cached = cache.retrieveLocation(world, lookupRange, worldName);
            if (cached != null) {
                this.completeTeleport(player, origin, Optional.of(cached));
                return true;
            }
        }

        this.findLocation(world, lookupRange)
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

        final double cost = module.getSettings().getTeleportCost();
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
                    this.spawnParticles(destination);

                    this.module.sendPrefixed(RTPLang.RANDOM_LOCATION_TELEPORT_SUCCESS, player,
                            builder -> builder
                                    .with(CommonPlaceholders.LOCATION.resolver(destination))
                                    .with(SLPlaceholders.GENERIC_DISTANCE,
                                            () -> NumberUtil.format(lastRTP.distance())));
                })
                .build();

        this.teleportManager.teleport(teleportContext, TeleportType.RTP);
    }

    private void spawnParticles(final Location location) {
        final Optional<Particle> particle = module.getSettings().getTeleportParticle();
        if (particle.isEmpty() || location.getWorld() == null)
            return;

        final double offset = module.getSettings().getTeleportParticleOffset();
        location.getWorld().spawnParticle(
                particle.get(),
                location,
                module.getSettings().getTeleportParticleCount(),
                offset, offset, offset,
                module.getSettings().getTeleportParticleSpeed());
    }

    public boolean isProtected(final Location location) {
        if (!module.getSettings().isProtectionEnabled())
            return false;

        return this.protectionManager.isProtected(location, module.getSettings().getProtectionIgnoredHooks());
    }

    public CompletableFuture<Optional<Location>> findLocation(final World world, final LookupRange range) {
        return this.findAttempt(world, range, 0);
    }

    private CompletableFuture<Optional<Location>> findAttempt(final World world, final LookupRange range,
            final int count) {
        if (count >= maxAttempts)
            return CompletableFuture.completedFuture(Optional.empty());

        final Candidate candidate = Candidate.random(range);
        if (candidate == null)
            return this.findAttempt(world, range, count + 1);

        if (module.getSettings().isLookupGeneratedChunksOnly()
                && !world.isChunkGenerated(candidate.chunkX(), candidate.chunkZ())) {
            return this.findAttempt(world, range, count + 1);
        }

        if (module.getSettings().isLookupLoadedChunksOnly()
                && !world.isChunkLoaded(candidate.chunkX(), candidate.chunkZ())) {
            return this.findAttempt(world, range, count + 1);
        }

        CompletableFuture<Chunk> chunkFuture;
        if (module.getSettings().getLookupMode() == LookupMode.SYNC
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

            final Optional<Location> found = this.validate(chunk, candidate, range);
            if (found.isEmpty() || this.isProtected(found.get()))
                return Optional.<Location>empty();

            return found;
        }, this.module.plugin()::runTask).thenCompose(found -> {
            if (found.isPresent())
                return CompletableFuture.completedFuture(found);

            return this.findAttempt(world, range, count + 1);
        });
    }

    private Optional<Location> validate(final Chunk chunk, final Candidate candidate, final LookupRange range) {
        final World world = chunk.getWorld();
        if (world == null)
            return Optional.empty();

        final int locX = candidate.locX();
        final int locZ = candidate.locZ();
        final int bX = candidate.bX();
        final int bZ = candidate.bZ();
        int bY;

        if (world.getEnvironment() == World.Environment.NETHER) {
            final int start = Math.max(world.getMinHeight(), range.getYMin());
            final int end = world.getHighestBlockYAt(locX, locZ);
            if (end < start)
                return Optional.empty();

            Integer found = null;
            boolean wasAir = false;

            for (int y = end; y > start; y--) {
                final Material blockType = chunk.getBlock(bX, y, bZ).getType();
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

        final int minY = Math.max(world.getMinHeight(), range.getYMin());
        final int maxY = range.getYMax() > 0 ? Math.min(world.getMaxHeight(), range.getYMax()) : world.getMaxHeight();
        if (bY < minY || bY > maxY)
            return Optional.empty();

        final Material material = chunk.getBlock(bX, bY, bZ).getType();
        if (!material.isBlock() || !material.isSolid() || range.getBlockedBlocks().contains(material))
            return Optional.empty();

        final String biomeKey = world.getBiome(locX, bY, bZ).getKey().toString();

        if (!range.getBiomeWhitelist().isEmpty() && !range.getBiomeWhitelist().contains(biomeKey)
                || range.getBiomeBlacklist().contains(biomeKey)) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, locX, bY + 1, locZ));
    }
}