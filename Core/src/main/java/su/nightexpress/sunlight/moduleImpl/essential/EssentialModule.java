package su.nightexpress.sunlight.moduleImpl.essential;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.integration.permission.PermissionBridge;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.essential.command.*;
import su.nightexpress.sunlight.moduleImpl.essential.listener.GodListener;
import su.nightexpress.sunlight.moduleImpl.essential.listener.InvulnerabilityListener;
import su.nightexpress.sunlight.teleport.TeleportManager;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;

public class EssentialModule extends Module {

    public static final UserProperty<Boolean> GOD = UserProperty.create("god", Boolean.class, false, true);

    private final TeleportManager teleportManager;
    private final EssentialSettings settings;

    public EssentialModule(ModuleDefinition<EssentialModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.teleportManager = plugin.teleportManager();
        this.settings = new EssentialSettings();
    }

    public EssentialSettings settings() {
        return this.settings;
    }

    public TeleportManager teleportManager() {
        return this.teleportManager;
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.plugin.injectLang(EssentialLang.class);
        this.settings.load(config);

        if (this.settings.isInvulnerabilityEnabled()) {
            this.commandRegistry.addProvider(new InvulnerabilityCommandProvider(this));
        }
        if (PermissionBridge.hasProvider()) {
            this.commandRegistry.addProvider(new StaffCommandProvider(this));
        }

        this.commandRegistry.addProvider(new BroadcastCommandProvider(this));
        this.commandRegistry.addProvider(new CondenseCommandProvider(this));
        this.commandRegistry.addProvider(new DimensionCommandProvider(this));
        this.commandRegistry.addProvider(new DisposalCommandProvider(this));
        this.commandRegistry.addProvider(new EnchantCommandsProvider(this));
        this.commandRegistry.addProvider(new ExperienceCommandsProvider(this));
        this.commandRegistry.addProvider(new FlyCommandProvider(this));
        this.commandRegistry.addProvider(new FlySpeedCommandProvider(this));
        this.commandRegistry.addProvider(new FoodLevelCommandProvider(this));
        this.commandRegistry.addProvider(new GamemodeCommandProvider(this));
        if (this.settings.isGodEnabled()) {
            this.commandRegistry.addProvider(new GodCommandProvider(this));
        }
        this.commandRegistry.addProvider(new HatCommandProvider(this));
        this.commandRegistry.addProvider(new HealthCommandProvider(this));
        this.commandRegistry.addProvider(new NearCommandProvider(this));
        this.commandRegistry.addProvider(new PlayerInfoCommandProvider(this));
        this.commandRegistry.addProvider(new SkullCommandProvider(this));
        this.commandRegistry.addProvider(new SmiteCommandProvider(this));
        this.commandRegistry.addProvider(new SpeedCommandProvider(this));
        this.commandRegistry.addProvider(new SuicideCommandProvider(this));
        this.commandRegistry.addProvider(new TeleportCommandsProvider(this));
        this.commandRegistry.addProvider(new TimeCommandProvider(this));

        if (this.settings.isGodEnabled()) {
            UserPropertyRegistry.register(GOD);
            this.addListener(new GodListener(this.plugin, this));
        }

        if (this.settings.isInvulnerabilityEnabled()) {
            this.addListener(new InvulnerabilityListener(this.plugin, this, this.settings));
        }
    }

    @Override
    protected void unloadModule() {

    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        if (this.settings.isGodEnabled()) {
            registry.register("essential_god_state", (player, payload) -> {
                return CoreLang.STATE_ENABLED_DISALBED
                        .get(this.userManager.getOrFetch(player).getPropertyOrDefault(GOD));
            });

            registry.register("essential_god_bool", (player, payload) -> {
                return String.valueOf(this.userManager.getOrFetch(player).getPropertyOrDefault(GOD));
            });
        }
        if (this.settings.isInvulnerabilityEnabled()) {
            registry.register("essential_invulnerability_state", (player, payload) -> {
                return CoreLang.STATE_ENABLED_DISALBED.get(player.isInvulnerable());
            });

            registry.register("essential_invulnerability_bool", (player, payload) -> {
                return String.valueOf(player.isInvulnerable());
            });
        }
    }
}