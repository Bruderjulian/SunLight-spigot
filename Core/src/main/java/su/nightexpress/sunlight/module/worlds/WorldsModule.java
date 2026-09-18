package su.nightexpress.sunlight.module.worlds;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.FileUtil;
import su.nightexpress.nightcore.util.StringUtil;
import su.nightexpress.sunlight.config.PermissionTree;
import su.nightexpress.sunlight.hook.placeholder.PlaceholderRegistry;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.module.ModuleContext;
import su.nightexpress.sunlight.module.spawns.SpawnsModule;
import su.nightexpress.sunlight.module.spawns.Spawn;
import su.nightexpress.sunlight.module.worlds.command.WorldCommands;
import su.nightexpress.sunlight.module.worlds.config.WorldsConfig;
import su.nightexpress.sunlight.module.worlds.config.WorldsLang;
import su.nightexpress.sunlight.module.worlds.config.WorldsPerms;
import su.nightexpress.sunlight.module.worlds.editor.WorldGenerationEditor;
import su.nightexpress.sunlight.module.worlds.editor.WorldListEditor;
import su.nightexpress.sunlight.module.worlds.editor.WorldMainEditor;
import su.nightexpress.sunlight.module.worlds.editor.WorldRulesEditor;
import su.nightexpress.sunlight.module.worlds.impl.WorldData;
import su.nightexpress.sunlight.module.worlds.impl.WorldInventories;
import su.nightexpress.sunlight.module.worlds.impl.WrappedWorld;
import su.nightexpress.sunlight.module.worlds.impl.generation.FlatChunkGenerator;
import su.nightexpress.sunlight.module.worlds.impl.generation.PlainsChunkGenerator;
import su.nightexpress.sunlight.module.worlds.impl.generation.VoidChunkGenerator;
import su.nightexpress.sunlight.module.worlds.listener.InventoryListener;
import su.nightexpress.sunlight.module.worlds.listener.WorldsListener;
import su.nightexpress.sunlight.module.worlds.util.Placeholders;
import su.nightexpress.sunlight.teleport.TeleportContext;
import su.nightexpress.sunlight.teleport.TeleportManager;

import java.io.File;
import java.util.*;

public class WorldsModule extends Module {

    public static final String DIR_WORLDS = "/worlds/";
    public static final String DIR_INVENTORIES = "/inventories/";

    private final TeleportManager teleportManager;

    private final Map<String, ChunkGenerator> generatorMap;
    private final Map<String, WorldInventories> inventoryMap;
    private final Map<String, WorldData> dataMap;

    private WorldListEditor listEditor;
    private WorldMainEditor mainEditor;
    private WorldRulesEditor rulesEditor;
    private WorldGenerationEditor generationEditor;

    public WorldsModule(ModuleContext context, TeleportManager teleportManager) {
        super(context);
        this.teleportManager = teleportManager;
        this.generatorMap = new HashMap<>();
        this.inventoryMap = new HashMap<>();
        this.dataMap = new HashMap<>();
    }

    @Override
    protected void loadModule(FileConfig config) {
        config.initializeOptions(WorldsConfig.class);
        this.plugin.injectLang(WorldsLang.class);

        this.loadGenerators();
        this.loadWorlds();
        this.loadEditor();

        this.addListener(new WorldsListener(this.plugin, this));
        if (WorldsConfig.INVENTORY_SPLIT_ENABLED.get()) {
            this.addListener(new InventoryListener(this.plugin, this));
        }

        this.addTask(this.plugin.createTask(this::tickAutoReset).setSecondsInterval(60));

        if (WorldsConfig.AUTO_RESET_NOTIFICATION_ENABLED.get()) {
            this.addTask(this.plugin.createAsyncTask(this::notifyAutoReset).setSecondsInterval(1));
        }
    }

    @Override
    protected void unloadModule() {
        if (this.listEditor != null)
            this.listEditor.clear();
        if (this.mainEditor != null)
            this.mainEditor.clear();
        if (this.rulesEditor != null)
            this.rulesEditor.clear();
        if (this.generationEditor != null)
            this.generationEditor.clear();

        this.getDatas().forEach(data -> this.unloadWorld(data, false));

        this.inventoryMap.values().forEach(WorldInventories::save);
        this.inventoryMap.clear();

        this.dataMap.clear();
        this.generatorMap.clear();
    }

    @Override
    protected void registerPermissions(PermissionTree root) {
        root.merge(WorldsPerms.MODULE);
    }

    @Override
    protected void registerCommands() {
        this.commandRegistry.addProvider("worlds", new WorldCommands(this.plugin, this), this);
    }

    @Override
    public void registerPlaceholders(PlaceholderRegistry registry) {
        // TODO
        /*
         * WorldData world = this.getWorld(module, subParams, "");
         * if (world != null) return SunUtils.formatDate(world.getNextWipe());
         * 
         * world = this.getWorld(module, subParams, "");
         * if (world != null) return TimeUtil.formatDuration(world.getNextWipe());
         * 
         * world = this.getWorld(module, subParams, "");
         * if (world != null) return SunUtils.formatDate(world.getLastResetDate());
         * 
         * world = this.getWorld(module, subParams, "");
         * if (world != null) return TimeUtil.formatDuration(world.getLastResetDate(),
         * System.currentTimeMillis());
         * 
         * registry.register("autowipe_next_date", (player, payload) -> {
         * 
         * });
         * 
         * registry.register("autowipe_timelft", (player, payload) -> {
         * 
         * });
         * 
         * registry.register("autowipe_latest_date", (player, payload) -> {
         * 
         * });
         * 
         * registry.register("autowipe_latest_since", (player, payload) -> {
         * 
         * });
         */
    }

    private void loadGenerators() {
        this.generatorMap.put(VoidChunkGenerator.NAME, new VoidChunkGenerator());
        this.generatorMap.put(FlatChunkGenerator.NAME, new FlatChunkGenerator());
        this.generatorMap.put(PlainsChunkGenerator.NAME, new PlainsChunkGenerator());
    }

    private void loadWorlds() {
        for (File file : FileUtil.getFiles(this.getSystemPath() + DIR_WORLDS, false)) {
            WorldData worldData = new WorldData(this.plugin, this, file);
            if (worldData.load()) {
                this.dataMap.put(worldData.getId(), worldData);
                if (worldData.isAutoLoad()) {
                    worldData.loadWorld();
                }
            } else
                this.error("World data not loaded: '" + worldData.getFile().getName() + "'!");
        }
    }

    private void loadEditor() {
        this.listEditor = new WorldListEditor(this.plugin, this);
        this.mainEditor = new WorldMainEditor(this.plugin, this);
        this.rulesEditor = new WorldRulesEditor(this.plugin, this);
        this.generationEditor = new WorldGenerationEditor(this.plugin, this);
    }

    public void tickAutoReset() {
        this.getDatas().forEach(WorldData::autoReset);
    }

    public void notifyAutoReset() {
        this.getDatas().forEach(WorldData::autoResetNotify);
    }

    public Map<String, WorldData> getDataMap() {
        return this.dataMap;
    }

    public Collection<WorldData> getDatas() {
        return this.dataMap.values();
    }

    public WorldData getWorldData(String id) {
        return this.dataMap.get(id.toLowerCase());
    }

    public Map<String, WorldInventories> getInventoryMap() {
        return inventoryMap;
    }

    public Map<String, ChunkGenerator> getGeneratorMap() {
        return generatorMap;
    }

    public WorldData createWorldData(String name) {
        name = StringUtil.lowerCaseUnderscore(name);
        if (this.getWorldData(name) != null)
            return null;
        if (this.plugin.getServer().getWorld(name) != null)
            return null;

        File file = new File(this.getAbsolutePath() + DIR_WORLDS, name + ".yml");
        WorldData worldData = new WorldData(this.plugin, this, file);
        worldData.save();
        worldData.load();
        this.dataMap.put(worldData.getId(), worldData);
        return worldData;
    }

    public Set<WrappedWorld> getWorlds() {
        Set<WrappedWorld> set = new HashSet<>();

        Set<World> worlds = new HashSet<>(this.plugin.getServer().getWorlds());

        this.getDatas().forEach(worldConfig -> {
            World world = worldConfig.getWorld();
            if (world != null) {
                worlds.remove(world);
                set.add(wrap(world));
            } else
                set.add(new WrappedWorld(null, worldConfig));
        });

        worlds.forEach(world -> set.add(wrap(world)));

        return set;
    }

    public boolean unloadWorld(WorldData worldData) {
        return this.unloadWorld(worldData, true);
    }

    public boolean unloadWorld(WorldData worldData, boolean movePlayers) {
        World world = worldData.getWorld();
        if (world == null)
            return false;

        if (movePlayers) {
            this.movePlayersOut(world);
        }

        return this.plugin.getServer().unloadWorld(world, true);
    }

    public boolean movePlayersOut(World world) {
        Location location = null;
        if (WorldsConfig.UNLOAD_MOVE_PLAYERS_TO_SPAWN_ENABLED.get()) {
            SpawnsModule spawnsModule = this.plugin.getModuleRegistry().byType(SpawnsModule.class).orElse(null);
            Spawn spawn = spawnsModule == null ? null
                    : spawnsModule.getSpawn(WorldsConfig.UNLOAD_MOVE_PLAYERS_TO_SPAWN_NAME.get());
            if (spawn != null) {
                location = spawn.getLocation();
            }
        }
        if (location == null) {
            World target = this.plugin.getServer().getWorlds().stream().filter(w -> w != world).findFirst()
                    .orElse(null);
            if (target == null)
                return false;

            location = target.getSpawnLocation();
        }

        for (Player player : world.getPlayers()) {
            TeleportContext teleportContext = TeleportContext.builder(this, player, location)
                    .callback(() -> this.sendPrefixed(WorldsLang.UNLOAD_MOVE_OUT_INFO, player))
                    .build();

            this.teleportManager.moveExact(teleportContext);
        }

        return world.getPlayers().isEmpty();
    }

    public boolean isCustomWorld(World world) {
        return this.getWorldData(world.getName()) != null;
    }

    public WrappedWorld wrap(World world) {
        WorldData worldData = this.getWorldData(world.getName());
        return new WrappedWorld(world, worldData);
    }

    public void openEditor(Player player) {
        this.listEditor.open(player, this);
    }

    public void openWorldSettings(Player player, WrappedWorld wrappedWorld) {
        this.mainEditor.open(player, wrappedWorld);
    }

    public void openGameRules(Player player, WrappedWorld wrappedWorld) {
        this.rulesEditor.open(player, wrappedWorld);
    }

    public void openGenerationSettings(Player player, WorldData worldData) {
        this.generationEditor.open(player, worldData);
    }

    public ChunkGenerator getChunkGenerator(String id) {
        if (id == null)
            return null;

        return this.generatorMap.get(id.toLowerCase());
    }

    public ChunkGenerator getPluginGenerator(String world, String name) {
        if (name == null || name.equalsIgnoreCase(Placeholders.DEFAULT))
            return null;

        ChunkGenerator generator = this.getChunkGenerator(name);
        if (generator != null)
            return generator;

        return WorldCreator.getGeneratorForName(world, name, null);
    }

    public boolean isInventoryAffected(Player player) {
        return this.isInventoryAffected(player.getWorld());
    }

    public boolean isInventoryAffected(World world) {
        return this.getWorldGroup(world) != null;
    }

    public WorldInventories getWorldInventory(Player player) {
        String id = player.getUniqueId().toString();
        if (this.inventoryMap.containsKey(id)) {
            return this.inventoryMap.get(id);
        }

        File file = new File(this.getAbsolutePath() + DIR_INVENTORIES, id + ".yml");
        WorldInventories worldInventories = new WorldInventories(this.plugin, this, file);
        worldInventories.load();

        this.inventoryMap.put(worldInventories.getId(), worldInventories);
        return worldInventories;
    }

    public String getWorldGroup(World world) {
        String worldName = world.getName();

        return WorldsConfig.INVENTORY_SPLIT_WORLD_GROUPS.get().entrySet().stream()
                .filter(entry -> entry.getValue().contains(worldName))
                .map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public boolean canFlyThere(Player player) {
        if (player.hasPermission(WorldsPerms.BYPASS_FLY))
            return true;

        return !this.isFlyDisabled(player.getWorld());
    }

    public boolean isFlyDisabled(World world) {
        return WorldsConfig.NO_FLY_WORLDS.get().contains(world.getName());
    }
}
