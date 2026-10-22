package su.nightexpress.sunlight.moduleImpl.profiles.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.MenuViewer;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.profile.PlayerProfiles;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.profiles.PlayerProfile;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfileManager;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfilesModule;
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesLang;
import su.nightexpress.sunlight.user.SunUser;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Profile selector. Each owned profile is the viewer's own head: the active
 * one glows, the rest show their name, id and last-played time.
 * Left-click switches, right-click opens the options menu.
 */
public class ProfileListMenu extends AbstractMenu {

    private static final int PAGE_SIZE = 28;
    private static final int SLOT_PREV = 36;
    private static final int SLOT_CREATE = 40;
    private static final int SLOT_CLOSE = 41;
    private static final int SLOT_NEXT = 44;

    private final SunLightPlugin plugin;
    private final ProfilesModule module;

    public ProfileListMenu(SunLightPlugin plugin, ProfilesModule module) {
        super(MenuType.GENERIC_9X5, ProfilesLang.MENU_TITLE.text());
        this.plugin = plugin;
        this.module = module;
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
        this.addBackgroundItem(Material.GRAY_STAINED_GLASS_PANE, IntStream.range(0, 36).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, 37, 38, 39, 42, 43);
        this.addNextPageButton(SLOT_NEXT);
        this.addPreviousPageButton(SLOT_PREV);
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
        Player player = context.getPlayer();
        MenuViewer viewer = context.getViewer();
        ProfileManager manager = this.module.getManager();
        SunUser user = this.module.userManager().getOrFetch(player);
        manager.ensureLoaded(player, user);

        List<PlayerProfile> profiles = manager.getProfileList(player.getUniqueId());
        viewer.setTotalPages(Math.max(1, (profiles.size() + PAGE_SIZE - 1) / PAGE_SIZE));

        int fromIndex = (viewer.getCurrentPage() - 1) * PAGE_SIZE;
        if (fromIndex < profiles.size()) {
            String activeId = manager.getActiveId(user);
            int toIndex = Math.min(profiles.size(), fromIndex + PAGE_SIZE);
            for (int index = fromIndex; index < toIndex; index++) {
                PlayerProfile profile = profiles.get(index);
                boolean active = profile.getId().equals(activeId);
                list.add(MenuItem.button()
                    .defaultState(this.profileIcon(player, profile, manager, active),
                        ctx -> this.onClickProfile(ctx.getPlayer(), profile, ctx.getEvent().getClick() == ClickType.RIGHT))
                    .slots(index - fromIndex)
                    .build());
            }
        }

        list.add(MenuItem.button()
            .defaultState(NightItem.fromType(Material.EMERALD)
                    .setDisplayName(GREEN.wrap("New Profile"))
                    .setLore(List.of(
                        GRAY.wrap("Slots used: " + WHITE.wrap(profiles.size() + "/" + manager.maxSlots(player))),
                        "",
                        GRAY.wrap("Create one with "),
                        GREEN.wrap("→ " + UNDERLINED.wrap("/profile create <name>")))),
                ctx -> {
                    ctx.getPlayer().closeInventory();
                    this.module.sendPrefixed(ProfilesLang.MENU_CREATE_HINT, ctx.getPlayer());
                })
            .slots(SLOT_CREATE)
            .build());

        list.add(MenuItem.button()
            .defaultState(NightItem.fromType(Material.BARRIER)
                    .setDisplayName(RED.wrap("Close"))
                    .setLore(List.of(GRAY.wrap("Close this menu."))),
                ctx -> ctx.getPlayer().closeInventory())
            .slots(SLOT_CLOSE)
            .build());
    }

    private NightItem profileIcon(Player player, PlayerProfile profile, ProfileManager manager, boolean active) {
        List<String> lore = new ArrayList<>();
        if (!profile.getDescription().isBlank()) {
            lore.add(WHITE.wrap(profile.getDescription()));
            lore.add("");
        }
        lore.add(GRAY.wrap("Id: " + WHITE.wrap(profile.getId())));
        lore.add(GRAY.wrap("Last played: " + WHITE.wrap(manager.formatLastPlayed(profile.getLastPlayed()))));
        lore.add("");
        if (active) {
            lore.add(GREEN.wrap("◆ Currently active"));
            lore.add(GRAY.wrap("Right-click for options."));
        } else {
            lore.add(GREEN.wrap("→ " + UNDERLINED.wrap("Click to switch")));
            lore.add(GRAY.wrap("Right-click for options."));
            if (manager.getCombat().isInCombat(player)) {
                lore.add(RED.wrap("✘ Switching blocked while in combat."));
            }
        }

        Material material = Material.matchMaterial(profile.getIcon());
        if (material == null || material.isAir()) material = Material.PLAYER_HEAD;

        NightItem icon = NightItem.fromType(material);
        if (material == Material.PLAYER_HEAD) {
            icon.setPlayerProfile(PlayerProfiles.createProfile(player.getUniqueId(), player.getName()));
        }
        icon.setDisplayName((active ? GREEN : YELLOW).wrap(profile.getName())).setLore(lore);
        if (active) {
            icon.setEnchantGlint(true);
        }
        return icon;
    }

    private void onClickProfile(Player player, PlayerProfile profile, boolean options) {
        if (options) {
            this.module.openProfileOptions(player, profile.getId());
            return;
        }
        if (this.module.getSettings().isSwitchConfirmGui()) {
            new ProfileConfirmMenu(this.plugin, this.module, profile.getId()).open(player);
            return;
        }
        player.closeInventory();
        this.module.requestSwitch(player, profile.getId());
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
