package su.nightexpress.sunlight.api.provider;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

public interface LinksProvider {

    /**
     * @return Every enabled and actionable link, keyed by ID. Command-only links map to an empty string.
     * Misconfigured links (no URL and no command) are excluded until an admin repairs them.
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

    /**
     * @return Total recorded clicks across all links.
     */
    default long getTotalClicks() {
        return 0L;
    }

    /**
     * @return Clicks recorded for the link, or -1 when no link with that ID exists.
     */
    default long getClicks(String id) {
        return -1L;
    }
}
