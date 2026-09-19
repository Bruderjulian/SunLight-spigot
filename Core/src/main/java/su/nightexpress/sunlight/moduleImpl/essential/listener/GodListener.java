package su.nightexpress.sunlight.moduleImpl.essential.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;

public class GodListener extends AbstractListener<SunLightPlugin> {

    public GodListener(SunLightPlugin plugin, EssentialModule module) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGodTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player))
            return;
        if (!this.isGod(player))
            return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGodDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        if (!this.isGod(player))
            return;

        event.setCancelled(true);
    }

    private boolean isGod(Player player) {
        return this.plugin.getUserManager().getOrFetch(player).getPropertyOrDefault(EssentialModule.GOD);
    }
}
