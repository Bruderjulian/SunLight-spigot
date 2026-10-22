package su.nightexpress.sunlight.moduleImpl.reports.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.bans.event.PlayerPunishEvent;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;

/**
 * Only registered when the Bans module is enabled, so there is no dead listener when it is off.
 * <p>
 * The dependency is one-directional: Bans knows nothing about reports, and this is the only place
 * the two meet.
 */
public class ReportsPunishListener extends AbstractListener<SunLightPlugin> {

    private final ReportsModule module;

    public ReportsPunishListener(ReportsModule module) {
        super(module.plugin());
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPunish(PlayerPunishEvent event) {
        this.module.handlePunish(event);
    }
}
