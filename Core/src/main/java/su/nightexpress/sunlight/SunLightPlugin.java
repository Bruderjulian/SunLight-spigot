package su.nightexpress.sunlight;

import java.util.Comparator;
import java.util.Optional;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;

import su.nightexpress.nightcore.NightCorePlugin;
import su.nightexpress.nightcore.NightPlugin;
import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.NodeUtils;
import su.nightexpress.nightcore.commands.command.NightCommand;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.tree.CommandNode;
import su.nightexpress.nightcore.commands.tree.ExecutableNode;
import su.nightexpress.nightcore.config.PluginDetails;
import su.nightexpress.nightcore.core.config.CoreLang;
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
import su.nightexpress.sunlight.moduleImpl.warmups.WarmupsModule;
import su.nightexpress.sunlight.moduleImpl.warps.WarpsModule;
import su.nightexpress.sunlight.nms.SunNMS;
import su.nightexpress.sunlight.nms.SunNMSFactory;
import su.nightexpress.sunlight.teleport.TeleportManager;
import su.nightexpress.sunlight.user.UserManager;
import su.nightexpress.sunlight.utils.Utils;

import static su.nightexpress.nightcore.util.Placeholders.GENERIC_DESCRIPTION;
import static su.nightexpress.nightcore.util.Placeholders.GENERIC_ENTRY;
import static su.nightexpress.sunlight.SLPlaceholders.*;

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
            this.commandRegistry.setup();
            this.registerCommands();
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
        manager.register("warmups", "Warmups", WarmupsModule::new);
        manager.register("warps", "Warps", WarpsModule::new);
    }

    private void registerCommands() {
        this.rootCommand = NightCommand.forPlugin((NightCorePlugin) this, builder -> builder
                .withHelpCommand(false)
                .executes((context, arguments) -> {
                    this.sendHelp(context);
                    return true;
                })
                .branch(Commands.literal("help")
                        .description(CoreLang.COMMAND_HELP_DESC)
                        .executes((context, arguments) -> {
                            this.sendHelp(context);
                            return true;
                        }))
                .branch(Commands.literal("reload")
                        .description(CoreLang.COMMAND_RELOAD_DESC)
                        .permission("sunlight.command.reload")
                        .executes((context, arguments) -> {
                            this.doReload(context.getSender());
                            return true;
                        }))
                .branch(Commands.literal("reloadmodule")
                        .description("Reloads a single module: /sunlight reloadmodule <id>.")
                        .permission("sunlight.command.reload")
                        .executes((context, arguments) -> {
                            String moduleId = this.parseTrailingArg(arguments);
                            if (moduleId == null || moduleId.isBlank()) {
                                context.getSender().sendMessage("Usage: /sunlight reloadmodule <id>");
                                return false;
                            }
                            boolean ok = this.moduleManager.reloadSingle(moduleId.toLowerCase(java.util.Locale.ROOT));
                            context.getSender().sendMessage("Reload module '" + moduleId + "': " + (ok ? "OK" : "NOT FOUND"));
                            return ok;
                        }))
                .branch(Commands.literal("dump")
                        .description("Prints diagnostics for support.")
                        .permission("sunlight.command.reload")
                        .executes((context, arguments) -> {
                            this.sendDump(context.getSender());
                            return true;
                        }))
                .branch(Commands.literal("export")
                        .description("Exports plugin data to a zip file.")
                        .permission("sunlight.command.reload")
                        .executes((context, arguments) -> {
                            String name = this.parseTrailingArg(arguments);
                            this.runTaskAsync(() -> this.exportData(context.getSender(), name));
                            return true;
                        }))
                .branch(Commands.literal("import")
                        .description("Imports plugin data from a zip file and reloads. Usage: /sunlight import <name>.")
                        .permission("sunlight.command.reload")
                        .executes((context, arguments) -> {
                            String name = this.parseTrailingArg(arguments);
                            if (name == null || name.isBlank()) {
                                name = this.latestExportName();
                                if (name == null) {
                                    context.getSender().sendMessage("No exports found. Usage: /sunlight import <name>.");
                                    return false;
                                }
                            }
                            final String fileName = name;
                            this.runTaskAsync(() -> this.importData(context.getSender(), fileName));
                            return true;
                        })));
    }

    private String parseTrailingArg(Object arguments) {
        if (arguments == null) return null;
        try {
            java.lang.reflect.Method fullInput = arguments.getClass().getMethod("fullInput");
            Object raw = fullInput.invoke(arguments);
            if (raw instanceof String text && !text.isBlank()) {
                String[] parts = text.trim().split("\\s+");
                if (parts.length == 0) return null;
                return parts[parts.length - 1];
            }
        } catch (Exception ignored) {
        }
        try {
            java.lang.reflect.Method get = arguments.getClass().getMethod("get", String.class);
            for (String key : new String[]{"module", "name", "id", "arg"}) {
                try {
                    Object value = get.invoke(arguments, key);
                    if (value instanceof String text && !text.isBlank()) return text;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String latestExportName() {
        try {
            java.nio.file.Path outDir = this.getDataFolder().toPath().resolve("exports");
            if (!java.nio.file.Files.isDirectory(outDir)) return null;
            try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(outDir)) {
                return stream.filter(p -> p.getFileName().toString().endsWith(".zip"))
                    .max(java.util.Comparator.comparingLong(p -> {
                        try { return java.nio.file.Files.getLastModifiedTime(p).toMillis(); } catch (Exception e) { return 0L; }
                    }))
                    .map(p -> {
                        String name = p.getFileName().toString();
                        return name.substring(0, name.length() - 4);
                    }).orElse(null);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private void sendDump(CommandSender sender) {
        StringBuilder builder = new StringBuilder();
        builder.append("SunLight ").append(this.getDescription().getVersion());
        builder.append(" | Server ").append(this.getServer().getVersion());
        builder.append(" | Java ").append(System.getProperty("java.version"));
        builder.append(" | Online ").append(this.getServer().getOnlinePlayers().size());
        builder.append(" | NMS ").append(this.sunNMS == null ? "none" : this.sunNMS.getClass().getSimpleName());
        builder.append(" | Modules ");
        this.moduleManager.getModules().stream()
            .sorted(java.util.Comparator.comparing(su.nightexpress.sunlight.module.Module::getId))
            .forEach(module -> builder.append(module.getId()).append(","));
        sender.sendMessage(builder.toString());
        this.getLogger().info("[SunLight dump] " + builder);
    }

    private void exportData(CommandSender sender, String name) {
        try {
            java.nio.file.Path dataDir = this.getDataFolder().toPath();
            java.nio.file.Path outDir = dataDir.resolve("exports");
            java.nio.file.Files.createDirectories(outDir);
            String fileName = (name == null || name.isBlank() ? "backup-" + System.currentTimeMillis() : name) + ".zip";
            java.nio.file.Path out = outDir.resolve(fileName);
            try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(out))) {
                try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(dataDir)) {
                    for (java.nio.file.Path path : (Iterable<java.nio.file.Path>) stream::iterator) {
                        if (java.nio.file.Files.isDirectory(path)) continue;
                        if (path.startsWith(outDir)) continue;
                        String entry = dataDir.relativize(path).toString().replace('\\', '/');
                        zip.putNextEntry(new java.util.zip.ZipEntry(entry));
                        java.nio.file.Files.copy(path, zip);
                        zip.closeEntry();
                    }
                }
            }
            sender.sendMessage("Exported to exports/" + fileName);
        } catch (Exception exception) {
            exception.printStackTrace();
            sender.sendMessage("Export failed: " + exception.getMessage());
        }
    }

    private void importData(CommandSender sender, String name) {
        try {
            java.nio.file.Path file = this.getDataFolder().toPath().resolve("exports").resolve(name.endsWith(".zip") ? name : name + ".zip");
            if (!java.nio.file.Files.exists(file)) {
                sender.sendMessage("Import file not found: exports/" + name);
                return;
            }
            try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(java.nio.file.Files.newInputStream(file))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    java.nio.file.Path out = this.getDataFolder().toPath().resolve(entry.getName()).normalize();
                    if (!out.startsWith(this.getDataFolder().toPath())) continue;
                    java.nio.file.Files.createDirectories(out.getParent());
                    java.nio.file.Files.copy(zip, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
            this.runTask(task -> this.doReload(sender));
            sender.sendMessage("Import complete, reloaded.");
        } catch (Exception exception) {
            exception.printStackTrace();
            sender.sendMessage("Import failed: " + exception.getMessage());
        }
    }

    /**
     * Prints the root command help. Handled explicitly (instead of relying on the
     * hub fallback) so that both '/sunlight' and '/sunlight help' always list the
     * available sub-commands.
     */
    private void sendHelp(CommandContext context) {
        CommandSender sender = context.getSender();
        CommandNode root = context.getRoot();

        Lang.PLUGIN_HELP.message().send(sender, replacer -> replacer
                .replace(GENERIC_NAME, this.getNameLocalized())
                .replace(GENERIC_ENTRY, list -> {
                    root.getChildren().stream().sorted(Comparator.comparing(CommandNode::getName)).forEach(child -> {
                        if (!child.hasPermission(sender))
                            return;
                        if (!(child instanceof ExecutableNode executable))
                            return;

                        String label = root instanceof ExecutableNode hub
                                ? (NodeUtils.formatLabel(hub, context) + " " + executable.getUsage()).trim()
                                : (root.getName() + " " + executable.getUsage()).trim();

                        list.add(Lang.PLUGIN_HELP_ENTRY.text()
                                .replace(GENERIC_COMMAND, label)
                                .replace(GENERIC_DESCRIPTION, executable.getDescription()));
                    });
                }));
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

    public Optional<? extends PlaytimeProvider> playtimeProvider() {
        return this.moduleManager.getByType(PlaytimeModule.class);
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
