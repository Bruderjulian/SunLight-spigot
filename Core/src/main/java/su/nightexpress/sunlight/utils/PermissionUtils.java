package su.nightexpress.sunlight.utils;

import org.bukkit.command.CommandSender;

public class PermissionUtils {

    private PermissionUtils() {
    }

    /**
     * Builds a child node of the given node, e.g. {@code node("sunlight.kits.kit", "starter")}
     * returns {@code sunlight.kits.kit.starter}.
     */
    public static String node(final String node, final String child) {
        return node + "." + child;
    }

    /**
     * Grants access to a single child node, or to every child at once via the
     * {@code <node>.*} wildcard.
     */
    public static boolean hasChildAccess(final CommandSender sender, final String node, final String child) {
        return sender.hasPermission(node + ".*") || sender.hasPermission(node(node, child));
    }
}
