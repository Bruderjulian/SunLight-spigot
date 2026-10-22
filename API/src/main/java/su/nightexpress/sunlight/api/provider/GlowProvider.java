package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.NamedTextColor;

public interface GlowProvider {

    boolean isGlowEnabled(Player player);

    String getEffectId(Player player);

    /**
     * The colour of the phase currently shown, as a vanilla colour name.
     *
     * @return null when the player has no active glow.
     */
    String getGlowColor(Player player);

    void setEffect(Player player, NamedTextColor color);

    void clearGlow(Player player);

}
