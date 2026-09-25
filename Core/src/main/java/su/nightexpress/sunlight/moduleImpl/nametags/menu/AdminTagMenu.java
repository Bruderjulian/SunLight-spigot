package su.nightexpress.sunlight.moduleImpl.nametags.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.ui.inventory.action.ActionContext;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.dialog.DefinitionFieldDialog;
import su.nightexpress.sunlight.moduleImpl.nametags.model.PriceMode;
import su.nightexpress.sunlight.moduleImpl.nametags.model.SubscriptionPeriod;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagAccessMode;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;
import su.nightexpress.sunlight.utils.EconomyUtils;

import java.util.List;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Admin editor for a single tag. Every configurable property is editable here, so no tag
 * behaviour requires a manual config edit.
 */
public class AdminTagMenu extends AbstractMenu {

    private static final int SLOT_DISPLAY = 10;
    private static final int SLOT_DESCRIPTION = 11;
    private static final int SLOT_PREFIX = 12;
    private static final int SLOT_SUFFIX = 13;
    private static final int SLOT_COLOR = 14;
    private static final int SLOT_ICON = 15;
    private static final int SLOT_ACCESS = 16;
    private static final int SLOT_PRICE = 17;
    private static final int SLOT_PERIOD = 22;
    private static final int SLOT_PERMISSION = 21;
    private static final int SLOT_GLOW = 23;
    private static final int SLOT_SORT = 24;
    private static final int SLOT_PRICE_MODE = 25;

    private final SunLightPlugin plugin;
    private final NametagsModule module;
    private final String tagId;

    public AdminTagMenu(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module, @NotNull String tagId) {
        super(MenuType.GENERIC_9X3, NametagsLang.MENU_ADMIN_TAG_TITLE.text());
        this.plugin = plugin;
        this.module = module;
        this.tagId = tagId;
    }

    public boolean open(Player player) {
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
    protected void onLoad(su.nightexpress.nightcore.config.FileConfig config) {

    }

    @Override
    protected void onClick(ViewerContext context, org.bukkit.event.inventory.InventoryClickEvent event) {

    }

    @Override
    protected void onDrag(ViewerContext context, org.bukkit.event.inventory.InventoryDragEvent event) {

    }

    @Override
    protected void onClose(ViewerContext context, org.bukkit.event.inventory.InventoryCloseEvent event) {

    }

    @Override
    public void onPrepare(ViewerContext context, InventoryView view, Inventory inventory, List<MenuItem> list) {
        TagDefinition tag = this.module.getCatalog().getTag(this.tagId);
        if (tag == null) {
            context.getPlayer().closeInventory();
            return;
        }

        list.add(this.textItem(SLOT_DISPLAY, Material.NAME_TAG, tag.getDisplay(),
            NametagsLang.MENU_FIELD_DISPLAY.text(),
            player -> this.openField(player, tag, "display", NametagsLang.MENU_FIELD_DISPLAY.text(),
                tag.getDisplay(), TagDefinition::setDisplay)));
        list.add(this.textItem(SLOT_DESCRIPTION, Material.PAPER, NametagsLang.MENU_FIELD_DESCRIPTION.text(),
            NametagsLang.MENU_FIELD_DESCRIPTION.text(),
            player -> this.openListField(player, tag, "description", NametagsLang.MENU_FIELD_DESCRIPTION.text(),
                tag.getDescription(), tag::setDescription)));
        list.add(this.textItem(SLOT_PREFIX, Material.GRAY_DYE, tag.getPrefix(), NametagsLang.MENU_FIELD_PREFIX.text(),
            player -> this.openField(player, tag, "prefix", NametagsLang.MENU_FIELD_PREFIX.text(),
                tag.getPrefix(), TagDefinition::setPrefix)));
        list.add(this.textItem(SLOT_SUFFIX, Material.ORANGE_DYE, tag.getSuffix(), NametagsLang.MENU_FIELD_SUFFIX.text(),
            player -> this.openField(player, tag, "suffix", NametagsLang.MENU_FIELD_SUFFIX.text(),
                tag.getSuffix(), TagDefinition::setSuffix)));
        list.add(this.textItem(SLOT_COLOR, Material.WHITE_DYE, tag.getColor(), NametagsLang.MENU_FIELD_COLOR.text(),
            player -> this.openField(player, tag, "color", NametagsLang.MENU_FIELD_COLOR.text(),
                tag.getColor(), TagDefinition::setColor)));
        list.add(this.textItem(SLOT_ICON, Material.ITEM_FRAME, tag.getIconMaterial(), NametagsLang.MENU_FIELD_ICON.text(),
            player -> this.openField(player, tag, "icon", NametagsLang.MENU_FIELD_ICON.text(),
                tag.getIconMaterial(), TagDefinition::setIconMaterial)));
        list.add(this.textItem(SLOT_PERMISSION, Material.PAPER, tag.getPermission(), NametagsLang.MENU_FIELD_PERMISSION.text(),
            player -> this.openField(player, tag, "permission", NametagsLang.MENU_FIELD_PERMISSION.text(),
                tag.getPermission(), TagDefinition::setPermission)));

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BOOK)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_FIELD_ACCESS_MODE.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(pretty(tag.getAccessMode())), "",
                        GRAY.wrap(NametagsLang.MENU_CLICK_CYCLE.text()))),
                ctx -> this.cycleAccess(ctx.getPlayer(), tag))
            .slots(SLOT_ACCESS)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.PLAYER_HEAD)
                    .setDisplayName(GOLD.wrap(NametagsLang.MENU_FIELD_PRICE.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(EconomyUtils.format(tag.getPrice())),
                        "",
                        GRAY.wrap(NametagsLang.MENU_CLICK_LEFT.text() + " ") + WHITE.wrap("-100"),
                        GRAY.wrap(NametagsLang.MENU_CLICK_RIGHT.text() + " ") + WHITE.wrap("+100"),
                        GRAY.wrap(NametagsLang.MENU_CLICK_SHIFT_RIGHT.text() + " ") + WHITE.wrap(
                            NametagsLang.MENU_VALUE_DOUBLE.text()))),
                ctx -> this.adjustPrice(ctx, tag))
            .slots(SLOT_PRICE)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.CLOCK)
                    .setDisplayName(LIGHT_PURPLE.wrap(NametagsLang.MENU_FIELD_PERIOD.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(tag.getSubscriptionPeriod().getId()),
                        "",
                        tag.isSubscription() ? GRAY.wrap(NametagsLang.MENU_CLICK_CYCLE.text())
                            : GRAY.wrap(NametagsLang.MENU_SUBSCRIPTIONS_ONLY.text()))),
                ctx -> this.cyclePeriod(ctx.getPlayer(), tag))
            .slots(SLOT_PERIOD)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.EMERALD)
                    .setDisplayName(WHITE.wrap(NametagsLang.MENU_FIELD_PRICE_MODE.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(pretty(tag.getPriceMode())),
                        "",
                        GRAY.wrap(NametagsLang.MENU_PRICE_MODE_INTERNAL.text()),
                        GRAY.wrap(NametagsLang.MENU_PRICE_MODE_EXTERNAL.text()),
                        "",
                        GRAY.wrap(NametagsLang.MENU_CLICK_TOGGLE.text()))),
                ctx -> this.togglePriceMode(ctx.getPlayer(), tag))
            .slots(SLOT_PRICE_MODE)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BLAZE_POWDER)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_FIELD_GLOW.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_FIELD_GLOW_HINT.text() + " ") + tag.isGlow(),
                        "",
                        GRAY.wrap(NametagsLang.MENU_CLICK_TOGGLE.text()))),
                ctx -> this.toggleGlow(ctx.getPlayer(), tag))
            .slots(SLOT_GLOW)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.COMPARATOR)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_FIELD_SORT.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(String.valueOf(tag.getSortPriority())),
                        "",
                        GRAY.wrap(NametagsLang.MENU_SORT_HINT.text()),
                        GRAY.wrap(NametagsLang.MENU_CLICK_LEFT.text() + " ") + WHITE.wrap("-1"),
                        GRAY.wrap(NametagsLang.MENU_CLICK_RIGHT.text() + " ") + WHITE.wrap("+1"))),
                ctx -> this.adjustSort(ctx, tag))
            .slots(SLOT_SORT)
            .build());
    }

    private @NotNull MenuItem textItem(int slot, @NotNull Material icon, @NotNull String value,
            @NotNull String description, @NotNull java.util.function.Consumer<Player> action) {
        return MenuItem.builder()
            .defaultState(NightItem.fromType(icon)
                    .setDisplayName(WHITE.wrap(value.isBlank() ? NametagsLang.MENU_VALUE_NONE.text() : value))
                    .setLore(List.of(description, "", GRAY.wrap(NametagsLang.MENU_CLICK_EDIT.text()))),
                ctx -> action.accept(ctx.getPlayer()))
            .slots(slot)
            .build();
    }

    private void openField(@NotNull Player player, @NotNull TagDefinition tag,
            @NotNull String field, @NotNull String title, @NotNull String value,
            @NotNull java.util.function.BiConsumer<TagDefinition, String> applier
    ) {
        new DefinitionFieldDialog<>(this.module, field, title, value, null, applier, this::commitTag)
                .show(player, tag, null);
    }

    private void openListField(@NotNull Player player, @NotNull TagDefinition tag,
            @NotNull String field, @NotNull String title, @NotNull List<String> values,
            @NotNull java.util.function.Consumer<List<String>> applier
    ) {
        new DefinitionFieldDialog<TagDefinition>(this.module, field, title,
                DefinitionFieldDialog.joinList(values), NametagsLang.MENU_HINT_COMMA_SEPARATED.text(),
                (definition, raw) -> applier.accept(DefinitionFieldDialog.splitList(raw)),
                this::commitTag)
                        .show(player, tag, null);
    }

    private void cycleAccess(@NotNull Player player, @NotNull TagDefinition tag) {
        TagAccessMode[] values = TagAccessMode.values();
        int next = (tag.getAccessMode().ordinal() + 1) % values.length;

        tag.setAccessMode(values[next]);
        this.commit(player, tag);
    }

    private void cyclePeriod(@NotNull Player player, @NotNull TagDefinition tag) {
        if (!tag.isSubscription()) return;

        SubscriptionPeriod[] values = SubscriptionPeriod.values();
        int next = (tag.getSubscriptionPeriod().ordinal() + 1) % values.length;

        tag.setSubscriptionPeriod(values[next]);
        this.commit(player, tag);
    }

    private void togglePriceMode(@NotNull Player player, @NotNull TagDefinition tag) {
        tag.setPriceMode(tag.getPriceMode() == PriceMode.INTERNAL ? PriceMode.EXTERNAL : PriceMode.INTERNAL);
        this.commit(player, tag);
    }

    private void toggleGlow(@NotNull Player player, @NotNull TagDefinition tag) {
        tag.setGlow(!tag.isGlow());
        this.commit(player, tag);
    }

    private void adjustPrice(@NotNull ActionContext context, @NotNull TagDefinition tag) {
        ClickType click = context.getEvent().getClick();
        if (click == ClickType.SHIFT_RIGHT) {
            tag.setPrice(tag.getPrice() * 2D);
        } else if (click == ClickType.RIGHT) {
            tag.setPrice(tag.getPrice() + 100D);
        } else if (click == ClickType.LEFT) {
            tag.setPrice(tag.getPrice() - 100D);
        }
        this.commit(context.getPlayer(), tag);
    }

    private void adjustSort(@NotNull ActionContext context, @NotNull TagDefinition tag) {
        ClickType click = context.getEvent().getClick();
        if (click == ClickType.RIGHT) {
            tag.setSortPriority(tag.getSortPriority() + 1);
        } else if (click == ClickType.LEFT) {
            tag.setSortPriority(tag.getSortPriority() - 1);
        }
        this.commit(context.getPlayer(), tag);
    }

    private void commit(@NotNull Player player, @NotNull TagDefinition tag) {
        this.commitTag(tag);
        this.show(this.plugin, player);
    }

    private void commitTag(@NotNull TagDefinition tag) {
        this.module.getCatalog().putTag(tag);
        this.module.saveCatalog();
    }

    private static @NotNull String pretty(@NotNull Enum<?> value) {
        String name = value.name().toLowerCase(java.util.Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).replace('_', ' ');
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
