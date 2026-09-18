package su.nightexpress.sunlight.module.playerwarps.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.IconLocale;
import su.nightexpress.nightcore.ui.inventory.action.MenuItemAction;
import su.nightexpress.nightcore.ui.inventory.action.ObjectActionContext;
import su.nightexpress.nightcore.ui.inventory.item.ItemPopulator;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.NightMessage;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.module.playerwarps.category.WarpCategory;
import su.nightexpress.sunlight.module.playerwarps.PlayerWarpsModule;
import su.nightexpress.sunlight.module.playerwarps.PlayerWarpsPlaceholders;
import su.nightexpress.sunlight.module.playerwarps.PlayerWarpRepository;
import su.nightexpress.sunlight.module.playerwarps.core.PlayerWarpsSettings;
import su.nightexpress.sunlight.module.playerwarps.PlayerWarp;

import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;
import static su.nightexpress.sunlight.module.warps.WarpsPlaceholders.*;

public class PlayerWarpsListMenu extends AbstractObjectMenu<WarpsListData> implements LangContainer {

    private static final IconLocale ICON_WARP_DEFAULT = LangEntry.iconBuilder("PlayerWarps.UI.WarpsMenu.WarpDefault")
            .rawName(WARP_NAME)
            .rawLore(
                    DARK_GRAY.wrap("» ") + GRAY.wrap("Owner: " + WHITE.wrap(PlayerWarpsPlaceholders.WARP_OWNER_NAME)),
                    DARK_GRAY.wrap("» ") + GRAY.wrap("Visits: " + WHITE.wrap(PlayerWarpsPlaceholders.WARP_VISITS)),
                    DARK_GRAY.wrap("» ") + GRAY.wrap("Description:"),
                    WARP_DESCRIPTION,
                    "",
                    GOLD.wrap("→ " + UNDERLINED.wrap("Click to teleport")))
            .build();

    private static final IconLocale ICON_WARP_OWN = LangEntry.iconBuilder("PlayerWarps.UI.WarpsMenu.WarpOwn")
            .rawName(WARP_NAME)
            .rawLore(
                    GREEN.wrap("✔ You own this warp."),
                    "",
                    DARK_GRAY.wrap("» ") + GRAY.wrap("Visits: " + WHITE.wrap(PlayerWarpsPlaceholders.WARP_VISITS)),
                    DARK_GRAY.wrap("» ") + GRAY.wrap("Description:"),
                    WARP_DESCRIPTION,
                    "",
                    GOLD.wrap("→ " + UNDERLINED.wrap("Left-Click to teleport")),
                    GOLD.wrap("→ " + UNDERLINED.wrap("Right-Click to edit")))
            .build();

    private static final IconLocale ICON_WARP_MODERATE = LangEntry.iconBuilder("PlayerWarps.UI.WarpsMenu.WarpModerate")
            .rawName(WARP_NAME)
            .rawLore(
                    GOLD.wrap("✎ You can edit this warp."),
                    "",
                    DARK_GRAY.wrap("» ") + GRAY.wrap("Owner: " + WHITE.wrap(PlayerWarpsPlaceholders.WARP_OWNER_NAME)),
                    DARK_GRAY.wrap("» ") + GRAY.wrap("Visits: " + WHITE.wrap(PlayerWarpsPlaceholders.WARP_VISITS)),
                    DARK_GRAY.wrap("» ") + GRAY.wrap("Description:"),
                    WARP_DESCRIPTION,
                    "",
                    GOLD.wrap("→ " + UNDERLINED.wrap("Left-Click to teleport")),
                    GOLD.wrap("→ " + UNDERLINED.wrap("Right-Click to edit")))
            .build();

    private final SunLightPlugin plugin;
    private final PlayerWarpsModule module;
    // private final PlayerWarpsSettings settings;
    private final PlayerWarpRepository repository;

    private final MenuItemAction backAction;
    private final MenuItemAction searchAction;
    private final MenuItemAction searchResetAction;
    private final MenuItemAction sortAction;

    private ItemPopulator<PlayerWarp> warpPopulator;

    public PlayerWarpsListMenu(SunLightPlugin plugin, PlayerWarpsModule module, PlayerWarpsSettings settings,
            PlayerWarpRepository repository) {
        super(MenuType.GENERIC_9X6, "[Warps - " + GENERIC_CATEGORY + "]", WarpsListData.class);
        this.plugin = plugin;
        this.module = module;
        // this.settings = settings;
        this.repository = repository;

        this.backAction = this.createObjectAction(this::backToMainMenu);
        this.searchAction = this.createObjectAction(this::searchWarps);
        this.searchResetAction = this.createObjectAction(this::resetSearch);
        this.sortAction = this.createObjectAction(this::toggleSorting);
    }

    @Override

    protected String getRawTitle(ViewerContext context) {
        return PlaceholderContext.builder().with(GENERIC_CATEGORY, () -> this.getObject(context).category().name())
                .build().apply(super.getRawTitle(context));
    }

    public boolean show(Player player, WarpCategory category, PlayerWarpSortType sortType, String searchText) {
        if (sortType == null)
            sortType = PlayerWarpSortType.DATE_CREATION;

        return this.show(this.plugin, player, new WarpsListData(category, sortType, searchText));
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
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(45, 54).toArray());

        this.addNextPageItem(Material.ARROW, 53);
        this.addPreviousPageItem(Material.ARROW, 45);

        this.addDefaultButton("back", MenuItem.builder()
                .defaultState(ItemState.defaultBuilder()
                        .icon(NightItem.fromType(Material.COMPASS)
                                .setDisplayName(WHITE.wrap("Back to Main Menu"))
                                .hideAllComponents())
                        .action(this.backAction)
                        .build())
                .slots(49)
                .build());

        this.addDefaultButton("search", MenuItem.builder()
                .defaultState(ItemState.defaultBuilder()
                        .icon(NightItem.fromType(Material.SPRUCE_SIGN)
                                .setDisplayName(GOLD.and(BOLD).wrap("Search by Name"))
                                .setLore(Lists.newList(
                                        GRAY.wrap("Search warps by keywords"),
                                        GRAY.wrap("in their names."),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to search"))))
                                .hideAllComponents())
                        .action(this.searchAction)
                        .build())
                .state(ItemState.builder("with_input")
                        .icon(NightItem.fromType(Material.DARK_OAK_SIGN)
                                .setDisplayName(YELLOW.and(BOLD).wrap("Search by Name"))
                                .setLore(Lists.newList(
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Current: ") + WHITE.wrap(GENERIC_INPUT)),
                                        "",
                                        GRAY.wrap("Search warps by keywords in their names."),
                                        "",
                                        YELLOW.wrap("→ " + UNDERLINED.wrap("Click to reset"))))
                                .hideAllComponents())
                        .displayModifier((context, item) -> item.replace(
                                builder -> builder.with(GENERIC_INPUT, () -> this.getObject(context).searchText())))
                        .condition(context -> this.getObject(context).searchText() != null)
                        .action(this.searchResetAction)
                        .build())
                .slots(47)
                .build());

        this.addDefaultButton("sorting", MenuItem.builder()
                .defaultState(ItemState.defaultBuilder()
                        .icon(NightItem.fromType(Material.HOPPER)
                                .setDisplayName(GOLD.and(BOLD).wrap("Sorting Mode"))
                                .setLore(Lists.newList(
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Current: ") + WHITE.wrap(GENERIC_TYPE)),
                                        "",
                                        GRAY.wrap("Toggle warps list order."),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to toggle"))))
                                .hideAllComponents())
                        .displayModifier((context, item) -> item.replace(builder -> builder
                                .with(GENERIC_TYPE, () -> this.getObject(context).sortType().localized())))
                        .action(this.sortAction)
                        .build())
                .slots(51)
                .build());
    }

    @Override
    protected void onLoad(FileConfig config) {
        int[] warpSlots = ConfigProperty.of(ConfigTypes.INT_ARRAY, "Warps.Slots", IntStream.range(9, 45).toArray())
                .resolveWithDefaults(config);

        this.warpPopulator = ItemPopulator.builder(PlayerWarp.class)
                .itemProvider((context, warp) -> {
                    Player player = context.getPlayer();

                    return warp.getIcon()
                            .hideAllComponents()
                            .localized(warp.isOwner(player) ? ICON_WARP_OWN
                                    : (warp.canEdit(player) ? ICON_WARP_MODERATE : ICON_WARP_DEFAULT))
                            .replace(builder -> builder.with(warp.placeholders()));
                })
                .actionProvider(warp -> context -> this.module.clickWarp(context, warp))
                .slots(warpSlots)
                .build();
    }

    private void backToMainMenu(ObjectActionContext<WarpsListData> context) {
        this.module.openWarpsMenu(context.getPlayer());
    }

    private void searchWarps(ObjectActionContext<WarpsListData> context) {
        this.module.openSearchDialog(context.getPlayer(), this.getObject(context));
    }

    private void resetSearch(ObjectActionContext<WarpsListData> context) {
        WarpsListData data = this.getObject(context);

        this.show(context.getPlayer(), data.category(), data.sortType(), null);
    }

    private void toggleSorting(ObjectActionContext<WarpsListData> context) {
        WarpsListData data = this.getObject(context);

        this.show(context.getPlayer(), data.category(), data.sortType().next(), data.searchText());
    }

    private boolean isGoodWarp(PlayerWarp warp, WarpsListData data) {
        if (!data.category().isWarpOfThis(warp))
            return false;

        if (data.searchText() != null) {
            String lowName = LowerCase.INTERNAL.apply(NightMessage.stripTags(warp.getName()));
            return lowName.contains(data.searchText());
        }

        return true;
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
    public void onPrepare(ViewerContext context, InventoryView view, Inventory inventory, List<MenuItem> items) {
        WarpsListData data = this.getObject(context);
        PlayerWarpSortType sortType = data.sortType();
        List<PlayerWarp> warps = this.repository.stream().filter(warp -> this.isGoodWarp(warp, data))
                .sorted(sortType.getComparator()).toList();

        this.warpPopulator.populateTo(context, warps, items);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
