package su.nightexpress.sunlight.moduleImpl.spawns;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.FileUtil;
import su.nightexpress.nightcore.util.StringUtil;
import su.nightexpress.nightcore.util.Strings;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.ModuleDefinition;
import su.nightexpress.sunlight.moduleImpl.spawns.command.SpawnCommands;
import su.nightexpress.sunlight.moduleImpl.spawns.config.SpawnsLang;
import su.nightexpress.sunlight.moduleImpl.spawns.config.SpawnsPerms;
import su.nightexpress.sunlight.moduleImpl.spawns.config.SpawnsSettings;
import su.nightexpress.sunlight.moduleImpl.spawns.dialog.SpawnsDialogKeys;
import su.nightexpress.sunlight.moduleImpl.spawns.dialog.impl.SpawnNameDialog;
import su.nightexpress.sunlight.moduleImpl.spawns.dialog.impl.SpawnPriorityDialog;
import su.nightexpress.sunlight.moduleImpl.spawns.dialog.impl.SpawnRulesDialog;
import su.nightexpress.sunlight.moduleImpl.spawns.editor.SpawnListEditor;
import su.nightexpress.sunlight.moduleImpl.spawns.editor.SpawnSettingsEditor;
import su.nightexpress.sunlight.moduleImpl.spawns.event.PlayerSpawnTeleportEvent;
import su.nightexpress.sunlight.moduleImpl.spawns.listener.SpawnListener;
import su.nightexpress.sunlight.teleport.*;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.EconomyUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class SpawnsModule extends Module {

    private final TeleportManager teleportManager;
    private final SpawnsSettings settings;
    private final Map<String, Spawn> spawnMap;

    private SpawnListEditor listEditor;
    private SpawnSettingsEditor settingsEditor;

    public SpawnsModule(ModuleDefinition<SpawnsModule> definition, SunLightPlugin plugin) {
        super(definition, plugin);
        this.teleportManager = plugin.teleportManager();
        this.settings = new SpawnsSettings();
        this.spawnMap = new HashMap<>();
    }

    @Override
    protected void loadModule(FileConfig config) {
        this.settings.load(config);
        this.plugin.injectLang(SpawnsLang.class);

        this.loadDialogs();
        this.loadSpawns();

        this.listEditor = new SpawnListEditor(this.plugin, this);
        this.settingsEditor = new SpawnSettingsEditor(this.plugin, this);

        this.addListener(new SpawnListener(this.plugin, this));

        this.addAsyncTask(this::saveSpawns, 60); // TODO Config
    }

    @Override
    protected void unloadModule() {
        this.saveSpawns();
        this.spawnMap.clear();
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(SpawnsPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("spawn", new SpawnCommands(this.plugin, this, this.userManager), this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {

    }

    private void loadDialogs() {
        this.dialogRegistry.register(SpawnsDialogKeys.SPAWN_NAME, SpawnNameDialog::new);
        this.dialogRegistry.register(SpawnsDialogKeys.SPAWN_PRIORITY, SpawnPriorityDialog::new);
        this.dialogRegistry.register(SpawnsDialogKeys.SPAWN_LOGIN_RULES,
                () -> new SpawnRulesDialog(Spawn::getLoginRule));
        this.dialogRegistry.register(SpawnsDialogKeys.SPAWN_RESPAWN_RULES,
                () -> new SpawnRulesDialog(Spawn::getRespawnRule));
    }

    private void loadSpawns() {
        FileUtil.findYamlFiles(this.getSystemPath() + SpawnsFiles.DIR_SPAWNS).forEach(file -> {
            String name = FileUtil.getNameWithoutExtension(file);
            String id = Strings.varStyle(name).orElse(null);
            if (id == null) {
                this.error("Invalid spawn file name: '%s'".formatted(file));
                return;
            }

            Spawn spawn = new Spawn(id, file);
            this.loadSpawn(spawn);
        });

        this.info("Loaded " + this.spawnMap.size() + " spawns!");
    }

    private void loadSpawn(Spawn spawn) {
        try {
            spawn.load();
            spawn.activate();
            this.spawnMap.put(spawn.getId(), spawn);
        } catch (IllegalStateException exception) {
            exception.printStackTrace();
            this.warn("Spawn not loaded: '" + spawn.getFile() + "'!");
        }
    }

    private void saveSpawns() {
        this.getSpawns().forEach(Spawn::saveIfDirty);
    }

    public void openEditor(Player player) {
        this.listEditor.show(this.plugin, player);
    }

    public void openSpawnSettings(Player player, Spawn spawn) {
        this.settingsEditor.show(this.plugin, player, spawn);
    }

    public Map<String, Spawn> getSpawnMap() {
        return Map.copyOf(this.spawnMap);
    }

    public Set<Spawn> getSpawns() {
        return Set.copyOf(this.spawnMap.values());
    }

    public List<String> getSpawnIds() {
        return new ArrayList<>(this.spawnMap.keySet());
    }

    public Spawn getSpawn(String id) {
        return this.spawnMap.get(id.toLowerCase());
    }

    public Spawn getDefaultSpawn() {
        return this.getSpawn(this.settings.getDefaultSpawnId());
    }

    public Spawn getSpawnOrDefault(String id) {
        return this.spawnMap.getOrDefault(id.toLowerCase(), this.getDefaultSpawn());
    }

    public Spawn getNewbieSpawn(Player player) {
        return this.getSpawn(this.settings.getNewPlayersSpawnId());
    }

    public Spawn getLoginSpawn(Player player) {
        return this.getSpawns().stream().filter(spawn -> spawn.isAvailableForJoin(player)).max(Comparator.comparingInt(
                Spawn::getPriority)).orElse(null);
    }

    public Spawn getDeathSpawn(Player player) {
        return this.getSpawns().stream().filter(spawn -> spawn.isAvailableForRespawn(player)).max(Comparator
                .comparingInt(Spawn::getPriority)).orElse(null);
    }

    public boolean createSpawn(Player player, String id) {
        Location location = player.getLocation();
        if (location == null)
            return false;

        id = StringUtil.lowerCaseUnderscore(id);

        Spawn spawn = this.getSpawn(id);
        if (spawn == null) {
            Path file = Path.of(this.getSystemPath() + SpawnsFiles.DIR_SPAWNS, id + ".yml");

            spawn = new Spawn(id, file);
            spawn.setName(StringUtil.capitalizeUnderscored(id));
        }
        spawn.setLocation(location);
        spawn.save();

        this.loadSpawn(spawn);
        Spawn finalSpawn = spawn;
        this.sendPrefixed(SpawnsLang.SPAWN_SET_FEEDBACK, player, replacer -> replacer.with(finalSpawn.placeholders()));
        return true;
    }

    public boolean deleteSpawn(Spawn spawn) {
        try {
            if (Files.deleteIfExists(spawn.getFile())) {
                this.spawnMap.remove(spawn.getId());
                return true;
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return false;
    }

    public void handleJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SunUser user = this.plugin.userManager().getOrFetch(player);
        Spawn spawn;
        if (!user.hasPlayedBefore()) {
            if (!this.settings.isNewPlayersSpawnEnabled())
                return;

            spawn = this.getNewbieSpawn(player);
        } else
            spawn = this.getLoginSpawn(player);

        if (spawn == null || !spawn.isActive())
            return;

        this.teleport(spawn, player, true, true);
    }

    public void handleRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (player.getRespawnLocation() != null && !this.settings.isOverridePlayerRespawnLocation())
            return;

        Spawn spawn = this.getDeathSpawn(player);
        if (spawn == null || !spawn.isActive())
            return;

        event.setRespawnLocation(spawn.getLocation());
    }

    public void teleport(Spawn spawn, Player player) {
        this.teleport(spawn, player, true, false);
    }

    public boolean teleport(Spawn spawn, Player player, boolean forced, boolean silent) {
        if (!spawn.isActive()) {
            if (!silent)
                SpawnsLang.ERROR_SPAWN_INACTIVE.message().send(player);
            return false;
        }

        if (!forced) {
            if (!spawn.hasPermission(player)) {
                if (!silent)
                    this.sendPrefixed(CoreLang.ERROR_NO_PERMISSION, player); // TODO CUstom
                return false;
            }
        }

        PlayerSpawnTeleportEvent event = new PlayerSpawnTeleportEvent(player, spawn);
        plugin.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        double cost = this.settings.getTeleportCost();
        boolean charge = !forced && cost > 0D && !EconomyUtils.hasBypass(player, SpawnsPerms.BYPASS_COST)
                && EconomyUtils
                        .hasCurrency();

        if (charge && !EconomyUtils.canAfford(player, cost)) {
            if (!silent)
                this.sendPrefixed(Lang.COST_ERROR_NOT_ENOUGH_FUNDS, player, builder -> builder
                        .with(SLPlaceholders.GENERIC_AMOUNT, () -> EconomyUtils.format(cost)));
            return false;
        }

        Location location = spawn.getLocation();

        TeleportContext teleportContext = TeleportContext.builder(this, player, location)
                .withFlag(TeleportFlag.LOOK_FOR_SURFACE)
                .withFlag(TeleportFlag.AVOID_LAVA)
                .withFlag(TeleportFlag.CENTERED)
                .withFlagIf(TeleportFlag.BYPASS_WARMUP, () -> forced)
                .callback(() -> {
                    if (charge)
                        EconomyUtils.withdraw(player, cost);

                    if (!silent)
                        this.sendPrefixed(SpawnsLang.SPAWN_TELEPORT_NOTIFY, player, replacer -> replacer.with(spawn
                                .placeholders()));
                })
                .build();

        return this.teleportManager.teleport(teleportContext, TeleportType.SPAWN);
    }
}
