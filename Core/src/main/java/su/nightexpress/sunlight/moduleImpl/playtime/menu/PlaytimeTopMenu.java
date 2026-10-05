package su.nightexpress.sunlight.moduleImpl.playtime.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
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
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeModule;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeModule.TopEntry;
import su.nightexpress.sunlight.moduleImpl.playtime.config.PlaytimeLang;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Paginated leaderboard with a period switcher on top.
 * <p>
 * One instance per open carries the selected period, so concurrent viewers
 * never share state.
 */
public class PlaytimeTopMenu extends AbstractMenu {

    private static final int PAGE_SIZE = 7;
    private static final int FIRST_SLOT = 19;

    private final PlaytimeModule module;
    private final PlaytimePeriod period;

    public PlaytimeTopMenu(PlaytimeModule module, PlaytimePeriod period) {
        super(MenuType.GENERIC_9X5, PlaytimeLang.MENU_TITLE_TOP.text());
        this.module = module;
        this.period = period == null ? PlaytimePeriod.ALLTIME : period;
    }

    public boolean open(SunLightPlugin plugin, Player viewer) {
        return this.show(plugin, viewer);
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
        this.addNextPageButton(41);
        this.addPreviousPageButton(39);
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
        Player viewer = context.getPlayer();
        MenuViewer menuViewer = context.getViewer();

        List<TopEntry> entries = new ArrayList<>(this.module.getTop(this.period));
        menuViewer.setTotalPages(Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE));

        this.addPeriodButton(list, PlaytimePeriod.ALLTIME, Material.NETHER_STAR, 1);
        this.addPeriodButton(list, PlaytimePeriod.YEAR, Material.ENDER_EYE, 2);
        this.addPeriodButton(list, PlaytimePeriod.MONTH, Material.MAP, 3);
        this.addPeriodButton(list, PlaytimePeriod.WEEK, Material.CLOCK, 4);
        this.addPeriodButton(list, PlaytimePeriod.DAY, Material.SUNFLOWER, 5);

        int fromIndex = (menuViewer.getCurrentPage() - 1) * PAGE_SIZE;
        if (fromIndex < entries.size()) {
            int toIndex = Math.min(entries.size(), fromIndex + PAGE_SIZE);
            for (int index = fromIndex; index < toIndex; index++) {
                TopEntry entry = entries.get(index);
                list.add(this.entryItem(entry, index + 1, viewer.getUniqueId(), FIRST_SLOT + index - fromIndex));
            }
        } else {
            list.add(MenuItem.button()
                .defaultState(NightItem.fromType(Material.BARRIER)
                    .setDisplayName(GRAY.wrap("No entries yet"))
                    .setLore(List.of(GRAY.wrap("Play for a while and come back!")))
                    .hideAllComponents())
                .slots(22)
                .build());
        }

        list.add(MenuItem.button()
            .defaultState(NightItem.fromType(Material.BOOK)
                .setDisplayName(GOLD.wrap(PlaytimeLang.MENU_BUTTON_STATS.text()))
                .setLore(List.of(GRAY.wrap("Back to your statistics."), "", GOLD.wrap("→ " + UNDERLINED.wrap("Click to open."))))
                .hideAllComponents(),
                actionContext -> {
                    Player clicker = actionContext.getPlayer();
                    this.module.openStatsMenu(clicker, this.module.userManager().getOrFetch(clicker));
                })
            .slots(40)
            .build());

        list.add(MenuItem.button()
            .defaultState(NightItem.fromType(Material.BARRIER)
                .setDisplayName(GRAY.wrap(PlaytimeLang.MENU_BUTTON_CLOSE.text()))
                .hideAllComponents(),
                actionContext -> actionContext.getViewer().closeMenu())
            .slots(44)
            .build());
    }

    private void addPeriodButton(List<MenuItem> list, PlaytimePeriod value, Material icon, int slot) {
        boolean selected = this.period == value;
        String label = this.periodLabel(value);
        List<String> lore = new ArrayList<>();
        lore.add(GRAY.wrap("Sort the board by " + label.toLowerCase() + " playtime."));
        lore.add("");
        lore.add(selected ? GREEN.wrap("Currently selected.") : GOLD.wrap("→ " + UNDERLINED.wrap("Click to select.")));

        list.add(MenuItem.button()
            .defaultState(NightItem.fromType(icon)
                .setDisplayName((selected ? GOLD : GRAY).wrap(label))
                .setLore(lore)
                .hideAllComponents(),
                actionContext -> new PlaytimeTopMenu(this.module, value).show(this.module.plugin(), actionContext.getPlayer()))
            .slots(slot)
            .build());
    }

    private MenuItem entryItem(TopEntry entry, int rank, UUID viewerId, int slot) {
        String medal = switch (rank) {
            case 1 -> GOLD.wrap("#" + rank + " ");
            case 2 -> WHITE.wrap("#" + rank + " ");
            case 3 -> ORANGE.wrap("#" + rank + " ");
            default -> GRAY.wrap("#" + rank + " ");
        };
        boolean self = entry.id().equals(viewerId);

        List<String> lore = new ArrayList<>();
        lore.add(GRAY.wrap("Playtime: ") + WHITE.wrap(this.module.format(entry.value())));
        lore.add(GRAY.wrap("Period: ") + WHITE.wrap(this.periodLabel(this.period)));
        if (self) {
            lore.add("");
            lore.add(GREEN.wrap("That's you!"));
        }

        NightItem icon = NightItem.fromType(Material.PLAYER_HEAD)
            .setDisplayName(medal + (self ? GREEN.wrap(entry.name()) : WHITE.wrap(entry.name())))
            .setLore(lore)
            .hideAllComponents();
        try {
            icon.setSkullOwner(Bukkit.getOfflinePlayer(entry.id()));
        } catch (Exception ignored) {
        }
        return MenuItem.button().defaultState(icon).slots(slot).build();
    }

    private String periodLabel(PlaytimePeriod period) {
        return period == PlaytimePeriod.ALLTIME ? "Alltime"
            : period.name().substring(0, 1) + period.name().substring(1).toLowerCase();
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
