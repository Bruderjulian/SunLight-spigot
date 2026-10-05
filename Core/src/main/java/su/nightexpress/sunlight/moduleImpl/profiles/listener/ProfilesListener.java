package su.nightexpress.sunlight.moduleImpl.profiles.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfilesModule;
import su.nightexpress.sunlight.user.SunUser;

public class ProfilesListener extends AbstractListener<SunLightPlugin> {

    private final ProfilesModule module;

    public ProfilesListener(SunLightPlugin plugin, ProfilesModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            SunUser user = this.plugin.userManager().getOrFetch(player);
            this.module.getManager().ensureLoaded(player, user);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        try {
            this.module.getWarmup().cancel(player, true);
            SunUser user = this.plugin.userManager().getOrFetch(player);
            this.module.getManager().saveCurrent(player, user);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        try {
            this.module.getWarmup().cancel(player, true);
            SunUser user = this.plugin.userManager().getOrFetch(player);
            this.module.getManager().saveCurrent(player, user);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        try {
            this.module.getWarmup().cancelOnMove(event.getPlayer());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        try {
            this.module.getWarmup().cancelOnDamage(player);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}
