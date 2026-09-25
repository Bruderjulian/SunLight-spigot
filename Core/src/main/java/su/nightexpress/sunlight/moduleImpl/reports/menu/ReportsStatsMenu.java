package su.nightexpress.sunlight.moduleImpl.reports.menu;

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
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportsStats;

import java.util.List;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Read-only overview of the queue, derived from the in-memory repository on each open.
 * <p>
 * Nothing here is persisted, deliberately: every number is recomputable from the reports table, so
 * a table of statistics would only ever be able to disagree with the truth.
 */
public class ReportsStatsMenu extends AbstractMenu {

    private final ReportsModule module;

    public ReportsStatsMenu(ReportsModule module) {
        super(MenuType.GENERIC_9X5, BLACK.wrap("Report Statistics"));
        this.module = module;
    }

    public boolean open(Player player) {
        return this.show(player);
    }

    @Override
    protected String getRawTitle(ViewerContext context) {
        return PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_MODE, () -> "Overview")
                .build()
                .apply(super.getRawTitle(context));
    }

    @Override
    public void registerActions() {

    }

    @Override
    public void registerConditions() {

    }

    @Override
    public void defineDefaultLayout() {
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, java.util.stream.IntStream.range(0, 9).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, java.util.stream.IntStream.range(36, 45)
                .toArray());

        this.addDefaultButton("back", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.ARROW)
                                .setDisplayName(ReportsLang.BUTTON_BACK.text()))
                        .action(context -> context.getViewer().closeMenu())
                        .build())
                .slots(40)
                .build());
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
        ReportsStats stats = this.module.collectStats();

        list.add(this.item(Material.BOOK, 10, "Open", String.valueOf(stats.open())));
        list.add(this.item(Material.CLOCK, 11, "Claimed", String.valueOf(stats.claimed())));
        list.add(this.item(Material.LIME_DYE, 12, "Resolved", String.valueOf(stats.resolved())));
        list.add(this.item(Material.RED_DYE, 13, "Denied", String.valueOf(stats.denied())));
        list.add(this.item(Material.GRAY_DYE, 14, "Expired", String.valueOf(stats.expired())));
        list.add(this.item(Material.PAPER, 15, "Concluded", String.valueOf(stats.concluded())));

        list.add(this.item(Material.LIME_CONCRETE, 19, "Resolution rate",
                percent(stats.resolutionRate())));
        list.add(this.item(Material.RED_CONCRETE, 20, "False positive rate",
                percent(stats.falsePositiveRate())));
        list.add(this.item(Material.SPYGLASS, 21, "Median time to claim",
                duration(stats.medianTimeToClaim())));
        list.add(this.item(Material.CLOCK, 22, "Median time to resolve",
                duration(stats.medianTimeToResolve())));
        list.add(this.item(Material.IRON_DOOR, 23, "Reporters opted out",
                String.valueOf(stats.reportersOptedOut())));

        this.addTopList(list, 28, Material.PLAYER_HEAD, "Most reported", stats.topTargets());
        this.addTopList(list, 29, Material.PAPER, "Most rewarded", stats.topReporters());

        if (stats.staff().isEmpty()) {
            list.add(this.item(Material.BARRIER, 32, "Staff activity", "none recorded"));
        } else {
            int slot = 31;
            for (ReportsStats.StaffEntry entry : stats.staff()) {
                if (slot > 34)
                    break;

                list.add(this.item(Material.PLAYER_HEAD, slot++,
                        entry.staffName() == null ? "unknown" : entry.staffName(),
                        entry.resolved() + " resolved, " + entry.denied() + " denied"));
            }
        }
    }

    private void addTopList(List<MenuItem> list, int slot, Material icon, String title,
            List<ReportsStats.TopEntry> entries) {
        if (entries.isEmpty()) {
            list.add(this.item(icon, slot, title, ReportsLang.STATS_EMPTY.text()));
            return;
        }

        List<String> lore = new java.util.ArrayList<>();
        for (ReportsStats.TopEntry entry : entries) {
            String name = entry.playerId() == null ? "unknown" : Bukkit.getOfflinePlayer(entry.playerId()).getName();
            lore.add(DARK_GRAY.wrap("• ") + WHITE.wrap(name == null ? "unknown" : name) + GRAY.wrap(" — "
                    + entry.count()));
        }
        list.add(MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(icon).setDisplayName(GOLD.wrap(title)).setLore(lore))
                        .build())
                .slots(slot)
                .build());
    }

    private MenuItem item(Material material, int slot, String name, String value) {
        return MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(material)
                                .setDisplayName(GOLD.wrap(name))
                                .setLore(Lists.newList(GRAY.wrap("» ") + WHITE.wrap(value)))
                                .hideAllComponents())
                        .build())
                .slots(slot)
                .build();
    }

    private static String percent(double value) {
        return Math.round(value * 100D) + "%";
    }

    private static String duration(long millis) {
        if (millis <= 0L)
            return "n/a";

        return TimeFormats.formatDuration(System.currentTimeMillis() + millis, TimeFormatType.LITERAL);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
