package su.nightexpress.sunlight;

import java.util.Optional;

import su.nightexpress.nightcore.NightCorePlugin;
import su.nightexpress.nightcore.NightPlugin;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.command.NightCommand;
import su.nightexpress.nightcore.config.PluginDetails;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.Version;
import su.nightexpress.sunlight.api.SunlightAPI;
import su.nightexpress.sunlight.api.provider.AfkProvider;
import su.nightexpress.sunlight.api.provider.FreezeProvider;
import su.nightexpress.sunlight.api.provider.GlowProvider;
import su.nightexpress.sunlight.api.provider.NametagsProvider;
import su.nightexpress.sunlight.api.provider.NickProvider;
import su.nightexpress.sunlight.api.provider.SocialsProvider;
import su.nightexpress.sunlight.api.provider.VanishProvider;
import su.nightexpress.sunlight.command.CommandRegistry;
import su.nightexpress.sunlight.config.Config;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;
import su.nightexpress.sunlight.data.DataHandler;
import su.nightexpress.sunlight.hook.impl.PlaceholderHook;
import su.nightexpress.sunlight.module.LoadCondition;
import su.nightexpress.sunlight.module.ModuleManager;
import su.nightexpress.sunlight.moduleImpl.afk.AfkModule;
import su.nightexpress.sunlight.moduleImpl.backlocation.BackLocationModule;
import su.nightexpress.sunlight.moduleImpl.bans.BansModule;
import su.nightexpress.sunlight.moduleImpl.chat.ChatModule;
import su.nightexpress.sunlight.moduleImpl.deathmessages.DeathMessagesModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.extras.ExtrasModule;
import su.nightexpress.sunlight.moduleImpl.freeze.FreezeModule;
import su.nightexpress.sunlight.moduleImpl.glow.GlowModule;
import su.nightexpress.sunlight.moduleImpl.greetings.GreetingsModule;
import su.nightexpress.sunlight.moduleImpl.homes.HomesModule;
import su.nightexpress.sunlight.moduleImpl.inventories.InventoriesModule;
import su.nightexpress.sunlight.moduleImpl.kits.KitsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.PhantomsModule;
import su.nightexpress.sunlight.moduleImpl.nick.NickModule;
import su.nightexpress.sunlight.moduleImpl.playerwarps.PlayerWarpsModule;
import su.nightexpress.sunlight.moduleImpl.ptp.PTPModule;
import su.nightexpress.sunlight.moduleImpl.rtp.RTPModule;
import su.nightexpress.sunlight.moduleImpl.scheduler.SchedulerModule;
import su.nightexpress.sunlight.moduleImpl.socials.SocialsModule;
import su.nightexpress.sunlight.moduleImpl.spawns.SpawnsModule;
import su.nightexpress.sunlight.moduleImpl.texts.TextsModule;
import su.nightexpress.sunlight.moduleImpl.vanish.VanishModule;
import su.nightexpress.sunlight.moduleImpl.warmups.WarmupsModule;
import su.nightexpress.sunlight.moduleImpl.warps.WarpsModule;
import su.nightexpress.sunlight.nms.SunNMS;
import su.nightexpress.sunlight.nms.mc_1_21_11.MC_1_21_11;
import su.nightexpress.sunlight.nms.v26p1.NMSv26p1;
import su.nightexpress.sunlight.teleport.TeleportManager;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.Utils;

public class SunLightPlugin extends NightPlugin implements SunlightAPI {

    private static SunlightAPI api;

    private CommandRegistry commandRegistry;
    private ModuleManager moduleManager;

    private DataHandler dataHandler;
    private UserManager userManager;

    private TeleportManager teleportManager;

    private SunNMS sunNMS;

    public static SunlightAPI getAPI() {
        return api;
    }

    public SunLightPlugin() {
        api = this;
    }

    @Override

    protected PluginDetails getDefaultDetails() {
        return PluginDetails.create("SunLight", new String[] { "sunlight", "sl" })
                .setConfigClass(Config.class);
    }

    @Override
    protected void addRegistries() {
        this.registerLang(Lang.class);
    }

    @Override
    protected boolean disableCommandManager() {
        return true;
    }

    @Override
    protected void onStartup() {
        this.commandRegistry = new CommandRegistry(this);
        this.moduleManager = new ModuleManager(this);
    }

    @Override
    public void enable() {
        this.setupInternalNMS();

        this.dataHandler = new DataHandler(this);
        this.dataHandler.setup();

        this.userManager = new UserManager(this, this.dataHandler);
        this.userManager.setup();

        this.teleportManager = new TeleportManager(this, this.sunNMS);
        this.teleportManager.setup();

        this.registerModules(moduleManager);
        moduleManager.loadAll();

        this.commandRegistry.setup();
        this.registerCommands();
        this.registerPermissions(Perms.ROOT);

        if (Utils.hasPlaceholderAPI()) {
            PlaceholderHook.setup(this);
        }
    }

    @Override
    public void disable() {
        if (Utils.hasPlaceholderAPI()) {
            PlaceholderHook.shutdown();
        }

        if (this.moduleManager != null)
            this.moduleManager.clear();
        if (this.dialogRegistry != null)
            this.dialogRegistry.clear();
        if (this.userManager != null)
            this.userManager.shutdown();
        if (this.dataHandler != null)
            this.dataHandler.shutdown();
        if (this.commandRegistry != null)
            this.commandRegistry.shutdown();
    }

    @Override
    protected void onShutdown() {
        super.onShutdown();
    }

    private void setupInternalNMS() {
        if (Version.isBehind(Version.MC_1_21_11)) {
            this.error("Your server version is not supported. Some of the features will be disabled.");
            return;
        }

        if (!this.isPaperServer()) {
            this.error("SunLight requires a Paper-compatible server. Some of the features will be disabled.");
            return;
        }

        try {
            this.sunNMS = switch (Version.getCurrent()) {
                case MC_1_21_11 -> new MC_1_21_11();
                default -> new NMSv26p1();
            };
        } catch (Exception | NoClassDefFoundError e) {
            e.printStackTrace();
        }

        if (this.sunNMS == null) {
            this.error("Unable to hook into server's internals. Some of the features will be disabled.");
        }
    }

    private boolean isPaperServer() {
        try {
            Class.forName("io.papermc.paper.configuration.GlobalConfiguration");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void registerModules(ModuleManager manager) {
        manager.register("afk", "AFK", AfkModule::new);
        manager.register("bans", "Bans", BansModule::new);
        manager.register("back_location", "Back", BackLocationModule::new);
        manager.register("custom_text", "Custom Text", TextsModule::new);
        manager.register("chat", "Chat", ChatModule::new);
        manager.register("death_messages", "Death Messages", DeathMessagesModule::new);
        manager.register("essential", "Essential", EssentialModule::new);
        manager.register("extras", "Extras", ExtrasModule::new);
        manager.register("freeze", "Freeze", FreezeModule::new);
        manager.register("glow", "Glow", GlowModule::new,
                LoadCondition::tab);
        manager.register("greetings", "Greetings", GreetingsModule::new);
        manager.register("homes", "Homes", HomesModule::new);
        manager.register("inventories", "Inventories", InventoriesModule::new);
        manager.register("kits", "Kits", KitsModule::new);
        manager.register("nerf_phantoms", "Nerf Phantoms", PhantomsModule::new);
        manager.register("nametags", "Nametags", NametagsModule::new,
                LoadCondition::tab);
        manager.register("nick", "Nick", NickModule::new);
        manager.register("playerwarps", "Player Warps", PlayerWarpsModule::new);
        manager.register("ptp", "PTP", PTPModule::new);
        manager.register("rtp", "RTP", RTPModule::new);
        manager.register("scheduler", "Scheduler", SchedulerModule::new);
        manager.register("socials", "Socials", SocialsModule::new);
        manager.register("spawns", "Spawn", SpawnsModule::new);
        manager.register("vanish", "Vanish", VanishModule::new);
        manager.register("warmups", "Warmups", WarmupsModule::new);
        manager.register("warps", "Warps", WarpsModule::new);
    }

    private void registerCommands() {
        this.rootCommand = NightCommand.forPlugin((NightCorePlugin) this, builder -> builder
                .branch(Commands.literal("reload")
                        .description(CoreLang.COMMAND_RELOAD_DESC)
                        .permission(Perms.COMMAND_RELOAD)
                        .executes((context, arguments) -> {
                            this.doReload(context.getSender());
                            return true;
                        })));
    }

    private void registerPermissions(PermissionTree tree) {
        tree.toList().forEach(permission -> {
            if (this.getPluginManager().getPermission(permission.getName()) == null) {
                this.getPluginManager().addPermission(permission);
            }
        });
    }

    public DataHandler dataHandler() {
        return this.dataHandler;
    }

    public UserManager userManager() {
        return userManager;
    }

    public ModuleManager moduleManager() {
        return this.moduleManager;
    }

    public SunNMS getInternals() {
        return this.sunNMS;
    }

    public Optional<SunNMS> internals() {
        return Optional.ofNullable(this.sunNMS);
    }

    public CommandRegistry commandRegistry() {
        return this.commandRegistry;
    }

    public TeleportManager teleportManager() {
        return this.teleportManager;
    }

    @Override

    public Optional<? extends AfkProvider> afkProvider() {
        return this.moduleManager.getByType(AfkModule.class);
    }

    @Override

    public Optional<? extends VanishProvider> vanishProvider() {
        return this.moduleManager.getByType(VanishModule.class);
    }

    @Override

    public Optional<? extends FreezeProvider> freezeProvider() {
        return this.moduleManager.getByType(FreezeModule.class);
    }

    @Override

    public Optional<? extends NickProvider> nickProvider() {
        return this.moduleManager.getByType(NickModule.class);
    }

    @Override

    public Optional<? extends SocialsProvider> socialsProvider() {
        return this.moduleManager.getByType(SocialsModule.class);
    }

    @Override

    public Optional<? extends NametagsProvider> nametagsProvider() {
        return this.moduleManager.getByType(NametagsModule.class);
    }

    @Override

    public Optional<? extends GlowProvider> glowProvider() {
        return this.moduleManager.getByType(GlowModule.class);
    }
}
