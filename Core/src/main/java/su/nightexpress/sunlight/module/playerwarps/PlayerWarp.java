package su.nightexpress.sunlight.module.playerwarps;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.user.UserInfo;
import su.nightexpress.nightcore.util.TimeUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.geodata.pos.ExactPos;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolvable;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.module.playerwarps.category.WarpCategory;
import su.nightexpress.sunlight.module.playerwarps.core.PlayerWarpsPerms;
import su.nightexpress.sunlight.module.playerwarps.core.PlayerWarpsSettings;
import su.nightexpress.sunlight.module.playerwarps.exception.PlayerWarpLoadException;
import su.nightexpress.sunlight.module.playerwarps.category.NormalCategory;
import su.nightexpress.sunlight.module.playerwarps.featuring.FeaturedData;
import su.nightexpress.sunlight.module.playerwarps.featuring.FeaturedSlot;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerWarp implements PlaceholderResolvable {

    private final Path file;
    private final String id;

    private String worldName;
    private ExactPos blockPos;
    private long creationTimestamp;

    private String name;
    private List<String> description;
    private NightItem icon;
    private double price;
    private UserInfo owner;
    private String categoryId;
    private long totalVisits;
    private FeaturedData featuredData;

    private World world;
    private boolean dirty;
    private WarpCategory category;

    public PlayerWarp(Path file, String id) {
        this.file = file;
        this.id = id;
    }

    @Override

    public PlaceholderResolver placeholders() {
        return PlayerWarpsPlaceholders.PLAYER_WARP.resolver(this);
    }

    public void load() throws PlayerWarpLoadException {
        this.loadConfig().edit(this::loadFromConfig);
    }

    public void loadFromConfig(FileConfig config) {
        this.blockPos = ExactPos.read(config, "BlockPos");
        this.worldName = config.getString("World");

        this.setIcon(config.getCosmeticItem("Icon"));
        this.setName(config.getString("Name", this.getId()));
        this.setDescription(config.getStringList("Description"));
        this.setPrice(config.getDouble("Price.Amount"));
        this.setCreationTimestamp(config.getLong("CreationTimestamp"));

        String ownerString = config.getString("Owner.Id", "null");

        UUID ownerId;
        try {
            ownerId = UUID.fromString(ownerString);
        } catch (IllegalArgumentException exception) {
            throw new PlayerWarpLoadException("Invalid owner UUID: '%s'.".formatted(ownerString));
        }

        String ownerName = String.valueOf(config.getString("Owner.Name"));
        this.setOwner(new UserInfo(ownerId, ownerName));
        this.setCategoryId(config.getString("CategoryId", SLPlaceholders.DEFAULT));
        this.setTotalVisits(config.getLong("Visits"));

        if (config.contains("Featured")) {
            String slotId = config.getString("Featured.Id", "null");
            int slotIndex = config.getInt("Featured.SlotIndex");
            long endTimestamp = config.getLong("Featured.EndTimestamp");
            if (!TimeUtil.isPassed(endTimestamp)) {
                this.setFeaturedData(new FeaturedData(slotId, slotIndex, endTimestamp));
            } else {
                config.remove("Featured");
            }
        }
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
        config.set("Price.Amount", this.price);
        config.set("Icon", this.icon);
        config.set("CreationTimestamp", this.creationTimestamp);

        config.set("Owner.Id", this.owner.id().toString());
        config.set("Owner.Name", this.owner.name());
        config.set("CategoryId", this.categoryId);
        config.set("Visits", this.totalVisits);

        if (this.featuredData != null && this.featuredData.isActive()) {
            config.set("Featured.Id", this.featuredData.slotId());
            config.set("Featured.SlotIndex", this.featuredData.slotIndex());
            config.set("Featured.EndTimestamp", this.featuredData.endTimestamp());
        } else {
            config.remove("Featured");
        }
    }

    public void updateCategory(PlayerWarpsSettings settings) {
        Optional.ofNullable(settings.getCategory(this.categoryId)).ifPresent(normalCategory -> {
            this.category = normalCategory;
        });
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

    public boolean isOwner(Player player) {
        return this.owner.isUser(player);
    }

    public boolean isCategory(NormalCategory category) {
        return category.isWarpOfThis(this);
    }

    public boolean canUse(Player player) {
        return true;
    }

    public boolean canFeature() {
        return !this.isFeatured();
    }

    public boolean canEdit(Player player) {
        return this.isOwner(player) || player.hasPermission(PlayerWarpsPerms.BYPASS_OWNERSHIP);
    }

    public boolean isFeatured(FeaturedSlot slot, int slotIndex) {
        return this.isFeatured() && this.featuredData.slotId().equalsIgnoreCase(slot.id())
                && this.featuredData.slotIndex() == slotIndex;
    }

    public boolean isFeatured() {
        return this.featuredData != null && this.featuredData.isActive();
    }

    public void addVisitCount() {
        this.totalVisits += 1;
    }

    public boolean hasPrice() {
        return this.price > 0D;
    }

    public FileConfig loadConfig() {
        return FileConfig.load(this.file);
    }

    public Path getFile() {
        return this.file;
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

    public long getCreationTimestamp() {
        return this.creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
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

    public double getPrice() {
        return this.price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public NightItem getIcon() {
        return this.icon.copy();
    }

    public void setIcon(NightItem icon) {
        this.icon = icon.copy();
    }

    public UserInfo getOwner() {
        return this.owner;
    }

    public UUID getOwnerId() {
        return this.owner.id();
    }

    public String getOwnerName() {
        return this.owner.name();
    }

    public void setOwner(UserInfo owner) {
        this.owner = owner;
    }

    public String getCategoryId() {
        return this.categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public WarpCategory getCategory() {
        return this.category;
    }

    public void setCategory(NormalCategory category) {
        this.category = category;
        this.categoryId = category.id();
    }

    public long getTotalVisits() {
        return this.totalVisits;
    }

    public void setTotalVisits(long totalVisits) {
        this.totalVisits = totalVisits;
    }

    public FeaturedData getFeaturedData() {
        return this.featuredData;
    }

    public void setFeaturedData(FeaturedData featuredData) {
        this.featuredData = featuredData;
    }
}
