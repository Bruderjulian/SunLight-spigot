package su.nightexpress.sunlight.moduleImpl.links.menu;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.inventory.item.ItemPopulator;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksFiles;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksLang;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class LinksMenu extends AbstractMenu {

    private final LinksModule module;
    private final ItemPopulator<Link> linkPopulator;

    public LinksMenu(SunLightPlugin plugin, LinksModule module) {
        super(plugin, MenuType.GENERIC_9X5, LinksLang.MENU_TITLE_LIST.text());
        this.module = module;

        this.linkPopulator = ItemPopulator.builder(Link.class)
                .slots(IntStream.range(9, 36).toArray())
                .itemProvider((context, link) -> {
                    // Built by hand rather than from an IconLocale: the lore has to show the link's own
                    // URL and command, which a static icon definition cannot express.
                    List<String> lore = new ArrayList<>();
                    if (link.hasUrl()) {
                        lore.add(GRAY.wrap(link.getUrl()));
                    }
                    if (link.hasCommand()) {
                        lore.add(GOLD.wrap("Runs ") + GRAY.wrap(link.getCommand()));
                    }
                    lore.add("");
                    lore.add(YELLOW.wrap("→ ") + UNDERLINED.wrap("Click to open"));

                    return link.getIcon()
                            .hideAllComponents()
                            .setDisplayName(link.getDisplay())
                            .setLore(lore)
                            .replace(builder -> builder.with(link.placeholders()));
                })
                .actionProvider(link -> context -> this.module.activateLink(context.getPlayer(), link))
                .build();

        this.load(plugin, FileConfig.load(module.getLocalUIPath(), LinksFiles.FILE_MENU_LIST));
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
        this.linkPopulator.populateTo(context, this.module.getVisibleLinks(context.getPlayer()), items);
    }

    @Override
    public void onReady(@NotNull ViewerContext context, @NotNull InventoryView view, @NotNull Inventory inventory) {

    }

    @Override
    public void onRender(@NotNull ViewerContext context, @NotNull InventoryView view, @NotNull Inventory inventory) {

    }
}
