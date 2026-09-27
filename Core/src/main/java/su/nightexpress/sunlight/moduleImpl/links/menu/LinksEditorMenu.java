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
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksFiles;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksLang;
import su.nightexpress.sunlight.moduleImpl.links.dialog.LinksDialogKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class LinksEditorMenu extends AbstractMenu {

    private final LinksModule module;
    private final ItemPopulator<Link> linkPopulator;

    public LinksEditorMenu(SunLightPlugin plugin, LinksModule module) {
        super(plugin, MenuType.GENERIC_9X5, LinksLang.MENU_TITLE_EDITOR.text());
        this.module = module;

        this.linkPopulator = ItemPopulator.builder(Link.class)
                .slots(IntStream.range(9, 36).toArray())
                .itemProvider((context, link) -> {
                    List<String> lore = new ArrayList<>();
                    lore.add(DARK_GRAY.wrap("» ") + GRAY.wrap("ID: ") + WHITE.wrap(link.getId()));
                    lore.add(DARK_GRAY.wrap("» ") + GRAY.wrap("Clicks: ") + ORANGE.wrap(String.valueOf(
                            link.getClicks())));
                    if (!link.isEnabled()) {
                        lore.add(SOFT_RED.wrap("Disabled"));
                    }

                    return link.getIcon()
                            .hideAllComponents()
                            .setDisplayName(link.getDisplay())
                            .setLore(lore)
                            .replace(builder -> builder.with(link.placeholders()));
                })
                .actionProvider(link -> context -> {
                    Link fresh = this.module.getLink(link.getId());
                    if (fresh == null) {
                        context.getViewer().refresh();
                        return;
                    }
                    this.module.openLinkSettings(context.getPlayer(), fresh);
                })
                .build();

        this.addDefaultButton("create", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.WRITABLE_BOOK)
                                .localized(LinksLang.ICON_EDITOR_CREATE)
                                .hideAllComponents())
                        .action(context -> this.plugin.showDialog(context.getPlayer(), LinksDialogKeys.LINK_CREATION,
                                this.module, () -> context.getViewer().refresh()))
                        .build())
                .slots(40)
                .build());

        this.load(plugin, FileConfig.load(module.getLocalUIPath(), LinksFiles.FILE_MENU_EDITOR));
    }

    @Override
    public void registerActions() {

    }

    @Override
    public void registerConditions() {

    }

    @Override
    public void defineDefaultLayout() {
        // No background row: the pagination buttons own the bottom row, and a background pane on the
        // same slots would overlap them.
        this.addNextPageButton(44);
        this.addPreviousPageButton(36);
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
        // Every link is listed here, including disabled ones: the editor is the only place they are
        // visible at all.
        this.linkPopulator.populateTo(context, this.module.getAllLinks(), items);
    }

    @Override
    public void onReady(@NotNull ViewerContext context, @NotNull InventoryView view, @NotNull Inventory inventory) {

    }

    @Override
    public void onRender(@NotNull ViewerContext context, @NotNull InventoryView view, @NotNull Inventory inventory) {

    }
}
