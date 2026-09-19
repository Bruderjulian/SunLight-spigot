package su.nightexpress.sunlight.moduleImpl.rtp;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.language.LangAssets;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleContext;
import su.nightexpress.sunlight.moduleImpl.rtp.command.RTPCommandProvider;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPLang;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPPerms;
import su.nightexpress.sunlight.moduleImpl.rtp.config.RTPSettings;
import su.nightexpress.sunlight.moduleImpl.rtp.engine.RTPEngine;
import su.nightexpress.sunlight.teleport.TeleportManager;

public class RTPModule extends Module {

    private final RTPEngine engine;
    private final RTPSettings settings;

    public RTPModule(final ModuleContext context, final TeleportManager teleportManager) {
        super(context);
        this.engine = new RTPEngine(this, teleportManager);
        this.settings = new RTPSettings();
    }

    public RTPEngine getEngine() {
        return this.engine;
    }

    public RTPSettings getSettings() {
        return settings;
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
    }

    @Override
    protected void unloadModule() {
        this.engine.shutdown();
    }

    @Override
    protected void registerPermissions(final PermissionTree root) {
        root.merge(RTPPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("rtp", new RTPCommandProvider(this.plugin, this), this);
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