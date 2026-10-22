package su.nightexpress.sunlight.moduleImpl.reports.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
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
import su.nightexpress.nightcore.util.profile.PlayerProfiles;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsPerms;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.CaseStatus;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportCase;

import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.moduleImpl.reports.ReportsPlaceholders.REPORT_DATE;
import static su.nightexpress.sunlight.moduleImpl.reports.ReportsPlaceholders.*;

/**
 * The staff-facing unit of work. Reports about one offender group together here, so five rows
 * about one griefer is one thing to work rather than five separate tickets.
 */
public class CaseViewMenu extends AbstractObjectMenu<ReportCase> {

    private final ReportsModule module;

    private ItemPopulator<Report> populator;

    public CaseViewMenu(ReportsModule module) {
        super(MenuType.GENERIC_9X5, BLACK.wrap("Case"), ReportCase.class);
        this.module = module;
    }

    public boolean open(Player player, ReportCase reportCase) {
        return this.show(player, reportCase);
    }

    @Override
    protected String getRawTitle(ViewerContext context) {
        return PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_TARGET, () -> this.getObject(context).getTargetName())
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
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(36, 45).toArray());

        this.addDefaultButton("info", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.PAPER)
                                .setDisplayName(WHITE.wrap(REPORT_TARGET))
                                .setLore(Lists.newList(
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Status: ") + WHITE.wrap(
                                                SLPlaceholders.GENERIC_STATUS)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Staff: ") + WHITE.wrap(
                                                SLPlaceholders.GENERIC_SOURCE)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Reports: ") + WHITE.wrap(
                                                SLPlaceholders.GENERIC_TOTAL)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Opened: ") + WHITE.wrap(
                                                REPORT_DATE)),
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Last activity: ") + WHITE.wrap(
                                                SLPlaceholders.GENERIC_TIME))))
                                .hideAllComponents())
                        .displayModifier((context, item) -> {
                            ReportCase reportCase = this.getObject(context);
                            item.replace(builder -> builder
                                    .with(SLPlaceholders.GENERIC_TARGET, reportCase::getTargetName)
                                    .with(SLPlaceholders.GENERIC_STATUS,
                                            () -> ReportsLang.CASE_STATUS.getLocalized(reportCase.getStatus()))
                                    .with(SLPlaceholders.GENERIC_SOURCE, () -> reportCase.getStaffName() == null
                                            ? "-" : reportCase.getStaffName())
                                    .with(SLPlaceholders.GENERIC_TOTAL,
                                            () -> String.valueOf(reportCase.getReportCount()))
                                    .with(REPORT_DATE,
                                            () -> TimeFormats.formatDateTime(reportCase.getCreateDate()))
                                    .with(SLPlaceholders.GENERIC_TIME,
                                            () -> TimeFormats.formatDateTime(reportCase.getUpdateDate())));
                            item.setMaterial(this.iconMaterial(context));
                        })
                        .build())
                .slots(4)
                .build());

        this.addDefaultButton("claim", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.BEACON)
                                .setDisplayName(ReportsLang.BUTTON_CLAIM.text())
                                .setLore(Lists.newList(
                                        GRAY.wrap("Take responsibility for the whole case."),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to claim"))))
                                .hideAllComponents())
                        .action(this.createObjectAction(this::claim))
                        .build())
                .slots(11)
                .build());

        this.addDefaultButton("resolve", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.EMERALD_BLOCK)
                                .setDisplayName(ReportsLang.BUTTON_CONCLUDE.text())
                                .setLore(Lists.newList(
                                        GRAY.wrap("Resolve the case as justified. Every report"),
                                        GRAY.wrap("still open inside it is resolved too."),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to resolve"))))
                                .hideAllComponents())
                        .action(this.createObjectAction(this::resolve))
                        .build())
                .slots(13)
                .build());

        this.addDefaultButton("deny", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.REDSTONE_BLOCK)
                                .setDisplayName(ReportsLang.BUTTON_DENY_CASE.text())
                                .setLore(Lists.newList(
                                        GRAY.wrap("Close the case as unjustified. No reward"),
                                        GRAY.wrap("is paid to anybody."),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to deny"))))
                                .hideAllComponents())
                        .action(this.createObjectAction(this::deny))
                        .build())
                .slots(15)
                .build());

        this.addDefaultButton("teleport", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.ENDER_PEARL)
                                .setDisplayName(ReportsLang.BUTTON_TELEPORT.text())
                                .setLore(Lists.newList(
                                        GRAY.wrap("Teleport to the reported player."),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to teleport"))))
                                .hideAllComponents())
                        .action(this.createObjectAction(this::teleport))
                        .build())
                .slots(30)
                .build());

        this.addDefaultButton("back", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.ARROW)
                                .setDisplayName(ReportsLang.BUTTON_BACK.text()))
                        .action(this.createObjectAction(this::back))
                        .build())
                .slots(40)
                .build());
    }

    private void claim(ObjectActionContext<ReportCase> context) {
        if (!context.getPlayer().hasPermission(ReportsPerms.COMMAND_REPORT_CLAIM))
            return;

        if (this.module.claimCase(context.getObject(), context.getPlayer())) {
            context.getViewer().refresh();
        }
    }

    private void resolve(ObjectActionContext<ReportCase> context) {
        if (!context.getPlayer().hasPermission(ReportsPerms.COMMAND_REPORT_RESOLVE))
            return;

        this.module.concludeCase(context.getObject(),
                CaseStatus.RESOLVED, context.getPlayer(), null);
        context.getViewer().refresh();
    }

    private void deny(ObjectActionContext<ReportCase> context) {
        if (!context.getPlayer().hasPermission(ReportsPerms.COMMAND_REPORT_DENY))
            return;

        this.module.concludeCase(context.getObject(),
                CaseStatus.DENIED, context.getPlayer(), null);
        context.getViewer().refresh();
    }

    private void teleport(ObjectActionContext<ReportCase> context) {
        if (!context.getPlayer().hasPermission(ReportsPerms.COMMAND_REPORT_TELEPORT))
            return;

        Player staff = context.getPlayer();
        for (Report report : this.module.getRepository().getReportsByCase(context.getObject().getId())) {
            if (this.module.teleportToTarget(staff, report)) {
                break;
            }
        }
        context.getViewer().refresh();
    }

    private void back(ObjectActionContext<ReportCase> context) {
        context.getViewer().closeMenu();
    }

    private su.nightexpress.sunlight.moduleImpl.reports.model.CaseStatus status(ViewerContext context) {
        return this.getObject(context).getStatus();
    }

    private Material iconMaterial(ViewerContext context) {
        return this.getObject(context).getTargetId() == null ? Material.PAPER : Material.PLAYER_HEAD;
    }

    @Override
    protected void onLoad(FileConfig config) {
        this.populator = ItemPopulator.builder(Report.class)
                .slots(IntStream.range(19, 34).toArray())
                .itemProvider((ctx, report) -> {
                    NightItem icon = report.getTargetId() == null ? NightItem.fromType(Material.PAPER)
                            : NightItem.fromType(Material.PLAYER_HEAD)
                            .setPlayerProfile(PlayerProfiles.createProfile(report.getTargetId(),
                                    report.getTargetName()));

                    return icon
                            .hideAllComponents()
                            .setDisplayName(WHITE.wrap(REPORT_TARGET))
                            .setLore(Lists.newList(
                                    DARK_GRAY.wrap("» " + GRAY.wrap("Status: ") + WHITE.wrap(REPORT_STATUS)),
                                    DARK_GRAY.wrap("» " + GRAY.wrap("Category: ") + WHITE.wrap(REPORT_CATEGORY)),
                                    DARK_GRAY.wrap("» " + GRAY.wrap("Reporter: ") + WHITE.wrap(REPORT_REPORTER)),
                                    DARK_GRAY.wrap("» " + GRAY.wrap("Age: ") + WHITE.wrap(REPORT_AGE)),
                                    "",
                                    ITALIC.and(GRAY).wrap("\"" + REPORT_REASON + "\"")))
                            .replace(builder -> builder.with(report.placeholders()));
                })
                .actionProvider(report -> actionContext -> {
                    if (actionContext.getEvent().isLeftClick()) {
                        this.module.openView(actionContext.getPlayer(), report);
                    }
                })
                .build();
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
        this.populator.populateTo(context,
                this.module.getRepository().getReportsByCase(this.getObject(context).getId()), list);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
