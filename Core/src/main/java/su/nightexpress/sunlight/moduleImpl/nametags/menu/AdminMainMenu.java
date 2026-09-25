package su.nightexpress.sunlight.moduleImpl.nametags.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;

import java.util.List;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Landing menu for {@code /nametag admin}. The module has three editable definition kinds
 * and this is where an admin picks between them.
 */
public class AdminMainMenu extends AbstractMenu {

    private static final int SLOT_TAGS = 11;
    private static final int SLOT_RANKS = 13;
    private static final int SLOT_PROFILES = 15;
    private static final int SLOT_CLOSE = 22;

    private final SunLightPlugin plugin;
    private final NametagsModule module;

    public AdminMainMenu(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module) {
        super(MenuType.GENERIC_9X3, NametagsLang.MENU_ADMIN_TITLE.text());
        this.plugin = plugin;
        this.module = module;
    }

    public boolean open(@NotNull Player player) {
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
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                17, 18, 19, 20, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35);
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
        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.NAME_TAG)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_HUB_TAGS.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(String.valueOf(
                            this.module.getCatalog().getTags().size())),
                        "",
                        GRAY.wrap(NametagsLang.MENU_HUB_TAGS_HINT.text()))),
                ctx -> new AdminTagsMenu(this.plugin, this.module).open(ctx.getPlayer()))
            .slots(SLOT_TAGS)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.PLAYER_HEAD)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_HUB_RANKS.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(String.valueOf(
                            this.module.getCatalog().getRanks().size())),
                        "",
                        GRAY.wrap(NametagsLang.MENU_HUB_RANKS_HINT.text()))),
                ctx -> new AdminRanksMenu(this.plugin, this.module).open(ctx.getPlayer()))
            .slots(SLOT_RANKS)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BOOK)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_HUB_PROFILES.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(String.valueOf(
                            this.module.getCatalog().getProfiles().size())),
                        "",
                        GRAY.wrap(NametagsLang.MENU_HUB_PROFILES_HINT.text()))),
                ctx -> new AdminProfilesMenu(this.plugin, this.module).open(ctx.getPlayer()))
            .slots(SLOT_PROFILES)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BARRIER)
                    .setDisplayName(RED.wrap(NametagsLang.MENU_CLOSE.text()))
                    .setLore(List.of(GRAY.wrap(NametagsLang.MENU_CLICK_CLOSE.text()))),
                ctx -> ctx.getPlayer().closeInventory())
            .slots(SLOT_CLOSE)
            .build());
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
