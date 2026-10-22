package su.nightexpress.sunlight.moduleImpl.socials.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.event.PlayerReportEvent;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.socials.SocialsModule;

public class SocialsListener extends AbstractListener<SunLightPlugin> {

    private final SocialsModule module;

    public SocialsListener(SunLightPlugin plugin, SocialsModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        this.module.relayJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.module.relayQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        this.module.relayDeath(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        String key = event.getAdvancement().getKey().getKey();
        if (key.contains("recipes/")) return;
        this.module.relayAdvancement(event.getPlayer(), key.replace('/', ' ').replace('_', ' '));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (message.isBlank()) return;
        this.module.relayChat(player, message);
    }

    /**
     * The listener lives here rather than in the reports module so that reports has no knowledge of
     * Discord at all. It is also the first subscriber to any of SunLight's custom module events.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReport(PlayerReportEvent event) {
        Report report = event.getReport();
        this.module.relayReport(
            report.getId().toString(),
            report.getReporterName(),
            report.getTargetName(),
            report.getCategoryDisplay(),
            report.getDetails(),
            TimeFormats.formatDateTime(report.getCreateDate())
        );
    }
}
