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
        final SunlightPlayerTeleportEvent event = new SunlightPlayerTeleportEvent(context, type);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isIntercepted())
            return true;
        if (event.isCancelled())
            return false;

        return this.move(context);
    }

    public boolean move(final TeleportContext context) {
        final Location destination = this.getDestination(context);
        if (destination == null) {
            context.getModule().sendPrefixed(
                    context.hasSender() ? Lang.TELEPORT_UNSAFE_FEEDBACK : Lang.TELEPORT_UNSAFE_NOTIFY,
                    context.getExecutor(), builder -> builder
                            .with(CommonPlaceholders.PLAYER.resolver(context.getTarget())));
            return false;
        }

        final Player player = context.getTarget();

        if (player.isOnline()) {
            if (!player.teleport(destination)) {
                return false;
            }
        } else {
            if (this.internals == null) {
                context.getModule().sendPrefixed(Lang.TELEPORT_NO_OFFLINE_HANDLER_FEEDBACK, context.getExecutor());
                return false;
            }
            this.internals.teleport(player, destination);
        }

        context.runCallback();
        return true;
    }

    private Location getDestination(final TeleportContext context) {
        final Location destination = context.getDestination();
        if (!context.hasFlags())
            return destination;

        final World world = destination.getWorld();
        if (world == null)
            return null;

        final Location location = destination.clone();

        if (context.hasFlag(TeleportFlag.LOOK_FOR_SURFACE)) {
            Block block = location.getBlock();
            final boolean isSolid = isSolidBlock(block);
            final BlockFace face = isSolid ? BlockFace.UP : BlockFace.DOWN;

            while (true) {
                final Block relative = block.getRelative(face);
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
            if (location.getBlock().getType() == Material.LAVA) {
                return null;
            }
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
