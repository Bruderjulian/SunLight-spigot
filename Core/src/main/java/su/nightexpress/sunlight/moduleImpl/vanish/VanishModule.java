package su.nightexpress.sunlight.moduleImpl.vanish;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.bridge.wrapper.NightComponent;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.sunlight.api.event.PlayerVanishToggleEvent;
import su.nightexpress.sunlight.api.provider.VanishProvider;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.greetings.GreetingsModule;
import su.nightexpress.sunlight.moduleImpl.vanish.command.VanishCommand;
import su.nightexpress.sunlight.moduleImpl.vanish.config.VanishConfig;
import su.nightexpress.sunlight.moduleImpl.vanish.config.VanishLang;
import su.nightexpress.sunlight.moduleImpl.vanish.config.VanishPerms;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.Utils;

public class VanishModule extends Module implements VanishProvider {

    public static final UserProperty<Boolean> VANISH = UserProperty.create("vanish", Boolean.class, false, true);

    private BossBar vanishIndicator;

    public VanishModule(final ModuleDefinition<VanishModule> definition, final SunLightPlugin plugin) {
        super(definition, plugin);
    }

    @Override
    protected void loadModule(final FileConfig config) {
        config.initializeOptions(VanishConfig.class);
        this.plugin.injectLang(VanishLang.class);
        UserPropertyRegistry.register(VANISH);

        this.addListener(new VanishListener(this.plugin, this));
        this.commandRegistry.addProvider(new VanishCommand(this));

        if (VanishConfig.BAR_INDICATOR_ENABLED.get()) {
            final String title = VanishConfig.BAR_INDICATOR_VANISHED_TITLE.get();
            final BarColor color = VanishConfig.BAR_INDICATOR_VANISHED_COLOR.get();
            final BarStyle style = VanishConfig.BAR_INDICATOR_VANISHED_STYLE.get();

            this.vanishIndicator = this.plugin.getServer().createBossBar(NightMessage.asLegacy(title), color, style);
        }

        this.plugin.runTask(this::updateOnlinePlayers);
    }

    @Override
    protected void unloadModule() {
        Utils.onlinePlayers().forEach(player -> this.vanish(player, false));

        if (this.vanishIndicator != null) {
            this.vanishIndicator.removeAll();
            this.vanishIndicator = null;
        }
    }

    @Override
    public void registerPlaceholders(final PlaceholderRegistry registry) {
        registry.register("vanish_state", (player, payload) -> {
            return CoreLang.STATE_YES_NO.get(this.userManager.getOrFetch(player).getPropertyOrDefault(VANISH));
        });
    }

    private void updateOnlinePlayers() {
        Utils.onlinePlayers().forEach(player -> {
            if (!this.isVanished(player))
                return;

            this.vanish(player, true);
        });
    }

    @Override
    public boolean isVanished(final Player player) {
        final SunUser user = this.plugin.userManager().getOrFetch(player);
        return user.getPropertyOrDefault(VANISH);
    }

    public void vanish(final Player player, boolean isVanished) {
        final PlayerVanishToggleEvent event = new PlayerVanishToggleEvent(player, isVanished);
        this.plugin.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;
        isVanished = event.isVanished();
        for (final Player other : this.plugin.getServer().getOnlinePlayers()) {
            if (isVanished) {
                if (!other.hasPermission(VanishPerms.BYPASS_SEE)) {
                    other.hidePlayer(this.plugin, player);
                }
                if (this.vanishIndicator != null)
                    this.vanishIndicator.addPlayer(player);
            } else {
                other.showPlayer(this.plugin, player);
                if (this.vanishIndicator != null)
                    this.vanishIndicator.removePlayer(player);
            }
        }
        this.setMetadata(player, isVanished);
    }

    private void setMetadata(final Player player, final boolean isVanished) {
        final String key = VanishConfig.METADATA_KEY.get();
        if (key == null || key.isBlank())
            return;

        if (isVanished) {
            player.setMetadata(key, new FixedMetadataValue(this.plugin, true));
        } else {
            player.removeMetadata(key, this.plugin);
        }
    }

    /**
     * Broadcasts a fake join/leave message for a vanish toggle. Only players
     * without the see-bypass permission receive it; staff who can see vanished
     * players are not lied to.
     *
     * @param player the toggled player
     * @param joined true for a fake join (unvanish), false for fake leave
     */
    public void broadcastFake(final Player player, final boolean joined) {
        if (!VanishConfig.FAKE_MESSAGES_ENABLED.get())
            return;
        if (joined && !VanishConfig.FAKE_MESSAGE_ON_UNVANISH.get())
            return;
        if (!joined && !VanishConfig.FAKE_MESSAGE_ON_VANISH.get())
            return;

        final NightComponent component = this.resolveFakeMessage(player, joined);
        if (component == null)
            return;

        for (final Player recipient : this.plugin.getServer().getOnlinePlayers()) {
            if (recipient.hasPermission(VanishPerms.BYPASS_SEE))
                continue;
            Players.sendMessage(recipient, component);
        }
    }

    private NightComponent resolveFakeMessage(final Player player, final boolean joined) {
        // Prefer the server's greetings so fakes match real join/leave style.
        final NightComponent greeting = this.plugin.moduleManager().getByType(GreetingsModule.class)
                .map(module -> joined ? module.getJoinMessage(player) : module.getQuitMessage(player))
                .orElse(null);
        if (greeting != null) {
            final PlaceholderContext context = PlaceholderContext.builder()
                    .with(CommonPlaceholders.PLAYER.resolver(player))
                    .andThen(CommonPlaceholders.forPlaceholderAPI(player))
                    .build();
            return NightMessage.parse(context.apply(greeting.toLegacy()));
        }
        // Fallback when greetings are absent or have no matching message.
        return NightMessage.parse(joined
                ? VanishLang.FAKE_JOIN.text().replace(su.nightexpress.sunlight.SLPlaceholders.PLAYER_DISPLAY_NAME,
                        player.getDisplayName())
                : VanishLang.FAKE_LEAVE.text().replace(su.nightexpress.sunlight.SLPlaceholders.PLAYER_DISPLAY_NAME,
                        player.getDisplayName()));
    }
}
