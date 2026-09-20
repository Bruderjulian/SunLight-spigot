package su.nightexpress.sunlight;

import java.util.Optional;

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
import su.nightexpress.sunlight.api.provider.NickProvider;
import su.nightexpress.sunlight.api.provider.VanishProvider;
import su.nightexpress.sunlight.command.CommandRegistry;
import su.nightexpress.sunlight.config.Config;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.config.Perms;
import su.nightexpress.sunlight.data.DataHandler;
import su.nightexpress.sunlight.hook.impl.PlaceholderHook;
import su.nightexpress.sunlight.module.LoadCondition;
import su.nightexpress.sunlight.module.ModuleId;
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
import su.nightexpress.sunlight.moduleImpl.items.ItemsModule;
import su.nightexpress.sunlight.moduleImpl.kits.KitsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.PhantomsModule;
import su.nightexpress.sunlight.moduleImpl.nick.NickModule;
import su.nightexpress.sunlight.moduleImpl.playerwarps.PlayerWarpsModule;
import su.nightexpress.sunlight.moduleImpl.ptp.PTPModule;
import su.nightexpress.sunlight.moduleImpl.rtp.RTPModule;
import su.nightexpress.sunlight.moduleImpl.recipes.RecipesModule;
import su.nightexpress.sunlight.moduleImpl.scheduler.SchedulerModule;
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

        /*
         * if (this.moduleRegistry.isCompleted()) {
         * this.info("Reloading all modules...");
         * this.moduleRegistry.reload();
         * }
         * else {
         */
        // this.info("Initializing modules...");
        this.loadModules();
        // }

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

    private void loadModules() {
        ModuleManager loader = new ModuleManager(this);

        loader.register(ModuleId.AFK, "AFK", AfkModule::new);
        loader.register(ModuleId.BANS, "Bans", BansModule::new);
        loader.register(ModuleId.BACK_LOCATION, "Back", BackLocationModule::new);
        loader.register(ModuleId.CUSTOM_TEXT, "Custom Text", TextsModule::new);
        loader.register(ModuleId.CHAT, "Chat", ChatModule::new);
        loader.register(ModuleId.DEATH_MESSAGES, "Death Messages", DeathMessagesModule::new);
        loader.register(ModuleId.ESSENTIAL, "Essential", EssentialModule::new);
        loader.register(ModuleId.EXTRAS, "Extras", ExtrasModule::new);
        loader.register(ModuleId.FREEZE, "Freeze", FreezeModule::new);
        loader.register(ModuleId.GLOW, "Glow", GlowModule::new);
        loader.register(ModuleId.GREETINGS, "Greetings", GreetingsModule::new);
        loader.register(ModuleId.HOMES, "Homes", HomesModule::new);
        loader.register(ModuleId.INVENTORIES, "Inventories", InventoriesModule::new);
        loader.register(ModuleId.ITEMS, "Items", ItemsModule::new);
        loader.register(ModuleId.KITS, "Kits", KitsModule::new);
        loader.register(ModuleId.NAME_TAGS, "Nametags", NametagsModule::new,
                LoadCondition::packetLibrary);
        loader.register(ModuleId.NERF_PHANTOMS, "Nerf Phantoms", PhantomsModule::new);
        loader.register(ModuleId.NICK, "Nick", NickModule::new);
        loader.register(ModuleId.PLAYER_WARPS, "Player Warps", PlayerWarpsModule::new);
        loader.register(ModuleId.PTP, "PTP", PTPModule::new);
        loader.register(ModuleId.RTP, "RTP", RTPModule::new);
        loader.register(ModuleId.RECIPES, "Recipes", RecipesModule::new);
        loader.register(ModuleId.SCHEDULER, "Scheduler", SchedulerModule::new);
        loader.register(ModuleId.SPAWNS, "Spawn", SpawnsModule::new);
        loader.register(ModuleId.VANISH, "Vanish", VanishModule::new);
        loader.register(ModuleId.WARMUPS, "Warmups", WarmupsModule::new);
        loader.register(ModuleId.WARPS, "Warps", WarpsModule::new);

        loader.loadAll();
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

    private void registerCommands() {
        this.rootCommand = NightCommand.forPlugin(this, builder -> builder
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

    public Optional<? extends GlowProvider> glowProvider() {
        return this.moduleManager.getByType(GlowModule.class);
    }
}
