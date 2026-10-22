package su.nightexpress.sunlight.moduleImpl.friends.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.profile.PlayerProfiles;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsPlaceholders;
import su.nightexpress.sunlight.moduleImpl.friends.dialog.FriendsDialogKeys;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRecord;

import java.util.List;
import java.util.UUID;

/**
 * Everything a player can do to one friend.
 * <p>
 * The bound object is the friend's id, so the buttons keep working when the friend's name changes
 * between opening the menu and clicking: every action resolves the id again.
 */
public class FriendOptionsMenu extends AbstractObjectMenu<UUID> {

    private final FriendsModule module;

    public FriendOptionsMenu(SunLightPlugin plugin, FriendsModule module) {
        super(plugin, MenuType.GENERIC_9X3, TagWrappers.BLACK.wrap("Friend Options"), UUID.class);
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
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, java.util.stream.IntStream.range(0, 9).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, java.util.stream.IntStream.range(18, 27).toArray());

        this.addDefaultButton("back", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.SPECTRAL_ARROW)
                                .setDisplayName(TagWrappers.WHITE.wrap("Back to Friends")))
                        .action(context -> {
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.module.openFriends(context.getPlayer());
                        })
                        .build())
                .slots(18)
                .build());

        this.addDefaultButton("head", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.PLAYER_HEAD)
                                .setDisplayName(TagWrappers.YELLOW.wrap(FriendsPlaceholders.FRIEND_NAME)))
                        .displayModifier((context, item) -> {
                            FriendRecord record = this.module.getFriendRecord(context.getPlayer().getUniqueId(),
                                    this.getObject(context));
                            if (record == null) {
                                return;
                            }
                            item.setSkullOwner(PlayerProfiles.create(record.getFriendId(), record.getFriend().name()))
                                    .setLore(Lists.newList(
                                            TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Status: ")
                                                    + TagWrappers.WHITE.wrap(FriendsPlaceholders.FRIEND_STATUS),
                                            TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Friends since: ")
                                                    + TagWrappers.WHITE.wrap(TimeFormats.formatSince(record.getCreateDate(),
                                                    TimeFormatType.LITERAL))))
                                    .replace(builder -> builder.with(record.placeholders()));
                        })
                        .build())
                .slots(13)
                .build());

        this.addDefaultButton("favorite", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.NETHER_STAR)
                                .setDisplayName(TagWrappers.SOFT_YELLOW.and(TagWrappers.BOLD).wrap("Favourite")))
                        .displayModifier((context, item) -> item
                                .setLore(Lists.newList(
                                        TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("State: ")
                                                + TagWrappers.WHITE.wrap(this.favoriteText(context)),
                                        "",
                                        TagWrappers.GREEN.wrap("→ " + TagWrappers.UNDERLINED.wrap("Click to toggle")))))
                        .action(context -> {
                            this.module.toggleFavorite(context.getPlayer(), this.getObject(context));
                            context.getViewer().refresh();
                        })
                        .build())
                .slots(11)
                .build());

        this.addDefaultButton("message", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.PAPER)
                                .setDisplayName(TagWrappers.WHITE.wrap("Send a Message")))
                        .action(context -> {
                            FriendRecord record = this.module.getFriendRecord(context.getPlayer().getUniqueId(),
                                    this.getObject(context));
                            if (record == null) {
                                return;
                            }
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.module.promptMessage(context.getPlayer(), record.getFriend().name());
                        })
                        .build())
                .slots(15)
                .build());

        this.addDefaultButton("jump", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.ENDER_PEARL)
                                .setDisplayName(TagWrappers.SOFT_AQUA.wrap("Teleport to Friend")))
                        .action(context -> {
                            FriendRecord record = this.module.getFriendRecord(context.getPlayer().getUniqueId(),
                                    this.getObject(context));
                            if (record == null) {
                                return;
                            }
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.module.jumpToFriend(context.getPlayer(), record.getFriend().name());
                        })
                        .build())
                .slots(22)
                .build());

        this.addDefaultButton("remove", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.RED_DYE)
                                .setDisplayName(TagWrappers.RED.and(TagWrappers.BOLD).wrap("Remove Friend"))
                                .setLore(Lists.newList(TagWrappers.DARK_GRAY.wrap("» ")
                                        + TagWrappers.GRAY.wrap("Removes the friendship from both sides."),
                                        "",
                                        TagWrappers.RED.wrap("→ " + TagWrappers.UNDERLINED.wrap("Click to remove")))))
                        .action(context -> {
                            FriendRecord record = this.module.getFriendRecord(context.getPlayer().getUniqueId(),
                                    this.getObject(context));
                            if (record == null) {
                                return;
                            }
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.plugin.showDialog(context.getPlayer(), FriendsDialogKeys.FRIEND_REMOVE,
                                    record.getFriend().name(), () -> this.module.openFriends(context.getPlayer()));
                        })
                        .build())
                .slots(24)
                .build());

        this.addDefaultButton("block", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.BARRIER)
                                .setDisplayName(TagWrappers.DARK_RED.and(TagWrappers.BOLD).wrap("Block Player"))
                                .setLore(Lists.newList(TagWrappers.DARK_GRAY.wrap("» ")
                                        + TagWrappers.GRAY.wrap("Also removes the friendship."),
                                        "",
                                        TagWrappers.DARK_RED.wrap("→ " + TagWrappers.UNDERLINED.wrap("Click to block")))))
                        .action(context -> {
                            FriendRecord record = this.module.getFriendRecord(context.getPlayer().getUniqueId(),
                                    this.getObject(context));
                            if (record == null) {
                                return;
                            }
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.plugin.showDialog(context.getPlayer(), FriendsDialogKeys.FRIEND_BLOCK,
                                    record.getFriend().name(), () -> this.module.openFriends(context.getPlayer()));
                        })
                        .build())
                .slots(16)
                .build());
    }

    private String favoriteText(ViewerContext context) {
        FriendRecord record = this.module.getFriendRecord(context.getPlayer().getUniqueId(), this.getObject(context));
        return record != null && record.isFavorite() ? "Yes" : "No";
    }

    @Override
    protected void onLoad(FileConfig config) {
        ConfigProperty.of(ConfigTypes.INT_ARRAY, "Friends.Slots", new int[] { 11, 13, 15, 16, 22, 24 })
                .resolveWithDefaults(config);
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
        Player player = context.getPlayer();
        if (this.module.getFriendRecord(player.getUniqueId(), this.getObject(context)) == null) {
            // The friendship was removed while the menu was open; an empty menu is better than a
            // set of buttons that all do nothing.
            context.getViewer().closeMenu();
        }
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}