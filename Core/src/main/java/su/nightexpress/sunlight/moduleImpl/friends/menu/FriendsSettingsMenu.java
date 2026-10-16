package su.nightexpress.sunlight.moduleImpl.friends.menu;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.MenuType;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.friends.FriendToggle;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsLang;
import su.nightexpress.sunlight.user.SunUser;

/**
 * The three privacy toggles.
 * <p>
 * Each toggle is its own config-backed button rather than a hardcoded row, so a server can move
 * them without touching code, and each one reads the user's stored value on every render rather
 * than caching it, so a change made from the command shows up here too.
 */
public class FriendsSettingsMenu extends AbstractMenu {

    private final FriendsModule module;

    private final Map<FriendToggle, ItemState> states;

    public FriendsSettingsMenu(SunLightPlugin plugin, FriendsModule module) {
        super(plugin, MenuType.GENERIC_9X3, FriendsLang.MENU_SETTINGS_TITLE.text());
        this.module = module;
        this.states = new EnumMap<>(FriendToggle.class);
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

        this.states.put(FriendToggle.REQUESTS, this.buildToggle(FriendToggle.REQUESTS,
                Material.PAPER,
                "Other players can send you a request.",
                11));
        this.states.put(FriendToggle.LOGIN_ALERTS, this.buildToggle(FriendToggle.LOGIN_ALERTS,
                Material.CLOCK,
                "You are told when a friend joins or leaves.",
                13));
        this.states.put(FriendToggle.TELEPORT, this.buildToggle(FriendToggle.TELEPORT,
                Material.ENDER_PEARL,
                "Friends can teleport to you.",
                15));

        this.states.forEach((toggle, state) -> this.addDefaultButton(toggle.getId(),
                MenuItem.button().defaultState(state).slots(this.slotOf(toggle)).build()));
    }

    private int slotOf(FriendToggle toggle) {
        return switch (toggle) {
            case REQUESTS -> 11;
            case LOGIN_ALERTS -> 13;
            case TELEPORT -> 15;
        };
    }

    private ItemState buildToggle(FriendToggle toggle, Material material, String description, int slot) {
        return ItemState.builder()
                .icon(NightItem.fromType(material)
                        .setDisplayName(TagWrappers.YELLOW.and(TagWrappers.BOLD).wrap(toggle.getDisplay())))
                .displayModifier((context, item) -> item.setLore(Lists.newList(
                        TagWrappers.DARK_GRAY.wrap("» ") + TagWrappers.GRAY.wrap("State: ")
                                + TagWrappers.WHITE.wrap(this.stateText(context.getPlayer(), toggle)),
                        "",
                        TagWrappers.GRAY.wrap(description),
                        "",
                        TagWrappers.GREEN.wrap("→ " + TagWrappers.UNDERLINED.wrap("Click to toggle")))))
                .action(context -> this.toggle(context.getPlayer(), toggle))
                .build();
    }

    private void toggle(Player player, FriendToggle toggle) {
        SunUser user = this.module.userManager().getOrFetch(player);
        boolean state = !toggle.get(user);

        this.module.applyToggle(user, toggle, state);
        this.module.closeInventoryOnAction(player);
        this.module.sendToggle(player, toggle, state, null);
    }

    private String stateText(Player player, FriendToggle toggle) {
        return toggle.get(this.module.userManager().getOrFetch(player))
                ? TagWrappers.GREEN.wrap("Enabled")
                : TagWrappers.RED.wrap("Disabled");
    }

    @Override
    protected void onLoad(FileConfig config) {
        // Nothing to read: the three buttons are declared as defaults, so a server can still move
        // or restyle them through the generated menu file.
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
        // The three toggles are declared as default buttons and re-read the user's stored value
        // through their display modifier on every render, so nothing dynamic belongs here.
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}