package su.nightexpress.sunlight.moduleImpl.greetings.listener;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.nightcore.util.EventUtils;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.bridge.wrapper.NightComponent;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.dto.GreetingMessageType;
import su.nightexpress.sunlight.moduleImpl.greetings.GreetingsModule;

public class GreetingsListener extends AbstractListener<SunLightPlugin> {

    private final GreetingsModule module;

    public GreetingsListener(final SunLightPlugin plugin, final GreetingsModule module) {
        super(plugin);
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(final PlayerJoinEvent event) {
        boolean isFirstJoin;
        if (module.userManager().getOrFetch(event.getPlayer()).isFirstTimeJoined()
                && module.getAvailableMessage(event.getPlayer(), GreetingMessageType.FIRST_JOIN) != null) {
            isFirstJoin = true;
        } else {
            isFirstJoin = false;
        }

        NightComponent message = module.getMessage(event.getPlayer(),
                isFirstJoin ? GreetingMessageType.FIRST_JOIN : GreetingMessageType.JOIN);
        if (message == null) {
            if (!isFirstJoin) {
                return;
            }
            message = module.getMessage(event.getPlayer(), GreetingMessageType.JOIN);
            if (message == null) {
                return;
            }
        }
        EventUtils.getAdapter().setJoinMessage(event, message);

        Bukkit.getScheduler().runTaskAsynchronously(module.plugin(), () -> {
            if (!module.settings.isJoinCommandsEnabled())
                return;

            final List<String> commands = isFirstJoin ? module.settings.getJoinCommandsFirst()
                    : module.settings.getJoinCommandsDefault();
            Players.dispatchCommands(event.getPlayer(), commands);
        });

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(final PlayerQuitEvent event) {
        final NightComponent message = module.getMessage(event.getPlayer(), GreetingMessageType.QUIT);
        if (message == null) {
            return;
        }
        EventUtils.getAdapter().setQuitMessage(event, message);
    }
}
