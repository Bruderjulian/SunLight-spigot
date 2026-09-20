package su.nightexpress.sunlight.moduleImpl.glow.handler;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.sunlight.SunLightPlugin;

import java.util.Collection;

/**
 * Sends glow color data as client-side scoreboard team packets.
 * <p>
 * Unlike Bukkit scoreboard teams, packets never touch the server's main
 * scoreboard, so other plugins (tab lists, minigames, name tags) are unaffected
 * and no empty teams leak on the server. The glowing flag itself is still
 * managed via {@link Player#setGlowing(boolean)}; only the color channel
 * goes through packets.
 */
public abstract class GlowPacketHandler {

    protected final SunLightPlugin plugin;

    protected GlowPacketHandler(@NotNull SunLightPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates (or updates, when the team already exists client-side) the glow team
     * of {@code target} with the given color. One packet instance is reused for all viewers.
     */
    public abstract void sendGlow(@NotNull Player target,
                                  @NotNull NamedTextColor color,
                                  @NotNull Collection<? extends Player> viewers);

    /**
     * Removes the glow team of {@code target} from the given viewers' clients.
     */
    public abstract void removeGlow(@NotNull Player target,
                                    @NotNull Collection<? extends Player> viewers);

    /**
     * Stable, unique team id per glowing player, kept within the 16-char legacy limit.
     */
    protected static @NotNull String teamId(@NotNull Player target) {
        String hex = target.getUniqueId().toString().replace("-", "");
        return "sg" + hex.substring(0, 14);
    }
}
