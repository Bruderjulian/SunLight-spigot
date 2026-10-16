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
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.profile.PlayerProfiles;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsLang;
import su.nightexpress.sunlight.moduleImpl.friends.model.FriendRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Pending requests, incoming above and outgoing below.
 * <p>
 * Incoming and outgoing are separate populators rather than one merged list, because the action a
 * click means is the whole difference between them: accept against decline.
 */
public class FriendsPendingMenu extends AbstractMenu {

    private final FriendsModule module;

    private ItemPopulator<FriendRequest> incomingPopulator;
    private ItemPopulator<FriendRequest> outgoingPopulator;

    public FriendsPendingMenu(SunLightPlugin plugin, FriendsModule module) {
        super(plugin, MenuType.GENERIC_9X6, FriendsLang.MENU_PENDING_TITLE.text());
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

        this.addBackgroundItem(Material.GRAY_STAINED_GLASS_PANE, IntStream.range(9, 18).toArray());
        this.addBackgroundItem(Material.GRAY_STAINED_GLASS_PANE, IntStream.range(36, 45).toArray());

        this.addDefaultButton("back", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.SPECTRAL_ARROW)
                                .setDisplayName(TagWrappers.WHITE.wrap("Back to Friends")))
                        .action(context -> {
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.module.openFriends(context.getPlayer());
                        })
                        .build())
                .slots(47)
                .build());
    }

    @Override
    protected void onLoad(FileConfig config) {
        int[] incomingSlots = ConfigProperty
                .of(ConfigTypes.INT_ARRAY, "Friends.Slots-Incoming", IntStream.range(18, 27).toArray())
                .resolveWithDefaults(config);
        int[] outgoingSlots = ConfigProperty
                .of(ConfigTypes.INT_ARRAY, "Friends.Slots-Outgoing", IntStream.range(27, 36).toArray())
                .resolveWithDefaults(config);

        this.incomingPopulator = ItemPopulator.builder(FriendRequest.class)
                .slots(incomingSlots)
                .itemProvider((context, request) -> this.icon(request, true)
                        .hideAllComponents()
                        .replace(builder -> builder.with(CommonPlaceholders.PLAYER_NAME, request::getSenderName)))
                .actionProvider(request -> actionContext -> {
                    InventoryClickEvent event = actionContext.getEvent();
                    Player player = actionContext.getPlayer();

                    if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                        this.module.closeInventoryOnAction(player);
                        this.module.declineIncoming(player, request.getSenderName());
                        actionContext.getViewer().refresh();
                        return;
                    }

                    if (event.isLeftClick()) {
                        this.module.closeInventoryOnAction(player);
                        this.module.acceptIncoming(player, request.getSenderName());
                    }
                })
                .build();

        this.outgoingPopulator = ItemPopulator.builder(FriendRequest.class)
                .slots(outgoingSlots)
                .itemProvider((context, request) -> this.icon(request, false)
                        .hideAllComponents()
                        .replace(builder -> builder.with(CommonPlaceholders.PLAYER_NAME, request::getTargetName)))
                .actionProvider(request -> actionContext -> {
                    this.module.closeInventoryOnAction(actionContext.getPlayer());
                    this.module.cancelRequest(actionContext.getPlayer(), request.getTargetName());
                    actionContext.getViewer().refresh();
                })
                .build();
    }

    /**
     * The head, the localized name and lore, and the one line that is per-request.
     * <p>
     * The lore is appended after {@code localized}, because that call replaces both name and lore,
     * so anything set before it would be silently dropped.
     */
    private NightItem icon(FriendRequest request, boolean incoming) {
        String name = incoming ? request.getSenderName() : request.getTargetName();
        java.util.UUID id = incoming ? request.getSenderId() : request.getTargetId();

        IconLocale locale = incoming ? FriendsLang.ICON_PENDING_INCOMING : FriendsLang.ICON_PENDING_OUTGOING;

        return NightItem.fromType(Material.PLAYER_HEAD)
                .setSkullOwner(PlayerProfiles.create(id, name))
                .localized(locale)
                .setLore(Lists.newList(
                        TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Expires in: ")
                                + TagWrappers.WHITE.wrap(TimeFormats.formatDuration(request.getExpireDate(),
                                TimeFormatType.LITERAL))));
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

        this.incomingPopulator.populateTo(context,
                new ArrayList<>(this.module.getRequestRepository().getIncoming(player.getUniqueId())), items);
        this.outgoingPopulator.populateTo(context,
                new ArrayList<>(this.module.getRequestRepository().getOutgoing(player.getUniqueId())), items);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}