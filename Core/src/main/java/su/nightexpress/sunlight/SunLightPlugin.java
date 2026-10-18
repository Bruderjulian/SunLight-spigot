package su.nightexpress.sunlight;

import java.util.Optional;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;

import su.nightexpress.nightcore.NightPlugin;
import su.nightexpress.nightcore.config.PluginDetails;
import su.nightexpress.nightcore.util.Version;
import su.nightexpress.sunlight.api.SunlightAPI;
import su.nightexpress.sunlight.api.provider.*;
import su.nightexpress.sunlight.command.CommandRegistry;
import su.nightexpress.sunlight.config.Config;
import su.nightexpress.sunlight.config.Lang;
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
import su.nightexpress.sunlight.moduleImpl.freeze.FreezeModule;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.moduleImpl.glow.GlowModule;
import su.nightexpress.sunlight.moduleImpl.greetings.GreetingsModule;
import su.nightexpress.sunlight.moduleImpl.homes.HomesModule;
import su.nightexpress.sunlight.moduleImpl.inventories.InventoriesModule;
import su.nightexpress.sunlight.moduleImpl.kits.KitsModule;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nerfphantoms.PhantomsModule;
import su.nightexpress.sunlight.moduleImpl.nick.NickModule;
import su.nightexpress.sunlight.moduleImpl.playerwarps.PlayerWarpsModule;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeModule;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfilesModule;
import su.nightexpress.sunlight.moduleImpl.ptp.PTPModule;
// TEMP
import su.nightexpress.sunlight.moduleImpl.rtp.RTPModule;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.scheduler.SchedulerModule;
import su.nightexpress.sunlight.moduleImpl.socials.SocialsModule;
import su.nightexpress.sunlight.moduleImpl.spawns.SpawnsModule;
import su.nightexpress.sunlight.moduleImpl.texts.TextsModule;
import su.nightexpress.sunlight.moduleImpl.vanish.VanishModule;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.warmups.WarmupsModule;
import su.nightexpress.sunlight.moduleImpl.warps.WarpsModule;
import su.nightexpress.sunlight.nms.SunNMS;
import su.nightexpress.sunlight.nms.SunNMSFactory;
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

    /**
     * CommandAPI must be loaded before any of its classes are touched, so its
     * bootstrap has to run in the plugin's {@code onLoad()} rather than in
     * {@link #onStartup()}.
     */
    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIPaperConfig(this).silentLogs(true));
        super.onLoad();
    }

    @Override
    protected void onStartup() {
        CommandAPI.onEnable();

        this.commandRegistry = new CommandRegistry(this);
        this.moduleManager = new ModuleManager(this);
    }

    @Override
    public void enable() {
        try {
            this.setupInternalNMS();
        } catch (Exception exception) {
            this.error("NMS setup failed, continuing in degraded mode: " + exception.getMessage());
            exception.printStackTrace();
        }

        try {
            this.dataHandler = new DataHandler(this);
            this.dataHandler.setup();
        } catch (Exception exception) {
            this.error("Database setup failed, disabling plugin: " + exception.getMessage());
            exception.printStackTrace();
            return;
        }

        try {
            this.userManager = new UserManager(this, this.dataHandler);
            this.userManager.setup();
        } catch (Exception exception) {
            this.error("User manager setup failed, disabling plugin: " + exception.getMessage());
            exception.printStackTrace();
            return;
        }

        try {
            this.teleportManager = new TeleportManager(this, this.sunNMS);
            this.teleportManager.setup();
        } catch (Exception exception) {
            this.error("Teleport manager setup failed, continuing without it: " + exception.getMessage());
            exception.printStackTrace();
        }

        try {
            this.registerModules(moduleManager);
            moduleManager.loadAll();
        } catch (Exception exception) {
            this.error("Module loading failed: " + exception.getMessage());
            exception.printStackTrace();
        }

        try {
            // Registered before the registry is set up, like the module providers.
            this.commandRegistry.addProvider(new SunlightCommandProvider(this));
            this.commandRegistry.setup();
        } catch (Exception exception) {
            this.error("Command registration failed: " + exception.getMessage());
            exception.printStackTrace();
        }

        if (Utils.hasPlaceholderAPI()) {
            try {
                PlaceholderHook.setup(this);
            } catch (Exception exception) {
                this.error("Placeholder hook failed: " + exception.getMessage());
            }
        }
    }

    @Override
    public void disable() {
        try {
            if (Utils.hasPlaceholderAPI()) {
                PlaceholderHook.shutdown();
            }
        } catch (Exception ignored) {
        }

        if (this.moduleManager != null) {
            try {
                this.moduleManager.clear();
            } catch (Exception exception) {
                this.error("Error during module shutdown: " + exception.getMessage());
            }
        }
        if (this.dialogRegistry != null) {
            try {
                this.dialogRegistry.clear();
            } catch (Exception ignored) {
            }
        }
        try {
            if (this.teleportManager != null)
                this.teleportManager.shutdown();
        } catch (Exception ignored) {
        }
        if (this.userManager != null) {
            try {
                this.userManager.shutdown();
            } catch (Exception exception) {
                this.error("Error during user shutdown: " + exception.getMessage());
            }
        }
        if (this.dataHandler != null) {
            try {
                this.dataHandler.shutdown();
            } catch (Exception exception) {
                this.error("Error during database shutdown: " + exception.getMessage());
            }
        }
        if (this.commandRegistry != null) {
            try {
                this.commandRegistry.shutdown();
            } catch (Exception ignored) {
            }
        }
        try {
            CommandAPI.onDisable();
        } catch (Exception | NoClassDefFoundError ignored) {
        }
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
            // Resolved reflectively: each implementation is compiled against a
            // different Minecraft version and must not be referenced from here.
            this.sunNMS = SunNMSFactory.create(Version.getCurrent());
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
        manager.register("freeze", "Freeze", FreezeModule::new);
        manager.register("friends", "Friends", FriendsModule::new);
        manager.register("glow", "Glow", GlowModule::new);
        manager.register("greetings", "Greetings", GreetingsModule::new);
        manager.register("homes", "Homes", HomesModule::new);
        manager.register("inventories", "Inventories", InventoriesModule::new);
        manager.register("kits", "Kits", KitsModule::new);
        manager.register("nerf_phantoms", "Nerf Phantoms", PhantomsModule::new);
        manager.register("links", "Links", LinksModule::new);
        manager.register("nametags", "Nametags", NametagsModule::new,
                LoadCondition::tabOrBridge);
        manager.register("nick", "Nick", NickModule::new);
        manager.register("playerwarps", "Player Warps", PlayerWarpsModule::new);
        manager.register("playtime", "Playtime", PlaytimeModule::new);
        manager.register("profiles", "Profiles", ProfilesModule::new);
        manager.register("ptp", "PTP", PTPModule::new);
        manager.register("rtp", "RTP", RTPModule::new);
        manager.register("reports", "Reports", ReportsModule::new);
        manager.register("scheduler", "Scheduler", SchedulerModule::new);
        manager.register("socials", "Socials", SocialsModule::new);
        manager.register("spawns", "Spawn", SpawnsModule::new);
        manager.register("vanish", "Vanish", VanishModule::new);
        manager.register("voting", "Voting", VotingModule::new);
        manager.register("warmups", "Warmups", WarmupsModule::new);
        manager.register("warps", "Warps", WarpsModule::new);
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

    public Optional<? extends FriendsProvider> friendsProvider() {
        return this.moduleManager.getByType(FriendsModule.class);
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

    public Optional<? extends LinksProvider> linksProvider() {
        return this.moduleManager.getByType(LinksModule.class);
    }

    @Override

    public Optional<? extends NametagsProvider> nametagsProvider() {
        return this.moduleManager.getByType(NametagsModule.class);
    }

    @Override

    public Optional<? extends GlowProvider> glowProvider() {
        return this.moduleManager.getByType(GlowModule.class);
    }

    @Override

    public Optional<? extends ProfilesProvider> profilesProvider() {
        return this.moduleManager.getByType(ProfilesModule.class);
    }

    @Override

    public Optional<? extends ReportsProvider> reportsProvider() {
        return this.moduleManager.getByType(ReportsModule.class);
    }

    @Override
    public Optional<? extends HomesProvider> homesProvider() {
        return this.moduleManager.getByType(HomesModule.class);
    }

    @Override
    public Optional<? extends WarpsProvider> warpsProvider() {
        return this.moduleManager.getByType(WarpsModule.class);
    }

    @Override
    public Optional<? extends KitsProvider> kitsProvider() {
        return this.moduleManager.getByType(KitsModule.class);
    }

@Override
    public Optional<? extends su.nightexpress.sunlight.api.provider.BansProvider> bansProvider() {
        return this.moduleManager.getByType(BansModule.class);
    }

    @Override
    public Optional<? extends SpawnsProvider> spawnsProvider() {
        return this.moduleManager.getByType(SpawnsModule.class);
    }

    @Override
    public Optional<? extends PtpProvider> ptpProvider() {
        return this.moduleManager.getByType(PTPModule.class);
    }

    @Override
    public Optional<? extends BackLocationProvider> backLocationProvider() {
        return this.moduleManager.getByType(BackLocationModule.class);
    }

    @Override
    public Optional<? extends PlayerWarpsProvider> playerWarpsProvider() {
        return this.moduleManager.getByType(PlayerWarpsModule.class);
    }

    @Override
    public Optional<? extends WarmupsProvider> warmupsProvider() {
        return this.moduleManager.getByType(WarmupsModule.class);
    }

    @Override
    public Optional<? extends RtpProvider> rtpProvider() {
        return this.moduleManager.getByType(RTPModule.class);
    }

    @Override
    public Optional<? extends TextsProvider> textsProvider() {
        return this.moduleManager.getByType(TextsModule.class);
    }

    @Override
    public Optional<? extends ChatProvider> chatProvider() {
        return this.moduleManager.getByType(ChatModule.class);
    }

    @Override
    public Optional<? extends GreetingsProvider> greetingsProvider() {
        return this.moduleManager.getByType(GreetingsModule.class);
    }

    @Override
    public Optional<? extends EssentialProvider> essentialProvider() {
        return this.moduleManager.getByType(EssentialModule.class);
    }

    @Override
    public Optional<? extends InventoriesProvider> inventoriesProvider() {
        return this.moduleManager.getByType(InventoriesModule.class);
    }

    @Override
    public Optional<? extends PhantomsProvider> phantomsProvider() {
        return this.moduleManager.getByType(PhantomsModule.class);
    }

}
