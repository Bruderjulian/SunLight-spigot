package su.nightexpress.sunlight.moduleImpl.friends.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;

/**
 * Login and quit handling.
 * <p>
 * Quit clears the player's request queue. That is not cosmetic: a request whose target has left
 * would otherwise still be listed for the sender, who could not act on it until they came back.
 */
public class FriendsListener extends AbstractListener<SunLightPlugin> {

    private final FriendsModule module;

    public FriendsListener(SunLightPlugin plugin, FriendsModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // Deferred a tick: the friends cache may still be loading, and the notification reads it.
        this.plugin.runTask(task -> {
            if (event.getPlayer().isOnline()) {
                this.module.notifyLogin(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.module.notifyQuit(event.getPlayer());
        this.module.clearRequests(event.getPlayer().getUniqueId());
    }
}