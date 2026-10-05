package su.nightexpress.sunlight.moduleImpl.playtime.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeModule;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeProperties;
import su.nightexpress.sunlight.user.SunUser;

public class PlaytimeListener extends AbstractListener<SunLightPlugin> {

    private final PlaytimeModule module;

    public PlaytimeListener(SunLightPlugin plugin, PlaytimeModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            SunUser user = this.module.userManager().getOrFetch(player);
            if (user == null) return;
            this.module.startSession(user);
        } catch (Exception ignored) {
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        try {
            SunUser user = this.module.userManager().getOrFetch(player);
            if (user == null) return;
            user.setProperty(PlaytimeProperties.LAST_SEEN, System.currentTimeMillis());
        } catch (Exception ignored) {
        }
    }
}
