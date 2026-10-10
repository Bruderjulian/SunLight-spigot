package su.nightexpress.sunlight.moduleImpl.greetings;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.bridge.wrapper.NightComponent;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.GreetingsProvider;
import su.nightexpress.sunlight.api.provider.dto.GreetingMessageType;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.greetings.listener.GreetingsListener;
import su.nightexpress.sunlight.moduleImpl.greetings.message.GreetingMessage;

import java.util.Collection;
import java.util.Comparator;

public class GreetingsModule extends Module implements GreetingsProvider {

    public final GreetingsSettings settings;

    public GreetingsModule(final ModuleDefinition<GreetingsModule> definition, final SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new GreetingsSettings();
    }

    @Override
    protected void loadModule(final FileConfig config) throws ModuleLoadException {
        this.settings.load(config);
        this.addListener(new GreetingsListener(this.plugin, this));
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    public void registerPlaceholders(final PlaceholderRegistry registry) {

    }

    private NightComponent resolve(final Player player, final GreetingMessage message) {
        final PlaceholderContext context = PlaceholderContext.builder()
                .with(CommonPlaceholders.PLAYER.resolver(player))
                .andThen(CommonPlaceholders.forPlaceholderAPI(player))
                .build();

        return NightMessage.parse(context.apply(message.getMessage()));
    }

    public Collection<GreetingMessage> getMessages(final GreetingMessageType type) {
        return this.settings.getMessages(type).values();
    }

    public GreetingMessage getAvailableMessage(final Player player, final GreetingMessageType type) {
        return this.settings.getMessages(type)
                .values()
                .stream()
                .filter(message -> message.isApplicable(player))
                .max(Comparator.comparingInt(GreetingMessage::getPriority))
                .orElse(null);
    }

    @Override
    public int getMessageCount(final GreetingMessageType type) {
        return type == null ? 0 : this.settings.getMessages(type).size();
    }

    @Override
    public boolean hasMessage(final Player player, final GreetingMessageType type) {
        return this.settings.getMessages(type)
                .values()
                .stream().anyMatch(message -> message.isApplicable(player));
    }

    @Override
    public NightComponent getMessage(final Player player, final GreetingMessageType type) {
        if (type == null) {
            return null;
        }
        final GreetingMessage message = this.getAvailableMessage(player, type);
        return message == null ? null : this.resolve(player, message);
    }

    @Override
    public NightComponent getJoinMessage(final Player player) {
        return this.getMessage(player, GreetingMessageType.JOIN);
    }

    @Override
    public NightComponent getFirstJoinMessage(final Player player) {
        return this.getMessage(player, GreetingMessageType.FIRST_JOIN);
    }

    @Override
    public NightComponent getQuitMessage(final Player player) {
        return this.getMessage(player, GreetingMessageType.QUIT);
    }
}
