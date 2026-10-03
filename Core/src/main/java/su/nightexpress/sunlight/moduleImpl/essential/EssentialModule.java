package su.nightexpress.sunlight.moduleImpl.essential;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.integration.permission.PermissionBridge;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.config.PermissionTree;
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
            this.commandApiRegistry.addProvider(new InvulnerabilityCommandProvider(this));
        }
        if (PermissionBridge.hasProvider()) {
            this.commandApiRegistry.addProvider(new StaffCommandProvider(this));
        }

        this.commandApiRegistry.addProvider(new BroadcastCommandProvider(this));
        this.commandApiRegistry.addProvider(new CondenseCommandProvider(this));
        this.commandApiRegistry.addProvider(new DimensionCommandProvider(this));
        this.commandApiRegistry.addProvider(new DisposalCommandProvider(this));
        this.commandApiRegistry.addProvider(new EnchantCommandsProvider(this));
        this.commandApiRegistry.addProvider(new ExperienceCommandsProvider(this));
        this.commandApiRegistry.addProvider(new FlyCommandProvider(this));
        this.commandApiRegistry.addProvider(new FlySpeedCommandProvider(this));
        this.commandApiRegistry.addProvider(new FoodLevelCommandProvider(this));
        this.commandApiRegistry.addProvider(new GamemodeCommandProvider(this));
        if (this.settings.isGodEnabled()) {
            this.commandApiRegistry.addProvider(new GodCommandProvider(this));
        }
        this.commandApiRegistry.addProvider(new HatCommandProvider(this));
        this.commandApiRegistry.addProvider(new HealthCommandProvider(this));
        this.commandApiRegistry.addProvider(new NearCommandProvider(this));
        this.commandApiRegistry.addProvider(new PlayerInfoCommandProvider(this));
        this.commandApiRegistry.addProvider(new SkullCommandProvider(this));
        this.commandApiRegistry.addProvider(new SmiteCommandProvider(this));
        this.commandApiRegistry.addProvider(new SpeedCommandProvider(this));
        this.commandApiRegistry.addProvider(new SuicideCommandProvider(this));
        this.commandApiRegistry.addProvider(new TeleportCommandsProvider(this));
        this.commandApiRegistry.addProvider(new TimeCommandProvider(this));

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
    protected void registerPermissions(PermissionTree root) {
        root.merge(EssentialPerms.MODULE);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        if (this.settings.isGodEnabled()) {
            registry.register("essential_god_state", (player, payload) -> {
                return CoreLang.STATE_ENABLED_DISALBED.get(this.userManager.getOrFetch(player).getPropertyOrDefault(GOD));
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