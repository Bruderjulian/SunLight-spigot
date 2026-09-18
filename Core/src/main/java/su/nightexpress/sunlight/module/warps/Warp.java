package su.nightexpress.sunlight.module.warps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import su.nightexpress.nightcore.commands.command.NightCommand;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.util.Strings;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.geodata.pos.ExactPos;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolvable;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import su.nightexpress.sunlight.module.warps.core.WarpsPerms;
import su.nightexpress.sunlight.module.warps.exception.WarpLoadException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Warp implements PlaceholderResolvable {

    private final Path file;
    private final String id;

    private String name;
    private List<String> description;
    private String worldName;
    private ExactPos blockPos;
    private NightItem icon;

    private int menuPage;
    private int[] menuSlots;
    private boolean permissionRequired;

    private boolean commandEnabled;
    private String commandLabel;

    private World world;
    private boolean dirty;

    private NightCommand command;

    public Warp(Path file, String id) {
        this.file = file;
        this.id = id;
    }

    @Override

    public PlaceholderResolver placeholders() {
        return WarpsPlaceholders.WARP.resolver(this);
    }

    public void load() throws WarpLoadException {
        this.loadConfig().edit(this::loadFromConfig);
    }

    public void loadFromConfig(FileConfig config) {
        this.blockPos = ExactPos.read(config, "BlockPos");
        this.worldName = config.getString("World");

        this.setIcon(config.getCosmeticItem("Icon"));
        this.setName(config.getString("Name", this.getId()));
        this.setDescription(config.getStringList("Description"));
        this.setPermissionRequired(config.getBoolean("Permission_Required"));

        this.setMenuPage(config.get(ConfigTypes.INT, "Menu.Page", 1));
        this.setMenuSlots(config.get(ConfigTypes.INT_ARRAY, "Menu.Slots", new int[0]));

        this.setCommandEnabled(config.get(ConfigTypes.BOOLEAN, "Command.Enabled", false));
        this.setCommandLabel(config.get(ConfigTypes.STRING, "Command.Label", this.id));
    }

    public void saveIfDirty() {
        if (this.dirty) {
            this.save();
            this.markClean();
        }
    }

    public void save() {
        this.loadConfig().edit(this::writeToConfig);
    }

    private void writeToConfig(FileConfig config) {
        config.set("World", this.worldName);
        config.set("BlockPos", this.blockPos);
        config.set("Name", this.name);
        config.set("Description", this.description);
        config.set("Icon", this.icon);
        config.set("Permission_Required", this.permissionRequired);

        config.set("Menu.Page", this.menuPage);
        config.setArray("Menu.Slots", this.menuSlots);

        config.set("Command.Enabled", this.commandEnabled);
        config.set("Command.Label", this.commandLabel);
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }

    public boolean isActive() {
        return this.world != null;
    }

    public boolean isInactive() {
        return !this.isActive();
    }

    public boolean isWorld(World world) {
        return this.worldName.equalsIgnoreCase(world.getName());
    }

    public boolean hasPermission(Player player) {
        return !this.permissionRequired || player.hasPermission(this.getPermission());
    }

    public boolean canUse(Player player) {
        return this.hasPermission(player);
    }

    public boolean canEdit(Player player) {
        return player.hasPermission(WarpsPerms.EDITOR);
    }

    public void activate() {
        World world = Bukkit.getWorld(this.worldName);
        if (world != null) {
            this.activate(world);
        }
    }

    public void activate(World world) {
        if (this.worldName.equalsIgnoreCase(world.getName())) {
            this.world = world;
        }
    }

    public void deactivate() {
        this.world = null;
    }

    public void clearCommand() {
        if (this.command != null && this.command.unregister()) {
            this.command = null;
        }
    }

    public World getWorld() {
        if (this.world == null)
            throw new IllegalStateException("Warp's world is not loaded");

        return this.world;
    }

    public Location getLocation() {
        return this.blockPos.toLocation(this.getWorld());
    }

    public void setLocation(Location location) {
        World locWorld = location.getWorld();
        if (locWorld == null)
            return;

        this.worldName = locWorld.getName();
        this.blockPos = ExactPos.from(location);
    }

    public void setCommand(NightCommand command) {
        this.command = command;
    }

    public NightCommand getCommand() {
        return this.command;
    }

    public String getPermission() {
        return WarpsPerms.WARP.childrenNode(this.getId());
    }

    public Path getFile() {
        return this.file;
    }

    public FileConfig loadConfig() {
        return FileConfig.load(this.file);
    }

    public String getId() {
        return this.id;
    }

    public String getWorldName() {
        return this.worldName;
    }

    public ExactPos getBlockPos() {
        return this.blockPos;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDescription() {
        return List.copyOf(this.description);
    }

    public void setDescription(List<String> description) {
        this.description = new ArrayList<>(description);
    }

    public NightItem getIcon() {
        return this.icon.copy();
    }

    public void setIcon(NightItem icon) {
        this.icon = icon.copy();
    }

    public int getMenuPage() {
        return this.menuPage;
    }

    public void setMenuPage(int menuPage) {
        this.menuPage = menuPage;
    }

    public int[] getMenuSlots() {
        return this.menuSlots;
    }

    public void setMenuSlots(int... menuSlots) {
        this.menuSlots = menuSlots;
    }

    public boolean isPermissionRequired() {
        return this.permissionRequired;
    }

    public void setPermissionRequired(boolean isPermission) {
        this.permissionRequired = isPermission;
    }

    public boolean isCommandEnabled() {
        return this.commandEnabled;
    }

    public void setCommandEnabled(boolean commandEnabled) {
        this.commandEnabled = commandEnabled;
    }

    public String getCommandLabel() {
        return this.commandLabel;
    }

    public void setCommandLabel(String commandLabel) {
        this.commandLabel = Strings.varStyle(commandLabel).orElse(this.id);
    }
}
