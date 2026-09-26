package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

public interface LinksProvider {

    /**
     * @return Every enabled link, keyed by ID. Command-only links map to an empty string.
     */
    Map<String, String> getLinkMap();

    /**
     * @return The IDs of every configured link, including disabled ones.
     */
    Set<String> getLinkIds();

    /**
     * Runs the link's command (if any) and then sends the player its URL, exactly as clicking the link
     * in the menu or running its command would.
     *
     * @return {@code false} when no link with that ID exists.
     */
    boolean activateLink(Player player, String id);
}
