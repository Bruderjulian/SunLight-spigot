package su.nightexpress.sunlight.moduleImpl.chat.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;

public class ChatListener extends AbstractListener<SunLightPlugin> {

    private final ChatModule module;

    public ChatListener(SunLightPlugin plugin, ChatModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.module.autoJoinChannels(player);
        this.module.deliverMails(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.module.removeFromAllChannels(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChatProcessCommand(PlayerCommandPreprocessEvent event) {
        this.module.handleCommandEvent(event);
    }
}
