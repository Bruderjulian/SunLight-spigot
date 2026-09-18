package su.nightexpress.sunlight.hook.placeholder;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlaceholderHandler {

    String handle(Player player, String payload);
}
