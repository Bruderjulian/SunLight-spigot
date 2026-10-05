package su.nightexpress.sunlight.moduleImpl.profiles.menu;

import java.util.ArrayList;
import java.util.List;
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
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.profile.PlayerProfiles;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.profiles.PlayerProfile;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfileManager;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfilesModule;
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesLang;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.EconomyUtils;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Confirms a profile switch: shows from -&gt; to with cost, warmup and
 * cooldown summary. Confirming delegates to the normal request pipeline.
 */
public class ProfileConfirmMenu extends AbstractMenu {

    private static final int SLOT_FROM = 11;
    private static final int SLOT_TO = 15;
    private static final int SLOT_CONFIRM = 12;
    private static final int SLOT_CANCEL = 14;

    private final SunLightPlugin plugin;
    private final ProfilesModule module;
    private final String profileId;

    public ProfileConfirmMenu(SunLightPlugin plugin, ProfilesModule module, String profileId) {
        super(MenuType.GENERIC_9X3, ProfilesLang.MENU_CONFIRM_TITLE.text());
        this.plugin = plugin;
        this.module = module;
        this.profileId = profileId;
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
        this.addBackgroundItem(Material.GRAY_STAINED_GLASS_PANE, IntStream.range(0, 27).toArray());
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
        ProfileManager manager = this.module.getManager();
        SunUser user = this.module.userManager().getOrFetch(player);

        PlayerProfile from = manager.getActive(player.getUniqueId(), user);
        PlayerProfile to = manager.findByIdOrName(player.getUniqueId(), this.profileId);
        if (to == null) {
            return;
        }

        list.add(MenuItem.builder()
            .defaultState(this.head(player, from == null ? "?" : from.getName(), GRAY.wrap("Current profile")),
                ctx -> {})
            .slots(SLOT_FROM)
            .build());

        list.add(MenuItem.builder()
            .defaultState(this.head(player, to.getName(), GREEN.wrap("Switch to this profile")),
                ctx -> {})
            .slots(SLOT_TO)
            .build());

        List<String> confirmLore = new ArrayList<>();
        double cost = this.module.getSwitchCost(player);
        if (cost > 0D) {
            confirmLore.add(GRAY.wrap("Cost: " + WHITE.wrap(EconomyUtils.format(cost))));
        }
        int warmup = this.module.getSwitchWarmup(player);
        if (warmup > 0) {
            confirmLore.add(GRAY.wrap("Warmup: " + WHITE.wrap(warmup + "s (don't move)")));
        }
        long cooldownLeft = manager.cooldownLeftMs(user);
        if (cooldownLeft > 0L) {
            confirmLore.add(RED.wrap("Cooldown active: wait before switching."));
        }
        if (confirmLore.isEmpty()) {
            confirmLore.add(GRAY.wrap("No cost, no warmup."));
        }
        confirmLore.add("");
        confirmLore.add(GREEN.wrap("→ " + UNDERLINED.wrap("Click to confirm")));

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.LIME_DYE)
                    .setDisplayName(GREEN.wrap("Confirm Switch"))
                    .setLore(confirmLore),
                ctx -> {
                    ctx.getPlayer().closeInventory();
                    this.module.requestSwitch(ctx.getPlayer(), to.getId());
                })
            .slots(SLOT_CONFIRM)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.RED_DYE)
                    .setDisplayName(RED.wrap("Cancel"))
                    .setLore(List.of(GRAY.wrap("Stay on your current profile."))),
                ctx -> this.module.openProfilesMenu(ctx.getPlayer()))
            .slots(SLOT_CANCEL)
            .build());
    }

    private NightItem head(Player player, String name, String sub) {
        return NightItem.fromType(Material.PLAYER_HEAD)
            .setPlayerProfile(PlayerProfiles.createProfile(player.getUniqueId(), player.getName()))
            .setDisplayName(YELLOW.wrap(name))
            .setLore(List.of(sub));
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
