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
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.profile.PlayerProfiles;
import su.nightexpress.sunlight.moduleImpl.reports.ReportsModule;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsLang;
import su.nightexpress.sunlight.moduleImpl.reports.config.ReportsPerms;

import java.util.ArrayList;
import java.util.List;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Step one of the guided flow: pick who to report. Only online players are listed, because a
 * menu cannot accept free text — the command form is still there for anyone reporting somebody by
 * name, and the review screen makes the target visible before anything is submitted.
 */
public class ReportTargetMenu extends AbstractMenu {

    private final ReportsModule module;

    public ReportTargetMenu(ReportsModule module) {
        super(MenuType.GENERIC_9X5, BLACK.wrap("Report a Player"));
        this.module = module;
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

        List<Player> targets = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(viewer))
                targets.add(online);
        }
        if (targets.isEmpty()) {
            this.addOfflineHint(list);
            return;
        }

        List<Integer> slots = new ArrayList<>();
        for (int row = 0; row * 9 < targets.size(); row++) {
            int inRow = Math.min(9, targets.size() - row * 9);
            int offset = (9 - inRow) / 2;
            for (int i = 0; i < inRow; i++) {
                slots.add(10 + row * 9 + offset + i);
            }
        }

        for (int index = 0; index < targets.size(); index++) {
            Player target = targets.get(index);
            int slot = slots.get(index);

            list.add(MenuItem.button()
                    .defaultState(NightItem.fromType(Material.PLAYER_HEAD)
                            .setPlayerProfile(PlayerProfiles.createProfile(target.getUniqueId(),
                                    target.getName()))
                            .setDisplayName(WHITE.wrap(target.getName()))
                            .setLore(List.of(
                                    GRAY.wrap("World: ") + DARK_GRAY.wrap(String.valueOf(target.getWorld().getName())),
                                    "",
                                    GOLD.wrap("→ " + UNDERLINED.wrap("Click to report"))))
                            .hideAllComponents(),
                            actionContext -> this.module.openCategoryDialog(actionContext.getPlayer(),
                                    target.getName()))
                    .slots(slot)
                    .build());
        }

        this.addOfflineHint(list);
    }

    /**
     * A menu can only list online players, so the pointer to the command form is unconditional
     * rather than permission-gated — there is no permission that unlocks reporting an offline
     * player, the command syntax simply can.
     */
    private void addOfflineHint(List<MenuItem> list) {
        list.add(MenuItem.button()
                .defaultState(NightItem.fromType(Material.PAPER)
                        .setDisplayName(ReportsLang.TARGET_MENU_OFFLINE.text())
                        .setLore(List.of(GRAY.wrap("/report <player> <category> <details>")))
                        .hideAllComponents())
                .slots(49)
                .build());
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
