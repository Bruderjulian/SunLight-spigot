package su.nightexpress.sunlight.teleport;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.manager.SimpleManager;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.event.SunlightPlayerTeleportEvent;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.nms.SunNMS;

public class TeleportManager extends SimpleManager<SunLightPlugin> {

    private final SunNMS internals;

    public TeleportManager(final SunLightPlugin plugin, final SunNMS internals) {
        super(plugin);
        this.internals = internals;
    }

    @Override
    protected void onLoad() {

    }

    @Override
    protected void onShutdown() {

    }

    public boolean teleport(final TeleportContext context, final TeleportType type) {
        if (context == null || context.getTarget() == null || context.getDestination() == null) return false;
        final SunlightPlayerTeleportEvent event = new SunlightPlayerTeleportEvent(context, type);
        try {
            this.plugin.getPluginManager().callEvent(event);
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
        if (event.isIntercepted())
            return true;
        if (event.isCancelled())
            return false;

        return this.move(context);
    }

    public boolean move(final TeleportContext context) {
        if (context == null || context.getTarget() == null) return false;
        final Location destination = this.getDestination(context);
        if (destination == null || destination.getWorld() == null) {
            context.getModule().sendPrefixed(
                    context.hasSender() ? Lang.TELEPORT_UNSAFE_FEEDBACK : Lang.TELEPORT_UNSAFE_NOTIFY,
                    context.getExecutor(), builder -> builder
                            .with(CommonPlaceholders.PLAYER.resolver(context.getTarget())));
            return false;
        }

        final Player player = context.getTarget();

        if (player.isOnline()) {
            if (!this.isFolia()) {
                if (!player.teleport(destination)) {
                    return false;
                }
            } else {
                // Folia: entity scheduler teleport. Fall back to sync teleport if scheduler is unavailable.
                try {
                    player.getScheduler().run(this.plugin, task -> player.teleport(destination), null);
                } catch (Exception | NoSuchMethodError exception) {
                    if (!player.teleport(destination)) return false;
                }
            }
        } else {
            if (this.internals == null) {
                context.getModule().sendPrefixed(Lang.TELEPORT_NO_OFFLINE_HANDLER_FEEDBACK, context.getExecutor());
                return false;
            }
            try {
                this.internals.teleport(player, destination);
            } catch (Exception exception) {
                exception.printStackTrace();
                return false;
            }
        }

        try {
            context.runCallback();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return true;
    }

    /**
     * Async-safe entry point: ensures the destination chunk is loaded before
     * moving the player on the main thread. Falls back to {@link #move} when
     * Paper's async chunk API is unavailable.
     */
    public void teleportAsync(final TeleportContext context, final TeleportType type) {
        if (context == null) return;
        Location raw = null;
        try {
            raw = context.getDestination();
        } catch (Exception ignored) {
        }
        if (raw == null || raw.getWorld() == null || context.getTarget() == null) {
            this.move(context);
            return;
        }
        final Location snapshot = raw.clone();
        try {
            snapshot.getWorld().getChunkAtAsync(snapshot).thenAccept(chunk -> {
                try {
                    this.plugin.runTask(task -> this.teleport(context, type));
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            });
        } catch (Exception | NoSuchMethodError exception) {
            this.teleport(context, type);
        }
    }

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private Location getDestination(final TeleportContext context) {
        final Location raw = context.getDestination();
        if (raw == null) return null;
        if (!context.hasFlags())
            return raw.clone();

        final World world = raw.getWorld();
        if (world == null)
            return null;

        final Location location;
        try {
            location = raw.clone();
        } catch (Exception e) {
            return null;
        }

        if (context.hasFlag(TeleportFlag.LOOK_FOR_SURFACE)) {
            try {
                if (!location.isChunkLoaded()) return null;
            } catch (Exception | NoSuchMethodError ignored) {
            }
            Block block;
            try {
                block = location.getBlock();
            } catch (Exception e) {
                return null;
            }
            final boolean isSolid = isSolidBlock(block);
            final BlockFace face = isSolid ? BlockFace.UP : BlockFace.DOWN;

            int steps = 0;
            final int maxSteps = Math.max(8, world.getMaxHeight() - world.getMinHeight() + 8);
            while (true) {
                if (++steps > maxSteps) return null;
                final Block relative;
                try {
                    relative = block.getRelative(face);
                } catch (Exception e) {
                    return null;
                }
                if (isSolidBlock(relative) == !isSolid)
                    break;

                final int y = relative.getY();
                if (y < world.getMinHeight() || y > world.getMaxHeight())
                    return null;

                block = relative;
            }

            final double delta = location.getY() - block.getY();
            if (delta > 0) {
                location.setY(location.getY() - delta);
            }
        }

        if (context.hasFlag(TeleportFlag.AVOID_LAVA)) {
            Material type;
            try {
                type = location.getBlock().getType();
            } catch (Exception e) {
                return null;
            }
            if (type == Material.LAVA || type == Material.MAGMA_BLOCK || type == Material.CACTUS
                || type == Material.FIRE || type == Material.SOUL_FIRE) {
                return null;
            }
            // Void guard: y below min height is never safe.
            if (location.getY() < world.getMinHeight()) return null;
        }
        if (context.hasFlag(TeleportFlag.CENTERED)) {
            location.setX(location.getBlockX() + 0.5);
            location.setZ(location.getBlockZ() + 0.5);
        }
        if (context.hasFlag(TeleportFlag.KEEP_DIRECTION)) {
            final Location source = context.getTarget().getLocation();
            location.setYaw(source.getYaw());
            location.setPitch(source.getPitch());
        }

        return location;
    }

    private static boolean isSolidBlock(final Block block) {
        return !block.isEmpty() && block.getType().isSolid();
    }
}
