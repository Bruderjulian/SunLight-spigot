package su.nightexpress.sunlight.hook.placeholder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PlaceholderHandler {

    String handle(Player player, String payload);
}
