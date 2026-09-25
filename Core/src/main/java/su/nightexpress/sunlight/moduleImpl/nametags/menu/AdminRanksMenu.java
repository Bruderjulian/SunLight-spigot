package su.nightexpress.sunlight.moduleImpl.nametags.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
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
import su.nightexpress.nightcore.ui.inventory.viewer.MenuViewer;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;
import su.nightexpress.sunlight.moduleImpl.nametags.model.RankDefinition;

import java.util.ArrayList;
import java.util.List;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Admin overview of every rank. Clicking a rank opens its editor, and the bottom row can
 * create a new one.
 */
public class AdminRanksMenu extends AbstractMenu {

    private static final int PAGE_SIZE = 28;
    private static final int FIRST_SLOT = 0;
    private static final int SLOT_CREATE = 40;
    private static final int SLOT_CLOSE = 41;

    private final SunLightPlugin plugin;
    private final NametagsModule module;

    public AdminRanksMenu(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module) {
        super(MenuType.GENERIC_9X5, NametagsLang.MENU_ADMIN_RANKS_TITLE.text());
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
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, 36, 37, 38, 39, 42, 43, 44);
        this.addNextPageItem(Material.ARROW, 39);
        this.addPreviousPageItem(Material.ARROW, 36);
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
        MenuViewer viewer = context.getViewer();

        List<RankDefinition> ranks = this.module.getCatalog().getRanksSorted();
        viewer.setTotalPages(Math.max(1, (ranks.size() + PAGE_SIZE - 1) / PAGE_SIZE));

        int fromIndex = (viewer.getCurrentPage() - 1) * PAGE_SIZE;
        if (fromIndex >= ranks.size()) return;

        int toIndex = Math.min(ranks.size(), fromIndex + PAGE_SIZE);
        for (int index = fromIndex; index < toIndex; index++) {
            RankDefinition rank = ranks.get(index);
            Material icon = Material.matchMaterial(rank.getIconMaterial());

            List<String> lore = new ArrayList<>(rank.getDescription());
            lore.add("");
            lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_ID.text() + " ") + WHITE.wrap(rank.getId()));
            lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_GROUPS.text() + " ") + WHITE.wrap(
                rank.getRanks().isEmpty() ? NametagsLang.MENU_VALUE_NONE.text() : String.join(", ", rank.getRanks())));
            lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_PRIORITY.text() + " ") + WHITE.wrap(String.valueOf(rank.getPriority())));
            if (rank.isDefault()) {
                lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_DEFAULT.text() + " ") + GREEN.wrap("✔"));
            }
            lore.add("");
            lore.add(GRAY.wrap(NametagsLang.MENU_RIGHT_CLICK_DELETE.text()));
            if (this.isReferenced(rank)) lore.add(GRAY.wrap(NametagsLang.MENU_DELETE_REFERENCED.text()));

            list.add(MenuItem.builder()
                .defaultState(NightItem.fromType(icon == null ? Material.PLAYER_HEAD : icon)
                        .setDisplayName(rank.getDisplay())
                        .setLore(lore),
                    ctx -> this.onClickRank(ctx.getPlayer(), rank,
                        ctx.getEvent().getClick() == ClickType.RIGHT))
                .slots(FIRST_SLOT + index - fromIndex)
                .build());
        }

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.LIME_DYE)
                    .setDisplayName(GREEN.wrap(NametagsLang.MENU_CREATE_RANK.text()))
                    .setLore(List.of(GRAY.wrap(NametagsLang.MENU_CREATE_RANK_HINT.text()))),
                ctx -> this.createRank(ctx.getPlayer()))
            .slots(SLOT_CREATE)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BARRIER)
                    .setDisplayName(RED.wrap(NametagsLang.MENU_CLOSE.text()))
                    .setLore(List.of(GRAY.wrap(NametagsLang.MENU_CLICK_CLOSE.text()))),
                ctx -> ctx.getPlayer().closeInventory())
            .slots(SLOT_CLOSE)
            .build());
    }

    /** Whether any profile still forces this rank, so the delete warning can say so. */
    private boolean isReferenced(@NotNull RankDefinition rank) {
        for (Profile profile : this.module.getCatalog().getProfiles()) {
            if (rank.getId().equals(profile.getRankId())) return true;
        }
        return false;
    }

    private void onClickRank(@NotNull Player player, @NotNull RankDefinition rank, boolean delete) {
        if (delete) {
            this.module.getCatalog().removeRank(rank.getId());
            this.module.saveCatalog();
            this.show(this.plugin, player);
            return;
        }
        new AdminRankMenu(this.plugin, this.module, rank.getId()).open(player);
    }

    private void createRank(@NotNull Player player) {
        String id = AdminTagsMenu.uniqueId("rank", this.module.getCatalog()::hasRank);
        RankDefinition rank = new RankDefinition(id);

        this.module.getCatalog().putRank(rank);
        this.module.saveCatalog();

        new AdminRankMenu(this.plugin, this.module, id).open(player);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
