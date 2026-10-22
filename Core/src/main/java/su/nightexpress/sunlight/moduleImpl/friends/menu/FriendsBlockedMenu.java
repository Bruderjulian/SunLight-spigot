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
import su.nightexpress.nightcore.ui.inventory.item.ItemPopulator;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsLang;
import su.nightexpress.sunlight.moduleImpl.friends.model.BlockRecord;

import java.util.List;
import java.util.stream.IntStream;

/**
 * The block list. Unblocking is the only action here, so it needs no confirmation: it can only
 * widen what the player is reachable for.
 */
public class FriendsBlockedMenu extends AbstractMenu {

    private final FriendsModule module;

    private ItemPopulator<BlockRecord> populator;

    public FriendsBlockedMenu(SunLightPlugin plugin, FriendsModule module) {
        super(plugin, MenuType.GENERIC_9X5, TagWrappers.BLACK.wrap("Blocked Players"));
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
        this.addNextPageButton(40);
        this.addPreviousPageButton(38);

        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(0, 9).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(27, 36).toArray());

        this.addDefaultButton("back", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.SPECTRAL_ARROW)
                                .setDisplayName(TagWrappers.WHITE.wrap("Back to Friends")))
                        .action(context -> {
                            this.module.closeInventoryOnAction(context.getPlayer());
                            this.module.openFriends(context.getPlayer());
                        })
                        .build())
                .slots(31)
                .build());
    }

    @Override
    protected void onLoad(FileConfig config) {
        int[] slots = ConfigProperty
                .of(ConfigTypes.INT_ARRAY, "Friends.Slots-Blocked", IntStream.range(9, 27).toArray())
                .resolveWithDefaults(config);

        this.populator = ItemPopulator.builder(BlockRecord.class)
                .slots(slots)
                .itemProvider((context, block) -> NightItem.fromType(Material.PAPER)
                        .setDisplayName(TagWrappers.RED.wrap(block.getBlocked().name()))
                        .setLore(Lists.newList(
                                TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Blocked: ")
                                        + TagWrappers.WHITE.wrap(TimeFormats.formatSince(block.getCreateDate(),
                                        TimeFormatType.LITERAL)),
                                TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("Status: ")
                                        + TagWrappers.WHITE.wrap(block.isBlockedOnline() ? "Online" : "Offline"),
                                "",
                                TagWrappers.YELLOW.wrap("→ " + TagWrappers.UNDERLINED.wrap("Click to unblock")))))
                .actionProvider(block -> actionContext -> {
                    Player player = actionContext.getPlayer();
                    this.module.closeInventoryOnAction(player);
                    this.module.unblockCommand(player, block.getBlocked().name());
                    actionContext.getViewer().refresh();
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
    public void onPrepare(ViewerContext context, InventoryView view, Inventory inventory, List<MenuItem> items) {
        this.populator.populateTo(context, this.module.getRepository().getBlocks(context.getPlayer().getUniqueId()),
                items);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}