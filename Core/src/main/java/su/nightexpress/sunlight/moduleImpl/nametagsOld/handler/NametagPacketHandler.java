package su.nightexpress.sunlight.moduleImpl.nametagsOld.handler;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.sunlight.SunLightPlugin;

import java.util.Collection;

/**
 * Applies nametag formats as client-side scoreboard team packets.
 * <p>
 * Packets never touch the server's main scoreboard, so other plugins (tab
 * lists, minigames, name tags) are unaffected and no empty teams leak on the
 * server.
 */
public abstract class NametagPacketHandler {

    protected final SunLightPlugin plugin;

    protected NametagPacketHandler(@NotNull SunLightPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates (or updates, when the team already exists client-side) the nametag
     * team of {@code target} with the given components. One packet instance is
     * reused for all viewers.
     */
    public abstract void applyTeam(@NotNull Player target, @NotNull String prefix, @NotNull String suffix,
            @NotNull NamedTextColor color, @NotNull Collection<? extends Player> viewers);

    /**
     * Removes the nametag team of {@code target} from the given viewers' clients.
     */
    public abstract void removeTeam(@NotNull Player target, @NotNull Collection<? extends Player> viewers);

    /**
     * Stable, unique team id per player, kept within the 16-char legacy limit.
     */
    protected static @NotNull String teamId(@NotNull Player target) {
        String hex = target.getUniqueId().toString().replace("-", "");
        return "nt" + hex.substring(0, 14);
    }
}