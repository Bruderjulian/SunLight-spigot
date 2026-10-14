package su.nightexpress.sunlight.moduleImpl.voting.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;

public class VotingListener extends AbstractListener<SunLightPlugin> {

    private final VotingModule module;

    public VotingListener(final SunLightPlugin plugin, final VotingModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        // Delayed by one sweep tick so the boot load cannot greet a player with half a list.
        // The module itself guards on dataLoaded, so this is only ordering, not correctness.
        this.plugin.runTask(task -> this.module.nudgeOnJoin(player));
    }
}
