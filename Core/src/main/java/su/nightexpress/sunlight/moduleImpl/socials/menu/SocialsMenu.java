package su.nightexpress.sunlight.moduleImpl.socials.menu;

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
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.socials.SocialsModule;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialLink;
import su.nightexpress.sunlight.moduleImpl.socials.config.SocialsPerms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class SocialsMenu extends AbstractMenu {

    private final SunLightPlugin plugin;
    private final SocialsModule module;

    public SocialsMenu(SunLightPlugin plugin, SocialsModule module) {
        super(MenuType.GENERIC_9X5, BLACK.wrap("Social Links"));
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

        List<Map.Entry<String, SocialLink>> links = this.module.getLinkMap().entrySet().stream()
            .filter(entry -> SocialsPerms.hasLinkAccess(player, entry.getValue().permission(), entry.getKey()))
            .limit(21)
            .toList();
        if (links.isEmpty()) return;

        List<Integer> slots = new ArrayList<>();
        for (int row = 0; row * 9 < links.size(); row++) {
            int inRow = Math.min(9, links.size() - row * 9);
            int offset = (9 - inRow) / 2;
            for (int i = 0; i < inRow; i++) {
                slots.add(10 + row * 9 + offset + i);
            }
        }

        for (int index = 0; index < links.size(); index++) {
            Map.Entry<String, SocialLink> entry = links.get(index);
            SocialLink link = entry.getValue();

            NightItem icon = NightItem.fromType(link.material())
                .setDisplayName(link.display())
                .setLore(List.of(
                    GRAY.wrap(link.url()),
                    "",
                    GOLD.wrap("→ " + UNDERLINED.wrap("Click to get the link"))));

            list.add(MenuItem.builder()
                .defaultState(icon, actionContext -> {
                    Player clicker = actionContext.getPlayer();
                    clicker.closeInventory();
                    this.module.sendLink(clicker, link);
                })
                .slots(slots.get(index))
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
