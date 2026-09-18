package su.nightexpress.sunlight.module.vanish;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.vanish.config.VanishPerms;

public class VanishListener extends AbstractListener<SunLightPlugin> {

    private final VanishModule module;

    public VanishListener(SunLightPlugin plugin, VanishModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.module.isVanished(player)) {
            this.module.vanish(player, true);
        }

        if (player.hasPermission(VanishPerms.BYPASS_SEE))
            return;

        for (Player vanished : plugin.getServer().getOnlinePlayers()) {
            if (vanished == player || !this.module.isVanished(vanished))
                continue;

            player.hidePlayer(plugin, vanished);
        }
    }
}
