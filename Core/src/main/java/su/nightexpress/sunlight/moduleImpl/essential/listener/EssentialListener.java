package su.nightexpress.sunlight.moduleImpl.essential.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;

public class EssentialListener extends AbstractListener<SunLightPlugin> {

    private final EssentialModule module;

    public EssentialListener(SunLightPlugin plugin, EssentialModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.module.updatePlayerName(player);
    }
}
