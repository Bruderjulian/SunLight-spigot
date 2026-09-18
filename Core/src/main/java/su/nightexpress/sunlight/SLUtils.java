package su.nightexpress.sunlight;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.Plugins;
import su.nightexpress.nightcore.util.TimeUtil;
import su.nightexpress.sunlight.config.Config;
import su.nightexpress.sunlight.hook.HookId;
import su.nightexpress.sunlight.utils.Direction;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class SLUtils {

    public static final String CONSOLE_NAME = Bukkit.getServer().getConsoleSender().getName();

    private static DateTimeFormatter dateFormatter;
    private static DateTimeFormatter timeFormatter;

    public static void setDateFormatter(String pattern) {
        dateFormatter = DateTimeFormatter.ofPattern(pattern);
    }

    public static void setTimeFormatter(String pattern) {
        timeFormatter = DateTimeFormatter.ofPattern(pattern);
    }

    public static boolean hasPacketLibrary() {
        return Plugins.isInstalled(HookId.PACKET_EVENTS) || Plugins.isInstalled(HookId.PROTOCOL_LIB);
    }

    @Deprecated
    public static String createIdentifier(Player player) {
        String uuid = player.getUniqueId().toString();

        // Bedrock players have UUIDs leading with zeros.
        if (Players.isBedrock(player)) {
            uuid = new StringBuilder(uuid).reverse().toString();
        }

        return uuid;
    }

    public static boolean isVanished(Player player) {
        return player.hasMetadata("vanished");
    }

    private static final Direction[] DIRECTIONS = {
            Direction.EAST, Direction.NORTH_EAST, Direction.NORTH, Direction.NORTH_WEST,
            Direction.WEST, Direction.SOUTH_WEST, Direction.SOUTH, Direction.SOUTH_EAST
    };

    public static Direction getDirection(Location from, Location to) {
        int dx = to.getBlockX() - from.getBlockX();
        int dz = to.getBlockZ() - from.getBlockZ();
        if (Math.abs(dx) == 0 && Math.abs(dz) == 0)
            return Direction.HERE;

        // "y" is -dx (because -Z is North) and "x" is dx.
        double angle = Math.toDegrees(Math.atan2(-dz, dx));

        // "Normalize" the angle from [-180;180] to [0;360]
        double angleNormalized = (angle + 360) % 360;

        int slices = DIRECTIONS.length;
        double sliceAngle = 360D / slices;
        double halfSlice = sliceAngle / 2D;

        int index = (int) ((angleNormalized + halfSlice) / sliceAngle);

        int finalIndex = index % slices;
        return DIRECTIONS[finalIndex];
    }

    @Deprecated
    public static int clamp(long value, long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException(min + " > " + max);
        }
        return (int) Math.min(max, Math.max(value, min));
    }

    public static String formatDate(long timestamp) {
        return dateFormatter.format(TimeUtil.getLocalDateTimeOf(timestamp));
    }

    public static String formatTime(LocalTime localTime) {
        return timeFormatter.format(localTime);
    }

    @Deprecated
    public static String getSenderName(String name) {
        if (name.equalsIgnoreCase(CONSOLE_NAME)) {
            return Config.CONSOLE_NAME.get();
        }
        Player player = Players.getPlayer(name);
        if (player != null) {
            return player.getDisplayName();
        }
        return name;
    }

    @Deprecated
    public static String getSenderName(CommandSender sender) {
        if (sender instanceof ConsoleCommandSender) {
            return Config.CONSOLE_NAME.get();
        }
        if (sender instanceof Player player) {
            return player.getDisplayName();
        }
        return sender.getName();
    }

    public static Optional<InetAddress> getInetAddress(Player player) {
        return Optional.ofNullable(player.getAddress()).map(InetSocketAddress::getAddress);
    }
}
