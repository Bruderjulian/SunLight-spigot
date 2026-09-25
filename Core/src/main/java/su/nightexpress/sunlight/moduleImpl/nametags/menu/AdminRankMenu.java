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
import su.nightexpress.nightcore.ui.inventory.action.ActionContext;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.dialog.DefinitionFieldDialog;
import su.nightexpress.sunlight.moduleImpl.nametags.model.RankDefinition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Admin editor for a single rank. Mirrors {@link AdminTagMenu} so an admin finds the same
 * layout whichever definition they are editing.
 */
public class AdminRankMenu extends AbstractMenu {

    private static final int SLOT_DISPLAY = 10;
    private static final int SLOT_DESCRIPTION = 11;
    private static final int SLOT_PREFIX = 12;
    private static final int SLOT_SUFFIX = 13;
    private static final int SLOT_COLOR = 14;
    private static final int SLOT_ICON = 15;
    private static final int SLOT_PRIORITY = 16;
    private static final int SLOT_GROUPS = 21;
    private static final int SLOT_DEFAULT = 23;

    private final SunLightPlugin plugin;
    private final NametagsModule module;
    private final String rankId;

    public AdminRankMenu(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module, @NotNull String rankId) {
        super(MenuType.GENERIC_9X3, NametagsLang.MENU_ADMIN_RANK_TITLE.text());
        this.plugin = plugin;
        this.module = module;
        this.rankId = rankId;
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
        RankDefinition rank = this.module.getCatalog().getRank(this.rankId);
        if (rank == null) {
            context.getPlayer().closeInventory();
            return;
        }

        list.add(this.textItem(SLOT_DISPLAY, Material.PLAYER_HEAD, rank.getDisplay(),
            NametagsLang.MENU_FIELD_DISPLAY.text(),
            player -> this.openField(player, rank, "rank_display", NametagsLang.MENU_FIELD_DISPLAY.text(),
                rank.getDisplay(), RankDefinition::setDisplay)));
        list.add(this.textItem(SLOT_DESCRIPTION, Material.PAPER, NametagsLang.MENU_FIELD_DESCRIPTION.text(),
            NametagsLang.MENU_FIELD_DESCRIPTION.text(),
            player -> this.openListField(player, rank, "rank_description", NametagsLang.MENU_FIELD_DESCRIPTION.text(),
                rank.getDescription(), rank::setDescription)));
        list.add(this.textItem(SLOT_PREFIX, Material.GRAY_DYE, rank.getPrefix(), NametagsLang.MENU_FIELD_PREFIX.text(),
            player -> this.openField(player, rank, "rank_prefix", NametagsLang.MENU_FIELD_PREFIX.text(),
                rank.getPrefix(), RankDefinition::setPrefix)));
        list.add(this.textItem(SLOT_SUFFIX, Material.ORANGE_DYE, rank.getSuffix(), NametagsLang.MENU_FIELD_SUFFIX.text(),
            player -> this.openField(player, rank, "rank_suffix", NametagsLang.MENU_FIELD_SUFFIX.text(),
                rank.getSuffix(), RankDefinition::setSuffix)));
        list.add(this.textItem(SLOT_COLOR, Material.WHITE_DYE, rank.getColor(), NametagsLang.MENU_FIELD_COLOR.text(),
            player -> this.openField(player, rank, "rank_color", NametagsLang.MENU_FIELD_COLOR.text(),
                rank.getColor(), RankDefinition::setColor)));
        list.add(this.textItem(SLOT_ICON, Material.ITEM_FRAME, rank.getIconMaterial(), NametagsLang.MENU_FIELD_ICON.text(),
            player -> this.openField(player, rank, "rank_icon", NametagsLang.MENU_FIELD_ICON.text(),
                rank.getIconMaterial(), RankDefinition::setIconMaterial)));

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.COMPARATOR)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_FIELD_PRIORITY.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(String.valueOf(rank.getPriority())),
                        "",
                        GRAY.wrap(NametagsLang.MENU_SORT_HINT.text()),
                        GRAY.wrap(NametagsLang.MENU_CLICK_LEFT.text() + " ") + WHITE.wrap("-1"),
                        GRAY.wrap(NametagsLang.MENU_CLICK_RIGHT.text() + " ") + WHITE.wrap("+1"))),
                ctx -> this.adjustPriority(ctx, rank))
            .slots(SLOT_PRIORITY)
            .build());

        list.add(this.textItem(SLOT_GROUPS, Material.SKELETON_SKULL, this.groupsOf(rank),
            NametagsLang.MENU_GROUPS_HINT.text(),
            player -> this.openListField(player, rank, "rank_groups", NametagsLang.MENU_FIELD_GROUPS.text(),
                new ArrayList<>(rank.getRanks()), values -> rank.setRanks(new LinkedHashSet<>(values)))));

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.NAME_TAG)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_FIELD_DEFAULT.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + NametagsLang.COMMAND_TAG_ENABLED.get(rank.isDefault()),
                        GRAY.wrap(NametagsLang.MENU_DEFAULT_HINT.text()),
                        "",
                        GRAY.wrap(NametagsLang.MENU_CLICK_TOGGLE.text()))),
                ctx -> this.toggleDefault(ctx.getPlayer(), rank))
            .slots(SLOT_DEFAULT)
            .build());
    }

    private @NotNull String groupsOf(@NotNull RankDefinition rank) {
        if (rank.getRanks().isEmpty()) return NametagsLang.MENU_VALUE_NONE.text();
        return String.join(", ", rank.getRanks());
    }

    private @NotNull MenuItem textItem(int slot, @NotNull Material icon, @NotNull String value,
            @NotNull String description, @NotNull Consumer<Player> action) {
        return MenuItem.builder()
            .defaultState(NightItem.fromType(icon)
                    .setDisplayName(WHITE.wrap(value.isBlank() ? NametagsLang.MENU_VALUE_NONE.text() : value))
                    .setLore(List.of(description, "", GRAY.wrap(NametagsLang.MENU_CLICK_EDIT.text()))),
                ctx -> action.accept(ctx.getPlayer()))
            .slots(slot)
            .build();
    }

    private void openField(@NotNull Player player, @NotNull RankDefinition rank,
            @NotNull String field, @NotNull String title, @NotNull String value,
            @NotNull java.util.function.BiConsumer<RankDefinition, String> applier
    ) {
        new DefinitionFieldDialog<>(this.module, field, title, value, null, applier, this::commitRank)
                .show(player, rank, null);
    }

    private void openListField(@NotNull Player player, @NotNull RankDefinition rank,
            @NotNull String field, @NotNull String title, @NotNull List<String> values,
            @NotNull Consumer<List<String>> applier
    ) {
        new DefinitionFieldDialog<RankDefinition>(this.module, field, title,
                DefinitionFieldDialog.joinList(values), NametagsLang.MENU_HINT_COMMA_SEPARATED.text(),
                (definition, raw) -> applier.accept(DefinitionFieldDialog.splitList(raw)),
                this::commitRank)
                        .show(player, rank, null);
    }

    private void adjustPriority(@NotNull ActionContext context, @NotNull RankDefinition rank) {
        ClickType click = context.getEvent().getClick();
        if (click == ClickType.RIGHT) {
            rank.setPriority(rank.getPriority() + 1);
        } else if (click == ClickType.LEFT) {
            rank.setPriority(rank.getPriority() - 1);
        }
        this.commit(context.getPlayer(), rank);
    }

    private void toggleDefault(@NotNull Player player, @NotNull RankDefinition rank) {
        rank.setDefault(!rank.isDefault());
        this.commit(player, rank);
    }

    private void commit(@NotNull Player player, @NotNull RankDefinition rank) {
        this.commitRank(rank);
        this.show(this.plugin, player);
    }

    private void commitRank(@NotNull RankDefinition rank) {
        this.module.getCatalog().putRank(rank);
        this.module.saveCatalog();
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
