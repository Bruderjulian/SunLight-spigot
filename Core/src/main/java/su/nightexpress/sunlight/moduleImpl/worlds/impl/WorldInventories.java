package su.nightexpress.sunlight.moduleImpl.worlds.impl;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.manager.AbstractFileData;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.worlds.WorldsModule;
import su.nightexpress.sunlight.moduleImpl.worlds.config.WorldsConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldInventories extends AbstractFileData<SunLightPlugin> {

    private final WorldsModule module;
    private final Map<String, Map<InventoryType, List<ItemStack>>> inventories;

    public WorldInventories(SunLightPlugin plugin, WorldsModule module, File file) {
        super(plugin, file);
        this.module = module;
        this.inventories = new HashMap<>();
    }

    @Override
    protected boolean onLoad(FileConfig config) {
        for (InventoryType type : WorldsConfig.INVENTORY_SPLIT_TYPES) {
            if (!WorldsConfig.isInventoryAffected(type))
                continue;

            for (String worldGroup : config.getSection(type.name())) {
                ItemStack[] items = config.getItemsEncoded(type.name() + "." + worldGroup);
                this.getInventoryMap(worldGroup).put(type, Arrays.asList(items));
            }
        }
        return true;
    }

    @Override
    protected void onSave(FileConfig config) {
        this.inventories.forEach((worldGroup, groupMap) -> {
            groupMap.forEach((type, items) -> {
                config.setItemsEncoded(type.name() + "." + worldGroup, items);
            });
        });
    }

    /**
     * Saves current player's inventory to a specified world group.
     * 
     * @param player Player to save inventories from.
     * @param group  Name of the world group.
     */
    public void saveInventory(Player player, String group) {
        // Always save all inventories to avoid items losing when change settings
        for (InventoryType type : WorldsConfig.INVENTORY_SPLIT_TYPES) {
            this.getInventoryMap(group).put(type, this.copyContents(this.getInventory(player, type).getContents()));
        }
    }

    /**
     * Loads inventories for a player in their current world group.
     * 
     * @param player Player to load inventories for.
     */
    public void loadInventory(Player player) {
        String worldGroup = this.module.getWorldGroup(player.getWorld());
        if (worldGroup == null)
            return;

        for (InventoryType type : WorldsConfig.INVENTORY_SPLIT_TYPES) {
            this.loadInventory(player, worldGroup, type);
        }
    }

    private void loadInventory(Player player, String worldGroup, InventoryType type) {
        ItemStack[] inventoryContent = this.getInventory(player, type).getContents();
        Arrays.fill(inventoryContent, null);

        List<ItemStack> items = this.getInventoryMap(worldGroup).get(type);
        if (items != null) {
            for (int slot = 0; slot < items.size() && slot < inventoryContent.length; slot++) {
                ItemStack item = items.get(slot);
                if (item == null || item.getType().isAir())
                    continue;

                inventoryContent[slot] = new ItemStack(item);
            }
        }

        this.getInventory(player, type).setContents(inventoryContent);
    }

    private Map<InventoryType, List<ItemStack>> getInventoryMap(String group) {
        return this.inventories.computeIfAbsent(group, k -> new HashMap<>());
    }

    private List<ItemStack> copyContents(ItemStack[] contents) {
        List<ItemStack> list = new ArrayList<>(contents.length);
        for (ItemStack item : contents) {
            if (item == null || item.getType().isAir()) {
                list.add(null);
            } else {
                list.add(new ItemStack(item));
            }
        }
        return list;
    }

    private Inventory getInventory(Player player, InventoryType type) {
        if (type == InventoryType.PLAYER)
            return player.getInventory();
        if (type == InventoryType.ENDER_CHEST)
            return player.getEnderChest();
        throw new UnsupportedOperationException("Unsupported inventory type!");
    }
}
