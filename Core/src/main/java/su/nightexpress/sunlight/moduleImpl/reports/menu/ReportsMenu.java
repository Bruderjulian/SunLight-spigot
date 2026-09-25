package su.nightexpress.sunlight.moduleImpl.reports.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.inventory.item.ItemPopulator;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.profile.PlayerProfiles;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportFilter;

import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.moduleImpl.reports.ReportsPlaceholders.*;

public class ReportsMenu extends AbstractObjectMenu<ReportsMenu.Data> {

    private final ReportsModule module;

    private ItemPopulator<Report> populator;

    /**
     * The bound object is view state, not a database row, exactly as in the Bans punishments menu.
     */
    public record Data(ReportFilter filter, String targetName) {
    }

    public ReportsMenu(ReportsModule module) {
        super(MenuType.GENERIC_9X5, ReportsLang.MENU_LIST_TITLE.text(), Data.class);
        this.module = module;
    }

    public boolean open(Player player, ReportFilter filter) {
        return this.open(player, filter, null);
    }

    public boolean open(Player player, ReportFilter filter, String targetName) {
        return this.show(player, new Data(filter, targetName));
    }

    @Override
    protected String getRawTitle(ViewerContext context) {
        return PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_MODE, () -> ReportsLang.FILTER.getLocalized(this.getObject(context)
                        .filter()))
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
        this.addNextPageItem(Material.ARROW, 41);
        this.addPreviousPageItem(Material.ARROW, 39);

        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(0, 9).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(36, 45).toArray());

        this.addDefaultButton("filter", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.COMPARATOR)
                                .setDisplayName(ReportsLang.BUTTON_FILTER.text())
                                .setLore(Lists.newList(
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Selected: ") + WHITE.wrap(
                                                SLPlaceholders.GENERIC_MODE)),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to switch"))))
                                .hideAllComponents())
                        .displayModifier((context, item) -> item.replace(builder -> builder
                                .with(SLPlaceholders.GENERIC_MODE,
                                        () -> ReportsLang.FILTER.getLocalized(this.getObject(context).filter()))))
                        .action(context -> {
                            Data data = this.getObject(context);
                            // TARGET and CASE both need a subject the button has no way to supply:
                            // TARGET is reached by shift-clicking a row, CASE by opening it from a
                            // case. Cycling into either would land on a permanently empty view.
                            ReportFilter next = switch (data.filter()) {
                                case ALL -> ReportFilter.OPEN;
                                case TARGET, CASE -> ReportFilter.OPEN;
                                default -> nextOf(data.filter());
                            };
                            this.open(context.getPlayer(), next);
                        })
                        .build())
                .slots(43)
                .build());
    }

    private static ReportFilter nextOf(ReportFilter filter) {
        ReportFilter[] values = ReportFilter.values();
        return values[(filter.ordinal() + 1) % values.length];
    }

    @Override
    protected void onLoad(FileConfig config) {
        this.populator = ItemPopulator.builder(Report.class)
                .slots(IntStream.range(9, 36).toArray())
                .itemProvider((context, report) -> this.icon(report)
                        .hideAllComponents()
                        .setLore(Lists.newList(
                                DARK_GRAY.wrap("» " + GRAY.wrap("Status: ") + WHITE.wrap(REPORT_STATUS)),
                                DARK_GRAY.wrap("» " + GRAY.wrap("Category: ") + WHITE.wrap(REPORT_CATEGORY)),
                                DARK_GRAY.wrap("» " + GRAY.wrap("Reporter: ") + WHITE.wrap(REPORT_REPORTER)),
                                DARK_GRAY.wrap("» " + GRAY.wrap("Age: ") + WHITE.wrap(REPORT_AGE)),
                                "",
                                ITALIC.and(GRAY).wrap("\"" + REPORT_REASON + "\""),
                                "",
                                GOLD.wrap("→ " + UNDERLINED.wrap("Left click to inspect")),
                                DARK_GRAY.wrap("→ "
                                        + UNDERLINED.wrap("Shift + right click for all reports against them"))))
                        .replace(builder -> builder.with(report.placeholders())))
                .actionProvider(report -> actionContext -> {
                    InventoryClickEvent event = actionContext.getEvent();
                    Player player = actionContext.getPlayer();

                    if (event.getClick() == ClickType.SHIFT_RIGHT) {
                        this.open(player, ReportFilter.TARGET, report.getTargetName());
                        return;
                    }
                    if (event.isLeftClick()) {
                        this.module.openView(player, report);
                    }
                })
                .build();
    }

    private NightItem icon(Report report) {
        if (report.getTargetId() == null) {
            return NightItem.fromType(Material.PAPER).setDisplayName(WHITE.wrap(REPORT_TARGET));
        }
        return NightItem.fromType(Material.PLAYER_HEAD)
                .setPlayerProfile(PlayerProfiles.createProfile(report.getTargetId(), report.getTargetName()));
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
        Data data = this.getObject(context);

        this.populator.populateTo(context,
                this.module.getVisibleReports(viewer, data.filter(), data.targetName()), list);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
