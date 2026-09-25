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
import su.nightexpress.nightcore.ui.inventory.action.ObjectActionContext;
import su.nightexpress.nightcore.ui.inventory.item.ItemPopulator;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsPerms;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportNote;

import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.moduleImpl.reports.ReportsPlaceholders.*;

public class ReportViewMenu extends AbstractObjectMenu<Report> {

    private final ReportsModule module;

    private ItemPopulator<ReportNote> notePopulator;

    public ReportViewMenu(ReportsModule module) {
        super(MenuType.GENERIC_9X3, ReportsLang.MENU_VIEW_TITLE.text(), Report.class);
        this.module = module;
    }

    public boolean open(Player player, Report report) {
        return this.show(player, report);
    }

    @Override
    protected String getRawTitle(ViewerContext context) {
        return PlaceholderContext.builder()
                .with(this.getObject(context).placeholders())
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
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(0, 9).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(18, 27).toArray());

        this.addDefaultButton("info", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.PAPER)
                                .setDisplayName(WHITE.wrap(REPORT_TARGET))
                                .setLore(Lists.newList(
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Status: ") + WHITE.wrap(REPORT_STATUS)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Category: ") + WHITE.wrap(REPORT_CATEGORY)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Reporter: ") + WHITE.wrap(REPORT_REPORTER)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Staff: ") + WHITE.wrap(REPORT_STAFF)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Filed: ") + WHITE.wrap(REPORT_DATE_CREATED)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Updated: ") + WHITE.wrap(REPORT_DATE_UPDATED)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Notes: ") + WHITE.wrap(REPORT_NOTES)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Reward: ") + WHITE.wrap(REPORT_REWARDED)),
                                        "",
                                        ITALIC.and(GRAY).wrap("\"" + REPORT_REASON + "\"")))
                                .hideAllComponents())
                        .displayModifier((context, item) -> item
                                .replace(builder -> builder.with(this.getObject(context).placeholders()))
                                .setMaterial(this.iconMaterial(context)))
                        .build())
                .slots(13)
                .build());

        this.addDefaultButton("claim", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.BEACON)
                                .setDisplayName(ReportsLang.BUTTON_CLAIM.text())
                                .setLore(Lists.newList(
                                        GRAY.wrap("Take responsibility so other staff"),
                                        GRAY.wrap("do not duplicate the work."),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to claim"))))
                                .hideAllComponents())
                        .action(this.createObjectAction(this::claim))
                        .build())
                .slots(11)
                .build());

        this.addDefaultButton("conclude", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.EMERALD_BLOCK)
                                .setDisplayName(ReportsLang.BUTTON_CONCLUDE.text())
                                .setLore(Lists.newList(
                                        GRAY.wrap("Resolve or deny this report and"),
                                        GRAY.wrap("optionally leave a note."),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to choose"))))
                                .hideAllComponents())
                        .action(this.createObjectAction(this::conclude))
                        .build())
                .slots(15)
                .build());

        this.addDefaultButton("teleport", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.ENDER_PEARL)
                                .setDisplayName(ReportsLang.BUTTON_TELEPORT.text())
                                .setLore(Lists.newList(
                                        GRAY.wrap("Teleport to the reported player."),
                                        GRAY.wrap("Warmups and safety checks apply."),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to teleport"))))
                                .hideAllComponents())
                        .action(this.createObjectAction(this::teleport))
                        .build())
                .slots(21)
                .build());

        this.addDefaultButton("note", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.WRITABLE_BOOK)
                                .setDisplayName(ReportsLang.BUTTON_NOTE.text())
                                .setLore(Lists.newList(
                                        GRAY.wrap("Append a permanent note to the trail."),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to write"))))
                                .hideAllComponents())
                        .action(this.createObjectAction(this::note))
                        .build())
                .slots(23)
                .build());

        this.addDefaultButton("back", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.ARROW)
                                .setDisplayName(ReportsLang.BUTTON_BACK.text()))
                        .action(this.createObjectAction(this::back))
                        .build())
                .slots(22)
                .build());
    }

    private void claim(ObjectActionContext<Report> context) {
        if (!context.getPlayer().hasPermission(ReportsPerms.COMMAND_REPORT_CLAIM))
            return;

        if (this.module.claim(context.getObject(), context.getPlayer())) {
            context.getViewer().refresh();
        }
    }

    private void conclude(ObjectActionContext<Report> context) {
        if (!context.getPlayer().hasPermission(ReportsPerms.COMMAND_REPORT_RESOLVE))
            return;

        this.module.openOutcomeDialog(context.getPlayer(), context.getObject(), context.getViewer()::refresh);
    }

    private void teleport(ObjectActionContext<Report> context) {
        if (!context.getPlayer().hasPermission(ReportsPerms.COMMAND_REPORT_TELEPORT))
            return;

        this.module.teleportToTarget(context.getPlayer(), context.getObject());
        context.getViewer().refresh();
    }

    private void note(ObjectActionContext<Report> context) {
        if (!context.getPlayer().hasPermission(ReportsPerms.COMMAND_REPORT_NOTE))
            return;

        this.module.openNoteDialog(context.getPlayer(), context.getObject(), context.getViewer()::refresh);
    }

    private void back(ObjectActionContext<Report> context) {
        context.getViewer().closeMenu();
    }

    /**
     * A report may target a player who never joined, in which case there is no UUID and therefore
     * no skull to render.
     */
    private Material iconMaterial(ViewerContext context) {
        return this.getObject(context).getTargetId() == null ? Material.PAPER : Material.PLAYER_HEAD;
    }

    @Override
    protected void onLoad(FileConfig config) {
        this.notePopulator = ItemPopulator.builder(ReportNote.class)
                .slots(IntStream.range(0, 9).toArray())
                .itemProvider((ctx, note) -> NightItem.fromType(Material.PAPER)
                        .setDisplayName(WHITE.wrap(ReportsLang.NOTE_LINE.text()
                                .replace(SLPlaceholders.GENERIC_NAME, note.getAuthorName())
                                .replace(SLPlaceholders.GENERIC_TIME,
                                        TimeFormats.formatSince(note.getDate(), TimeFormatType.LITERAL))))
                        .setLore(Lists.newList(GRAY.wrap(note.getText())))
                        .hideAllComponents())
                .build();
    }

    @Override
    protected void onClick(ViewerContext context, InventoryClickEvent event) {
        // Drop-click deletes, matching the Bans menus' convention. Deletion is a purge action, so
        // it is gated behind its own permission rather than reusing claim or resolve.
        if (event.getClick() != ClickType.DROP)
            return;

        Player player = context.getPlayer();
        if (!player.hasPermission(ReportsPerms.COMMAND_REPORT_DELETE))
            return;

        if (this.module.deleteReport(this.getObject(context), player)) {
            player.closeInventory();
        }
    }

    @Override
    protected void onDrag(ViewerContext context, InventoryDragEvent event) {

    }

    @Override
    protected void onClose(ViewerContext context, InventoryCloseEvent event) {

    }

    @Override
    public void onPrepare(ViewerContext context, InventoryView view, Inventory inventory, List<MenuItem> list) {
        this.notePopulator.populateTo(context,
                this.module.getNotesForViewer(context.getPlayer(), this.getObject(context)), list);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
