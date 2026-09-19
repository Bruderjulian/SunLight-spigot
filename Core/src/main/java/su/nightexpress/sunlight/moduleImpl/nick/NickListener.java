package su.nightexpress.sunlight.moduleImpl.nick;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;

public class NickListener extends AbstractListener<SunLightPlugin> {

    private final NickModule module;

    public NickListener(SunLightPlugin plugin, NickModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.module.applyNickname(player);
    }
}
