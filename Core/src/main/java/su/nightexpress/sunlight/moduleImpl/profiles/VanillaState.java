package su.nightexpress.sunlight.moduleImpl.profiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

/**
 * Bukkit-side snapshot of a profile: inventory, enderchest, vitals, position.
 *
 * <p>Serializes to plain maps/lists so it can be stored in a YAML file
 * without any NMS. Item stacks use {@link ItemStack#serialize()} which is
 * part of the Bukkit API.
 */
public class VanillaState {

    private List<Map<String, Object>> inventory;
    private List<Map<String, Object>> armor;
    private Map<String, Object> offhand;
    private List<Map<String, Object>> enderchest;

    private double health;
    private int foodLevel;
    private float saturation;
    private float exhaustion;
    private int totalExperience;
    private int level;
    private float expProgress;

    private String gameMode;
    private boolean allowFlight;
    private boolean flying;
    private float flySpeed;
    private float walkSpeed;

    private List<Map<String, Object>> potionEffects;

    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    private double economyBalance;
    private boolean economyTracked;

    public VanillaState() {
        this.inventory = new ArrayList<>();
        this.armor = new ArrayList<>();
        this.enderchest = new ArrayList<>();
        this.potionEffects = new ArrayList<>();
        this.health = 20D;
        this.foodLevel = 20;
        this.saturation = 5F;
        this.gameMode = GameMode.SURVIVAL.name();
    }

    public static VanillaState capture(Player player, ProfileScopeRegistry scopes, double economyBalance, boolean economyTracked) {
        VanillaState state = new VanillaState();

        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_INVENTORY)) {
            PlayerInventory inv = player.getInventory();
            state.inventory = serializeItems(inv.getContents());
            state.armor = serializeItems(inv.getArmorContents());
            ItemStack off = inv.getItemInOffHand();
            state.offhand = (off == null || off.getType().isAir()) ? null : off.serialize();
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_ENDERCHEST)) {
            state.enderchest = serializeItems(player.getEnderChest().getContents());
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_HEALTH)) {
            state.health = Math.max(0D, player.getHealth());
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_FOOD)) {
            state.foodLevel = player.getFoodLevel();
            state.saturation = player.getSaturation();
            state.exhaustion = player.getExhaustion();
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_EXPERIENCE)) {
            state.totalExperience = player.getTotalExperience();
            state.level = player.getLevel();
            state.expProgress = player.getExp();
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_GAMEMODE)) {
            state.gameMode = player.getGameMode().name();
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_FLIGHT)) {
            state.allowFlight = player.getAllowFlight();
            state.flying = player.isFlying();
            state.flySpeed = player.getFlySpeed();
            state.walkSpeed = player.getWalkSpeed();
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_POTIONS)) {
            state.potionEffects = new ArrayList<>();
            for (PotionEffect effect : player.getActivePotionEffects()) {
                state.potionEffects.add(effect.serialize());
            }
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_LOCATION)) {
            Location location = player.getLocation();
            World world = location.getWorld();
            state.world = world == null ? null : world.getName();
            state.x = location.getX();
            state.y = location.getY();
            state.z = location.getZ();
            state.yaw = location.getYaw();
            state.pitch = location.getPitch();
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_ECONOMY)) {
            state.economyBalance = economyBalance;
            state.economyTracked = economyTracked;
        }
        return state;
    }

    /**
     * Applies the snapshot to a live player. Must run on the main thread.
     * Returns the world name the player should end up in (for teleport
     * handling), or {@code null} when location is not scoped per-profile.
     */
    @SuppressWarnings("unchecked")
    public Location apply(Player player, ProfileScopeRegistry scopes) {
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_INVENTORY)) {
            PlayerInventory inv = player.getInventory();
            inv.setContents(deserializeItems(this.inventory, inv.getContents().length));
            inv.setArmorContents(deserializeItems(this.armor, 4));
            try {
                ItemStack off = this.offhand == null ? null : ItemStack.deserialize(this.offhand);
                inv.setItemInOffHand(off);
            } catch (Exception ignored) {
            }
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_ENDERCHEST)) {
            player.getEnderChest().setContents(deserializeItems(this.enderchest, player.getEnderChest().getContents().length));
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_HEALTH)) {
            double max = player.getMaxHealth();
            player.setHealth(Math.min(Math.max(0D, this.health), max));
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_FOOD)) {
            player.setFoodLevel(Math.max(0, this.foodLevel));
            player.setSaturation(Math.max(0F, this.saturation));
            player.setExhaustion(Math.max(0F, this.exhaustion));
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_EXPERIENCE)) {
            player.setTotalExperience(Math.max(0, this.totalExperience));
            player.setLevel(Math.max(0, this.level));
            player.setExp(Math.min(1F, Math.max(0F, this.expProgress)));
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_GAMEMODE)) {
            try {
                player.setGameMode(GameMode.valueOf(this.gameMode));
            } catch (Exception ignored) {
            }
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_FLIGHT)) {
            player.setAllowFlight(this.allowFlight);
            player.setFlying(this.flying && this.allowFlight);
            try {
                player.setFlySpeed(this.flySpeed);
                player.setWalkSpeed(this.walkSpeed == 0F ? 0.2F : this.walkSpeed);
            } catch (Exception ignored) {
            }
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_POTIONS)) {
            for (PotionEffect active : player.getActivePotionEffects()) {
                player.removePotionEffect(active.getType());
            }
            if (this.potionEffects != null) {
                for (Map<String, Object> raw : this.potionEffects) {
                    try {
                        PotionEffect effect = new PotionEffect((Map<String, Object>) raw);
                        player.addPotionEffect(effect);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (scopes.isVanillaPerProfile(ProfileScopeRegistry.VANILLA_LOCATION) && this.world != null) {
            World world = player.getServer().getWorld(this.world);
            if (world != null) {
                return new Location(world, this.x, this.y, this.z, this.yaw, this.pitch);
            }
        }
        return null;
    }

    private static List<Map<String, Object>> serializeItems(ItemStack[] items) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (items == null) return list;
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                list.add(null);
            } else {
                try {
                    list.add(item.serialize());
                } catch (Exception exception) {
                    list.add(null);
                }
            }
        }
        return list;
    }

    private static ItemStack[] deserializeItems(List<Map<String, Object>> list, int size) {
        ItemStack[] items = new ItemStack[Math.max(0, size)];
        if (list == null) return items;
        for (int index = 0; index < items.length && index < list.size(); index++) {
            Map<String, Object> raw = list.get(index);
            if (raw == null) continue;
            try {
                items[index] = ItemStack.deserialize(raw);
            } catch (Exception ignored) {
            }
        }
        return items;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("inventory", this.inventory);
        map.put("armor", this.armor);
        map.put("offhand", this.offhand);
        map.put("enderchest", this.enderchest);
        map.put("health", this.health);
        map.put("food", this.foodLevel);
        map.put("saturation", (double) this.saturation);
        map.put("exhaustion", (double) this.exhaustion);
        map.put("totalExp", this.totalExperience);
        map.put("level", this.level);
        map.put("expProgress", (double) this.expProgress);
        map.put("gamemode", this.gameMode);
        map.put("allowFlight", this.allowFlight);
        map.put("flying", this.flying);
        map.put("flySpeed", (double) this.flySpeed);
        map.put("walkSpeed", (double) this.walkSpeed);
        map.put("potions", this.potionEffects);
        map.put("world", this.world);
        map.put("x", this.x);
        map.put("y", this.y);
        map.put("z", this.z);
        map.put("yaw", (double) this.yaw);
        map.put("pitch", (double) this.pitch);
        map.put("economy", this.economyBalance);
        map.put("economyTracked", this.economyTracked);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static VanillaState deserialize(Map<String, Object> map) {
        VanillaState state = new VanillaState();
        if (map == null) return state;
        state.inventory = castItemList(map.get("inventory"));
        state.armor = castItemList(map.get("armor"));
        Object off = map.get("offhand");
        state.offhand = off instanceof Map ? (Map<String, Object>) off : null;
        state.enderchest = castItemList(map.get("enderchest"));
        state.health = number(map.get("health"), 20D);
        state.foodLevel = (int) number(map.get("food"), 20D);
        state.saturation = (float) number(map.get("saturation"), 5D);
        state.exhaustion = (float) number(map.get("exhaustion"), 0D);
        state.totalExperience = (int) number(map.get("totalExp"), 0D);
        state.level = (int) number(map.get("level"), 0D);
        state.expProgress = (float) number(map.get("expProgress"), 0D);
        Object mode = map.get("gamemode");
        state.gameMode = mode == null ? GameMode.SURVIVAL.name() : String.valueOf(mode);
        state.allowFlight = bool(map.get("allowFlight"), false);
        state.flying = bool(map.get("flying"), false);
        state.flySpeed = (float) number(map.get("flySpeed"), 0.1D);
        state.walkSpeed = (float) number(map.get("walkSpeed"), 0.2D);
        state.potionEffects = castItemList(map.get("potions"));
        Object world = map.get("world");
        state.world = world == null ? null : String.valueOf(world);
        state.x = number(map.get("x"), 0D);
        state.y = number(map.get("y"), 0D);
        state.z = number(map.get("z"), 0D);
        state.yaw = (float) number(map.get("yaw"), 0D);
        state.pitch = (float) number(map.get("pitch"), 0D);
        state.economyBalance = number(map.get("economy"), 0D);
        state.economyTracked = bool(map.get("economyTracked"), false);
        return state;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castItemList(Object raw) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (!(raw instanceof List<?> rawList)) return list;
        for (Object entry : rawList) {
            if (entry == null) {
                list.add(null);
            } else if (entry instanceof Map) {
                list.add((Map<String, Object>) entry);
            }
        }
        return list;
    }

    private static double number(Object raw, double fallback) {
        return raw instanceof Number number ? number.doubleValue() : fallback;
    }

    private static boolean bool(Object raw, boolean fallback) {
        return raw instanceof Boolean value ? value : fallback;
    }

    public double getEconomyBalance() {
        return this.economyBalance;
    }

    public boolean isEconomyTracked() {
        return this.economyTracked;
    }
}
