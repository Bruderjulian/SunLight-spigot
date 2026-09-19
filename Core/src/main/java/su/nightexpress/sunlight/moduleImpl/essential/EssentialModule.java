package su.nightexpress.sunlight.moduleImpl.essential;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.integration.permission.PermissionBridge;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleContext;
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

    public EssentialModule(ModuleContext context, TeleportManager teleportManager) {
        super(context);
        this.teleportManager = teleportManager;
        this.settings = new EssentialSettings();
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.plugin.injectLang(EssentialLang.class);
        this.settings.load(config);
        this.registerCommands();

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

    protected void registerCommands() {
        if (this.settings.isInvulnerabilityEnabled()) {
            this.commandRegistry.addProvider("ess-invulnerability",
                    new InvulnerabilityCommandProvider(this.plugin, this, this.settings, this.userManager), this);
        }
        if (PermissionBridge.hasProvider()) {
            this.commandRegistry.addProvider("staff", new StaffCommandProvider(this.plugin, this, this.settings), this);
        }

        this.commandRegistry.addProvider("broadcast",
                new BroadcastCommandProvider(this.plugin, this.settings.broadcastFormat.get()), this);
        this.commandRegistry.addProvider("condense", new CondenseCommandProvider(this.plugin, this), this);
        this.commandRegistry.addProvider("dimension",
                new DimensionCommandProvider(this.plugin, this, this.userManager, this.teleportManager), this);
        this.commandRegistry.addProvider("disposal", new DisposalCommandProvider(this.plugin, this, this.settings),
                this);
        this.commandRegistry.addProvider("enchant", new EnchantCommandsProvider(this.plugin, this, this.userManager),
                this);
        this.commandRegistry.addProvider("experience",
                new ExperienceCommandsProvider(this.plugin, this, this.userManager), this);
        this.commandRegistry.addProvider("fly", new FlyCommandProvider(this.plugin, this, this.userManager), this);
        this.commandRegistry.addProvider("flyspeed", new FlySpeedCommandProvider(this.plugin, this, this.userManager),
                this);
        this.commandRegistry.addProvider("foodlevel",
                new FoodLevelCommandProvider(this.plugin, this, this.settings, this.userManager), this);
        this.commandRegistry.addProvider("gamemode", new GamemodeCommandProvider(this.plugin, this, this.userManager),
                this);
        if (this.settings.isGodEnabled()) {
            this.commandRegistry.addProvider("god", new GodCommandProvider(this.plugin, this, this.userManager), this);
        }
        this.commandRegistry.addProvider("hat", new HatCommandProvider(this.plugin, this), this);
        this.commandRegistry.addProvider("health",
                new HealthCommandProvider(this.plugin, this, this.settings, this.userManager), this);
        this.commandRegistry.addProvider("near",
                new NearCommandProvider(this.plugin, this, this.settings, this.userManager), this);
        this.commandRegistry.addProvider("playerinfo",
                new PlayerInfoCommandProvider(this.plugin, this, this.settings, this.userManager), this);
        this.commandRegistry.addProvider("skull", new SkullCommandProvider(this.plugin, this), this);
        this.commandRegistry.addProvider("smite", new SmiteCommandProvider(this.plugin, this), this);
        this.commandRegistry.addProvider("speed", new SpeedCommandProvider(this.plugin, this, this.userManager), this);
        this.commandRegistry.addProvider("suicide", new SuicideCommandProvider(this.plugin, this), this);
        this.commandRegistry.addProvider("teleport",
                new TeleportCommandsProvider(this.plugin, this, this.userManager, this.teleportManager), this);
        this.commandRegistry.addProvider("time", new TimeCommandProvider(this.plugin, this, this.settings), this);
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
