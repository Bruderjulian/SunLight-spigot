package su.nightexpress.sunlight.moduleImpl.rtp;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.language.LangAssets;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.RtpProvider;
import su.nightexpress.sunlight.api.provider.dto.LastRtpHandle;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.rtp.command.RTPCommandProvider;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPLang;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPSettings;
import su.nightexpress.sunlight.moduleImpl.rtp.engine.RTPEngine;

public class RTPModule extends Module implements RtpProvider {

    private final RTPEngine engine;
    private final RTPSettings settings;

    public RTPModule(final ModuleDefinition<RTPModule> definition, final SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new RTPSettings();
        this.engine = new RTPEngine(this, plugin.teleportManager());
    }

    public RTPEngine getEngine() {
        return this.engine;
    }

    public RTPSettings getSettings() {
        return settings;
    }

    @Override
    public boolean teleportToRandomPlace(@NotNull final Player player, @Nullable final World world) {
        return this.engine.teleportToRandomPlace(player, world);
    }

    @Override
    public LastRtpHandle getLastRtp(@NotNull final Player player) {
        return this.engine.getLastRTP(player).map(data -> {
            final Location origin = data.origin();
            final Location destination = data.destination();

            return new LastRtpHandle(
                    origin.getWorld() != null ? origin.getWorld().getName() : "null",
                    origin.getX(),
                    origin.getZ(),
                    destination.getWorld() != null ? destination.getWorld().getName() : "null",
                    destination.getX(),
                    destination.getZ(),
                    data.distance());
        }).orElse(null);
    }

    @Override
    public boolean isProtected(@NotNull final Location location) {
        return this.engine.isProtected(location);
    }

    public void reload() {
        final FileConfig config = this.getConfig();
        this.settings.load(config);
        this.engine.load();
        this.settings.load(config);
        config.saveChanges();
        this.plugin.injectLang(RTPLang.class);
    }

    @Override
    protected void loadModule(final FileConfig config) {
        this.engine.load();
        this.plugin.injectLang(RTPLang.class);
        this.commandRegistry.addProvider(new RTPCommandProvider(this));
    }

    @Override
    protected void unloadModule() {
        this.engine.shutdown();
    }

    @Override
    public void registerPlaceholders(final PlaceholderRegistry registry) {
        registry.register("rtp_last_x", (player, payload) -> this.engine.getLastRTP(player)
                .map(data -> NumberUtil.format(data.destination().getX()))
                .orElse(CoreLang.OTHER_NONE.text()));
        registry.register("rtp_last_y", (player, payload) -> this.engine.getLastRTP(player)
                .map(data -> NumberUtil.format(data.destination().getY()))
                .orElse(CoreLang.OTHER_NONE.text()));
        registry.register("rtp_last_z", (player, payload) -> this.engine.getLastRTP(player)
                .map(data -> NumberUtil.format(data.destination().getZ()))
                .orElse(CoreLang.OTHER_NONE.text()));
        registry.register("rtp_last_world", (player, payload) -> this.engine.getLastRTP(player)
                .map(data -> data.destination().getWorld() != null
                        ? LangAssets.get(data.destination().getWorld())
                        : CoreLang.OTHER_NONE.text())
                .orElse(CoreLang.OTHER_NONE.text()));
        registry.register("rtp_last_distance", (player, payload) -> this.engine.getLastRTP(player)
                .map(data -> NumberUtil.format(data.distance()))
                .orElse(CoreLang.OTHER_NONE.text()));
    }
}