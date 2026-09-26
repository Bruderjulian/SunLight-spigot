package su.nightexpress.sunlight.moduleImpl.links.menu;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.inventory.item.ItemPopulator;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinkDefaults;
import su.nightexpress.sunlight.moduleImpl.links.LinksFiles;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksLang;

import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.WHITE;

/**
 * A curated icon palette rather than a free-form item editor: it keeps the menu small and stops admins
 * from picking an item whose name and lore would clash with the link's own display name.
 */
public class LinkIconSelectionMenu extends AbstractObjectMenu<Link> {

    private static final int SLOT_RETURN = 31;

    private final LinksModule module;
    private final ItemPopulator<Material> iconPopulator;

    public LinkIconSelectionMenu(SunLightPlugin plugin, LinksModule module) {
        super(plugin, MenuType.GENERIC_9X4, LinksLang.MENU_TITLE_ICONS.text(), Link.class);
        this.module = module;

        this.iconPopulator = ItemPopulator.builder(Material.class)
                .slots(IntStream.range(0, 27).toArray())
                .itemProvider((context, material) -> NightItem.fromType(material).hideAllComponents())
                .actionProvider(material -> context -> {
                    Link link = this.getObject(context);
                    link.setIcon(NightItem.fromType(material));
                    this.module.saveLink(link);
                    this.module.openLinkSettings(context.getPlayer(), link);
                })
                .build();

        this.addDefaultButton("return", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.ARROW)
                                .setDisplayName(WHITE.wrap("Return"))
                                .hideAllComponents())
                        .action(context -> this.module.openLinkSettings(context.getPlayer(),
                                this.getObject(context)))
                        .build())
                .slots(SLOT_RETURN)
                .build());

        this.load(plugin, FileConfig.load(module.getLocalUIPath(), LinksFiles.FILE_MENU_ICONS));
    }

    @Override
    public void registerActions() {

    }

    @Override
    public void registerConditions() {

    }

    @Override
    public void defineDefaultLayout() {

    }

    @Override
    protected void onLoad(@NotNull FileConfig config) {

    }

    @Override
    protected void onClick(@NotNull ViewerContext context, @NotNull InventoryClickEvent event) {

    }

    @Override
    protected void onDrag(@NotNull ViewerContext context, @NotNull InventoryDragEvent event) {

    }

    @Override
    protected void onClose(@NotNull ViewerContext context, @NotNull InventoryCloseEvent event) {

    }

    @Override
    public void onPrepare(@NotNull ViewerContext context, @NotNull InventoryView view, @NotNull Inventory inventory,
            @NotNull List<MenuItem> items) {
        this.iconPopulator.populateTo(context, LinkDefaults.ICON_PALETTE, items);
    }

    @Override
    public void onReady(@NotNull ViewerContext context, @NotNull InventoryView view, @NotNull Inventory inventory) {

    }

    @Override
    public void onRender(@NotNull ViewerContext context, @NotNull InventoryView view, @NotNull Inventory inventory) {

    }
}
