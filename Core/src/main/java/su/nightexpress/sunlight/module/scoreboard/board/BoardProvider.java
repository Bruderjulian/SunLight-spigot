package su.nightexpress.sunlight.module.scoreboard.board;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;

public interface BoardProvider {

    Board create(Player player, PlaceholderContext context, BoardDefinition config);
}
