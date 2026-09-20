package su.nightexpress.sunlight.moduleImpl.greetings;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.EventUtils;
import su.nightexpress.nightcore.util.bridge.wrapper.NightComponent;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.greetings.listener.GreetingsListener;
import su.nightexpress.sunlight.moduleImpl.greetings.message.GreetingMessage;
import su.nightexpress.sunlight.moduleImpl.greetings.message.MessageType;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Consumer;

public class GreetingsModule extends Module {

    private final GreetingsSettings settings;

    public GreetingsModule(ModuleDefinition<GreetingsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new GreetingsSettings();
    }

    @Override
    protected void loadModule(FileConfig config) throws ModuleLoadException {
        this.settings.load(config);

        this.addListener(new GreetingsListener(this.plugin, this));
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    protected void registerPermissions(PermissionTree root) {

    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {

    }

    @Override
    protected void registerCommands() {

    }

    public void handleJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        MessageType type = MessageType.JOIN;
        if (this.userManager.getOrFetch(player).isFirstTimeJoined()
                && this.getAvailableMessage(player, MessageType.FIRST_JOIN) != null) {
            type = MessageType.FIRST_JOIN;
        }

        this.setEventMessage(player, type, component -> EventUtils.getAdapter().setJoinMessage(event, component));
    }

    public void handleQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.setEventMessage(player, MessageType.QUIT,
                component -> EventUtils.getAdapter().setQuitMessage(event, component));
    }

    private void setEventMessage(Player player, MessageType type, Consumer<NightComponent> consumer) {
        GreetingMessage message = this.getAvailableMessage(player, type);
        if (message == null) {
            consumer.accept(null);
            return;
        }

        PlaceholderContext context = PlaceholderContext.builder()
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .andThen(CommonPlaceholders.forPlaceholderAPI(player))
                .build();

        NightComponent component = NightMessage.parse(context.apply(message.getMessage()));
        consumer.accept(component);
    }

    public Set<GreetingMessage> getMessages(MessageType type) {
        return Set.copyOf(this.settings.getMessages(type).values());
    }

    public GreetingMessage getJoinMessage(Player player) {
        return this.getAvailableMessage(player, MessageType.JOIN);
    }

    public GreetingMessage getFirstJoinMessage(Player player) {
        return this.getAvailableMessage(player, MessageType.FIRST_JOIN);
    }

    public GreetingMessage getQuitMessage(Player player) {
        return this.getAvailableMessage(player, MessageType.QUIT);
    }

    public GreetingMessage getAvailableMessage(Player player, MessageType type) {
        return this.getMessages(type)
                .stream()
                .filter(message -> message.isApplicable(player))
                .max(Comparator.comparingInt(GreetingMessage::getPriority))
                .orElse(null);
    }
}
