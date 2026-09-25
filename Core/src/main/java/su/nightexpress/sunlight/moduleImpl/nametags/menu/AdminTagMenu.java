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
import su.nightexpress.sunlight.moduleImpl.nametags.dialog.TagFieldDialog;
import su.nightexpress.sunlight.moduleImpl.nametags.model.PriceMode;
import su.nightexpress.sunlight.moduleImpl.nametags.model.SubscriptionPeriod;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagAccessMode;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;
import su.nightexpress.sunlight.utils.EconomyUtils;

import java.util.List;
import java.util.Locale;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Admin editor for a single tag. Every configurable property is editable here, so no tag
 * behaviour requires a manual config edit.
 */
public class AdminTagMenu extends AbstractMenu {

    private static final int SLOT_DISPLAY = 10;
    private static final int SLOT_PREFIX = 11;
    private static final int SLOT_SUFFIX = 12;
    private static final int SLOT_COLOR = 13;
    private static final int SLOT_ICON = 14;
    private static final int SLOT_ACCESS = 15;
    private static final int SLOT_PRICE = 16;
    private static final int SLOT_PERIOD = 22;
    private static final int SLOT_PERMISSION = 21;
    private static final int SLOT_GLOW = 23;
    private static final int SLOT_SORT = 24;
    private static final int SLOT_PRICE_MODE = 25;

    private final SunLightPlugin plugin;
    private final NametagsModule module;
    private final String tagId;

    public AdminTagMenu(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module, @NotNull String tagId) {
        super(MenuType.GENERIC_9X3, DARK_PURPLE.wrap("Edit Tag"));
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
        TagDefinition tag = this.module.getCatalog().getTag(this.tagId);
        if (tag == null) {
            context.getPlayer().closeInventory();
            return;
        }

        list.add(this.textItem(SLOT_DISPLAY, Material.NAME_TAG, tag.getDisplay(), GRAY.wrap("Display name"),
            player -> this.openField(player, tag, "display", "Display", tag.getDisplay())));
        list.add(this.textItem(SLOT_PREFIX, Material.GRAY_DYE, tag.getPrefix(), GRAY.wrap("Prefix"),
            player -> this.openField(player, tag, "prefix", "Prefix", tag.getPrefix())));
        list.add(this.textItem(SLOT_SUFFIX, Material.ORANGE_DYE, tag.getSuffix(), GRAY.wrap("Suffix"),
            player -> this.openField(player, tag, "suffix", "Suffix", tag.getSuffix())));
        list.add(this.textItem(SLOT_COLOR, Material.WHITE_DYE, tag.getColor(), GRAY.wrap("Name colour"),
            player -> this.openField(player, tag, "color", "Colour", tag.getColor())));
        list.add(this.textItem(SLOT_ICON, Material.ITEM_FRAME, tag.getIconMaterial(), GRAY.wrap("GUI icon material"),
            player -> this.openField(player, tag, "icon", "Icon", tag.getIconMaterial())));
        list.add(this.textItem(SLOT_PERMISSION, Material.NAME_TAG, tag.getPermission(),
            GRAY.wrap("Extra permission"),
            player -> this.openField(player, tag, "permission", "Permission", tag.getPermission())));

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BOOK)
                    .setDisplayName(YELLOW.wrap("Access mode"))
                    .setLore(java.util.List.of(GRAY.wrap("Current: ") + WHITE.wrap(pretty(tag.getAccessMode())), "", GRAY.wrap("Click to cycle"))),
                ctx -> this.cycleAccess(ctx.getPlayer(), tag))
            .slots(SLOT_ACCESS)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.PLAYER_HEAD)
                    .setDisplayName(GOLD.wrap("Price"))
                    .setLore(java.util.List.of(
                        GRAY.wrap("Current: ") + WHITE.wrap(EconomyUtils.format(tag.getPrice())),
                        "",
                        GRAY.wrap("Left click: ") + WHITE.wrap("-100"),
                        GRAY.wrap("Right click: ") + WHITE.wrap("+100"),
                        GRAY.wrap("Shift + right: ") + WHITE.wrap("double"))),
                ctx -> this.adjustPrice(ctx, tag))
            .slots(SLOT_PRICE)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.CLOCK)
                    .setDisplayName(LIGHT_PURPLE.wrap("Billing period"))
                    .setLore(java.util.List.of(
                        GRAY.wrap("Current: ") + WHITE.wrap(tag.getSubscriptionPeriod().getId()),
                        "",
                        tag.isSubscription() ? GRAY.wrap("Click to cycle") : GRAY.wrap("Only used by subscriptions"))),
                ctx -> this.cyclePeriod(ctx.getPlayer(), tag))
            .slots(SLOT_PERIOD)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.EMERALD)
                    .setDisplayName(WHITE.wrap("Price mode"))
                    .setLore(java.util.List.of(
                        GRAY.wrap("Current: ") + WHITE.wrap(pretty(tag.getPriceMode())),
                        "",
                        GRAY.wrap("INTERNAL: SunLight handles the payment"),
                        GRAY.wrap("EXTERNAL: another plugin owns the grant"),
                        "",
                        GRAY.wrap("Click to toggle"))),
                ctx -> this.togglePriceMode(ctx.getPlayer(), tag))
            .slots(SLOT_PRICE_MODE)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BLAZE_POWDER)
                    .setDisplayName(YELLOW.wrap("Glow colour"))
                    .setLore(java.util.List.of(
                        GRAY.wrap("Allow the glow module to colour this tag: ") + tag.isGlow(),
                        "",
                        GRAY.wrap("Click to toggle"))),
                ctx -> this.toggleGlow(ctx.getPlayer(), tag))
            .slots(SLOT_GLOW)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.COMPARATOR)
                    .setDisplayName(YELLOW.wrap("Sort priority"))
                    .setLore(java.util.List.of(
                        GRAY.wrap("Current: ") + WHITE.wrap(String.valueOf(tag.getSortPriority())),
                        "",
                        GRAY.wrap("Higher sorts first in the player menu"),
                        GRAY.wrap("Left click: ") + WHITE.wrap("-1"),
                        GRAY.wrap("Right click: ") + WHITE.wrap("+1"))),
                ctx -> this.adjustSort(ctx, tag))
            .slots(SLOT_SORT)
            .build());
    }

    private @NotNull MenuItem textItem(int slot, @NotNull Material icon, @NotNull String value,
            @NotNull String description, @NotNull java.util.function.Consumer<Player> action) {
        return MenuItem.builder()
            .defaultState(NightItem.fromType(icon)
                    .setDisplayName(WHITE.wrap(value.isBlank() ? "none" : value))
                    .setLore(java.util.List.of(description, "", GRAY.wrap("Click to edit"))),
                ctx -> action.accept(ctx.getPlayer()))
            .slots(slot)
            .build();
    }

    private void openField(@NotNull Player player, @NotNull TagDefinition tag,
            @NotNull String field, @NotNull String title, @NotNull String value) {
        new TagFieldDialog(this.module, field, title, value).show(player, tag, null);
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
        this.module.getCatalog().putTag(tag);
        this.module.saveCatalog();
        this.show(this.plugin, player);
    }

    private static @NotNull String pretty(@NotNull Enum<?> value) {
        String name = value.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).replace('_', ' ');
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
