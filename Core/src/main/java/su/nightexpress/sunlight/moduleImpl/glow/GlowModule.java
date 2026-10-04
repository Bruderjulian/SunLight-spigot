package su.nightexpress.sunlight.moduleImpl.glow;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.NamedTextColor;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.bukkit.NightTask;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.api.provider.GlowProvider;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.exception.ModuleLoadException;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.glow.command.GlowCommandProvider;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowLang;
import su.nightexpress.sunlight.moduleImpl.glow.menu.GlowMenu;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;

/**
 * Applies the vanilla glow outline and contributes its colour to the nametag.
 * <p>
 * The glowing flag itself is plain Bukkit API ({@code Player#setGlowing}), so
 * no packet
 * library is needed any more. The colour is handed to the nametags module,
 * which appends it
 * as the last colour token of the nameplate, so the two features never fight
 * over teams.
 */
public class GlowModule extends Module implements GlowProvider {

    private final GlowSettings settings;
    private final GlowHandler handler;
    private GlowMenu menu;

    public GlowModule(final ModuleDefinition<GlowModule> definition, final SunLightPlugin plugin) {
        super(definition, plugin);
        this.settings = new GlowSettings();
        this.handler = new GlowHandler(this);
    }

    @Override
    protected void loadModule(final FileConfig config) throws ModuleLoadException {
        this.settings.load(config);
        this.plugin.injectLang(GlowLang.class);
        UserPropertyRegistry.register(GlowHandler.PROPERTY_GLOW);
        UserPropertyRegistry.register(GlowHandler.PROPERTY_GLOW_ENABLED);
        UserPropertyRegistry.register(GlowHandler.PROPERTY_GLOW_PRESETS);

        this.addListener(new GlowListener(this.plugin, this));

        this.menu = new GlowMenu(this.plugin, this);
        this.menu.load(this.plugin, FileConfig.load(this.getLocalUIPath(), "glow.yml"));

        handler.init();
        this.commandRegistry.addProvider(new GlowCommandProvider(this));

    }

    @Override
    protected void unloadModule() {
        handler.shutdown();
        this.menu = null;
    }

    @Override
    protected void registerPermissions(final PermissionTree root) {
    }

    @Override
    public void registerPlaceholders(final PlaceholderRegistry registry) {
        registry.register("glow_state", (player, payload) -> CoreLang.STATE_YES_NO.get(this.isGlowEnabled(player)));
        registry.register("glow_color", (player, payload) -> {
            final GlowEffect effect = handler.getEffectiveEffect(player);
            return effect == null ? "" : effect.getName();
        });
    }

    public GlowSettings settings() {
        return this.settings;
    }

    public GlowHandler handler() {
        return this.handler;
    }

    protected void addTask(final Runnable runnable, final long interval) {
        this.addTask(NightTask.create(plugin, runnable, interval));
    }

    public void openGlowMenu(final Player player) {
        if (this.menu == null) {
            this.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_MENU, player);
            return;
        }
        this.menu.open(player);
    }

    @Override
    public boolean isGlowEnabled(final Player player) {
        return handler.isGlowEnabled(player);
    }

    @Override
    public void setEffect(final Player player, final NamedTextColor color) {
        handler.setSingleColorGlow(player, color);
    }

    @Override
    public void clearGlow(final Player player) {
        handler.setEffect(player, null);
    }

    @Override
    public String getEffectId(Player player) {
        return handler.getEffectiveEffectId(player);
    }

    @Override
    public String getGlowColor(final Player player) {
        final NamedTextColor color = handler.getCurrentColor(player);
        return color == null ? null : color.toString();
    }

}
