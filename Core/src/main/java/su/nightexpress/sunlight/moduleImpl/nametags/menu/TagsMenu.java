package su.nightexpress.sunlight.moduleImpl.nametags.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.MenuViewer;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.model.NametagTag;
import su.nightexpress.sunlight.utils.EconomyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class TagsMenu extends AbstractMenu {

    private static final int PAGE_SIZE = 27;
    private static final int FIRST_SLOT = 9;

    private final SunLightPlugin plugin;
    private final NametagsModule module;

    public TagsMenu(SunLightPlugin plugin, NametagsModule module) {
        super(MenuType.GENERIC_9X5, BLACK.wrap("Tags"));
        this.plugin = plugin;
        this.module = module;
    }

    public boolean open(Player player) {
        return this.show(this.plugin, player);
    }

    @Override
    public void registerActions() {

    }

    @Override
    public void registerConditions() {

    }

    @Override
    public void defineDefaultLayout() {
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(0, 9).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(36, 45).toArray());
        this.addNextPageItem(Material.ARROW, 41);
        this.addPreviousPageItem(Material.ARROW, 39);
    }

    @Override
    protected void onLoad(FileConfig config) {

    }

    @Override
    protected void onClick(ViewerContext context, InventoryClickEvent event) {

    }

    @Override
    protected void onDrag(ViewerContext context, InventoryDragEvent event) {

    }

    @Override
    protected void onClose(ViewerContext context, InventoryCloseEvent event) {

    }

    @Override
    public void onPrepare(ViewerContext context, InventoryView view, Inventory inventory, List<MenuItem> list) {
        Player player = context.getPlayer();
        MenuViewer viewer = context.getViewer();
        NametagTag selected = this.module.getSelectedTag(player);

        List<Map.Entry<String, NametagTag>> tags = this.module.getTags().entrySet().stream()
            .filter(entry -> this.module.isTagAccessible(player, entry.getValue()))
            .toList();
        viewer.setTotalPages(Math.max(1, (tags.size() + PAGE_SIZE - 1) / PAGE_SIZE));
        if (tags.isEmpty()) return;

        int fromIndex = (viewer.getCurrentPage() - 1) * PAGE_SIZE;
        if (fromIndex >= tags.size()) return;

        int toIndex = Math.min(tags.size(), fromIndex + PAGE_SIZE);
        for (int index = fromIndex; index < toIndex; index++) {
            Map.Entry<String, NametagTag> entry = tags.get(index);
            String tagId = entry.getKey();
            NametagTag tag = entry.getValue();

            boolean isSelected = selected != null && selected.getId().equals(tagId);

            List<String> lore = new ArrayList<>(tag.getDescription());
            if (tag.hasCost()) {
                lore.add("");
                lore.add(GOLD.wrap("Cost: ") + WHITE.wrap(EconomyUtils.format(tag.getCost())));
            }
            lore.add("");
            lore.add(isSelected ? GREEN.wrap("✔ Selected")
                : GOLD.wrap("→ " + UNDERLINED.wrap("Click to select")));

            NightItem icon = NightItem.fromType(Material.NAME_TAG)
                .setDisplayName(tag.getDisplay())
                .setLore(lore);

            list.add(MenuItem.builder()
                .defaultState(icon, actionContext -> {
                    Player clicker = actionContext.getPlayer();
                    clicker.closeInventory();
                    this.module.selectTag(clicker, tagId);
                })
                .slots(FIRST_SLOT + index - fromIndex)
                .build());
        }
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}