package su.nightexpress.sunlight.module.kits;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import su.nightexpress.nightcore.util.PDCUtil;
import su.nightexpress.sunlight.SunLightPlugin;

import java.util.Optional;
import java.util.UUID;

public class KitsUtils {

    public static NamespacedKey itemOwnerKey;

    public static void loadKeys(SunLightPlugin plugin) {
        itemOwnerKey = new NamespacedKey(plugin, "kits.item_owner");
    }

    public static Optional<UUID> getItemOwner(ItemStack item) {
        String data = PDCUtil.getString(item, itemOwnerKey).orElse(null);
        return data == null ? Optional.empty() : Optional.of(UUID.fromString(data));
    }

    public static void setItemOwner(ItemStack item, Entity entity) {
        setItemOwner(item, entity.getUniqueId());
    }

    public static void setItemOwner(ItemStack item, UUID uuid) {
        PDCUtil.set(item, itemOwnerKey, uuid.toString());
    }

    public static boolean isItemOwner(ItemStack item, Entity entity) {
        return isItemOwner(item, entity.getUniqueId());
    }

    public static boolean isItemOwner(ItemStack item, UUID uuid) {
        Optional<UUID> ownerId = getItemOwner(item);
        return ownerId.isEmpty() || ownerId.get().equals(uuid);
    }

    /*
     * public static ItemStack[] filterNulls(ItemStack[] array) {
     * for (int count = 0; count < array.length; count++) {
     * if (array[count] == null) {
     * array[count] = new ItemStack(Material.AIR);
     * }
     * }
     * return array;
     * }
     */

    /*
     * public static ItemStack[] bindToPlayer( Player player, ItemStack[] from) {
     * ItemStack[] array = new ItemStack[from.length];
     * for (int index = 0; index < array.length; index++) {
     * ItemStack item = new ItemStack(from[index]);
     * KitsUtils.setItemOwner(item, player);
     * array[index] = item;
     * }
     * return array;
     * }
     */

    /*
     * public static ItemStack[] fuseItems(ItemStack[] kitItems, ItemStack[]
     * inventory, List<ItemStack> left) {
     * for (int index = 0; index < inventory.length; index++) {
     * if (index >= kitItems.length) break;
     * 
     * ItemStack itemInv = inventory[index];
     * ItemStack itemKit = kitItems[index];
     * if (itemKit == null || itemKit.getType().isAir()) continue;
     * 
     * if (itemInv == null || itemInv.getType().isAir()) {
     * inventory[index] = new ItemStack(itemKit);
     * }
     * else left.add(itemKit);
     * }
     * return inventory;
     * }
     */
}
