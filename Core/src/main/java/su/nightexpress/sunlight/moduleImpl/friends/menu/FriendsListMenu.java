package su.nightexpress.sunlight.moduleImpl.friends.menu;

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
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.locale.entry.IconLocale;
import su.nightexpress.nightcore.ui.inventory.item.ItemPopulator;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.profile.PlayerProfiles;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsPlaceholders;
import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsLang;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRecord;
import su.nightexpress.sunlight.moduleImpl.friends.repository.FriendsRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * The friend list.
 * <p>
 * Rows come from the repository rather than from the config, and the bound object is the list's
 * owner rather than its viewer, so staff can open somebody else's list.
 */
public class FriendsListMenu extends AbstractObjectMenu<UUID> {

    private final FriendsModule module;

    private ItemPopulator<FriendRecord> populator;

    public FriendsListMenu(SunLightPlugin plugin, FriendsModule module) {
        super(plugin, MenuType.GENERIC_9X6, FriendsLang.MENU_TITLE.text(), UUID.class);
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
        this.addNextPageButton(53);
        this.addPreviousPageButton(45);

        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(0, 9).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(45, 54).toArray());

        this.addDefaultButton("add", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.PLAYER_HEAD)
                                .setDisplayName(TagWrappers.GREEN.and(TagWrappers.BOLD).wrap("Add a Friend")))
                        .action(context -> {
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.module.openAddDialog(context.getPlayer());
                        })
                        .build())
                .slots(47)
                .build());

        this.addDefaultButton("pending", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.PAPER)
                                .setDisplayName(TagWrappers.GREEN.and(TagWrappers.BOLD).wrap("Pending Requests")))
                        .displayModifier((context, item) -> item
                                .setLore(Lists.newList(
                                        TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Incoming: ")
                                                + TagWrappers.WHITE.wrap(FriendsModule.countText(
                                                        this.module.countIncoming(context.getPlayer()))),
                                        TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Sent: ")
                                                + TagWrappers.WHITE.wrap(FriendsModule.countText(
                                                        this.module.countOutgoing(context.getPlayer()))),
                                        "",
                                        TagWrappers.GREEN.wrap("→ " + TagWrappers.UNDERLINED.wrap("Click to review")))))
                        .action(context -> {
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.module.openPending(context.getPlayer());
                        })
                        .build())
                .slots(48)
                .build());

        this.addDefaultButton("blocked", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.BARRIER)
                                .setDisplayName(TagWrappers.RED.and(TagWrappers.BOLD).wrap("Blocked Players")))
                        .displayModifier((context, item) -> item
                                .setLore(Lists.newList(
                                        TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Blocked: ")
                                                + TagWrappers.WHITE.wrap(FriendsModule.countText(
                                                        this.module.countBlocked(context.getPlayer()))),
                                        "",
                                        TagWrappers.RED.wrap("→ " + TagWrappers.UNDERLINED.wrap("Click to review")))))
                        .action(context -> {
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.module.openBlocked(context.getPlayer());
                        })
                        .build())
                .slots(49)
                .build());

        this.addDefaultButton("settings", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.COMPARATOR)
                                .setDisplayName(TagWrappers.YELLOW.and(TagWrappers.BOLD).wrap("Friends Settings")))
                        .action(context -> {
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.module.openSettings(context.getPlayer());
                        })
                        .build())
                .slots(50)
                .build());
    }

    @Override
    protected void onLoad(FileConfig config) {
        int[] friendSlots = ConfigProperty
                .of(ConfigTypes.INT_ARRAY, "Friends.Slots-Friends", IntStream.range(18, 36).toArray())
                .resolveWithDefaults(config);

        this.populator = ItemPopulator.builder(FriendRecord.class)
                .slots(friendSlots)
                .itemProvider((context, record) -> this.icon(record)
                        .hideAllComponents()
                        .setLore(Lists.newList(
                                TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Status: ")
                                        + TagWrappers.WHITE.wrap(FriendsPlaceholders.FRIEND_STATUS),
                                TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Friends since: ")
                                        + TagWrappers.WHITE.wrap(FriendsPlaceholders.FRIEND_AGE),
                                TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Last seen: ")
                                        + TagWrappers.WHITE.wrap(FriendsPlaceholders.FRIEND_LAST_SEEN),
                                "",
                                TagWrappers.YELLOW.wrap("→ " + TagWrappers.UNDERLINED.wrap("Left click to manage")),
                                TagWrappers.DARK_GRAY.wrap("→ " + TagWrappers.UNDERLINED.wrap("Right click to message")),
                                TagWrappers.DARK_GRAY.wrap("→ " + TagWrappers.UNDERLINED.wrap(
                                        "Shift + right click to teleport"))))
                .replace(builder -> builder.with(record.placeholders())
                        .with(FriendsPlaceholders.FRIEND_LAST_SEEN,
                                () -> this.module.getLastSeenText(record.getFriendId()))))
                .actionProvider(record -> actionContext -> {
                    InventoryClickEvent event = actionContext.getEvent();
                    Player player = actionContext.getPlayer();

                    if (event.getClick() == ClickType.RIGHT) {
                        this.module.closeInventoryOnAction(player);
                        this.module.promptMessage(player, record.getFriend().name());
                        return;
                    }
                    if (event.getClick() == ClickType.SHIFT_RIGHT) {
                        this.module.closeInventoryOnAction(player);
                        this.module.jumpToFriend(player, record.getFriend().name());
                        return;
                    }
                    if (event.isLeftClick()) {
                        this.module.closeInventoryOnAction(player);
                        this.module.openOptions(player, record.getFriendId());
                    }
                })
                .build();
    }

    private NightItem icon(FriendRecord record) {
        NightItem item = NightItem.fromType(Material.PLAYER_HEAD)
                .setSkullOwner(PlayerProfiles.create(record.getFriendId(), record.getFriend().name()));

        IconLocale locale;
        if (record.isFavorite()) {
            locale = FriendsLang.ICON_FRIEND_FAVORITE;
        } else if (FriendsRepository.isOnline(record.getFriendId())) {
            locale = FriendsLang.ICON_FRIEND_ONLINE;
        } else {
            locale = FriendsLang.ICON_FRIEND_OFFLINE;
        }

        return item.localized(locale);
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
        this.populator.populateTo(context, this.module.getFriendRecords(this.getObject(context)), items);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}