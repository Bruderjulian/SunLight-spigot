package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.NamedTextColor;

public interface GlowProvider {

    boolean isGlowEnabled(Player player);

    String getEffectId(Player player);

    void setEffect(Player player, NamedTextColor color);

    void clearGlow(Player player);

}
