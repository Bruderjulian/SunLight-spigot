package su.nightexpress.sunlight.moduleImpl.profiles.menu;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesPerms;
import su.nightexpress.sunlight.moduleImpl.profiles.dialog.ProfilesDialogKeys;
import su.nightexpress.sunlight.user.SunUser;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Per-profile options: switch, delete (two-step confirm) and back.
 * Renaming stays a chat command to keep the flow simple.
 */
public class ProfileOptionsMenu extends AbstractMenu {

    private static final long DELETE_ARM_MS = 5_000L;

    private static final int SLOT_INFO = 4;
    private static final int SLOT_SWITCH = 10;
    private static final int SLOT_RENAME = 12;
    private static final int SLOT_ICON = 13;
    private static final int SLOT_DESCRIBE = 14;
    private static final int SLOT_DELETE = 16;
    private static final int SLOT_BACK = 22;

    private final SunLightPlugin plugin;
    private final ProfilesModule module;
    private final String profileId;

    private final Map<String, Long> deleteArmed = new ConcurrentHashMap<>();

    public ProfileOptionsMenu(SunLightPlugin plugin, ProfilesModule module, String profileId) {
        super(MenuType.GENERIC_9X3, ProfilesLang.MENU_OPTIONS_TITLE.text());
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
        PlayerProfile profile = manager.findByIdOrName(player.getUniqueId(), this.profileId);
        if (profile == null) {
            return;
        }
        boolean active = manager.getActiveId(user).equals(profile.getId());

        Material infoMaterial = Material.matchMaterial(profile.getIcon());
        if (infoMaterial == null || infoMaterial.isAir()) infoMaterial = Material.PLAYER_HEAD;
        NightItem infoIcon = NightItem.fromType(infoMaterial);
        if (infoMaterial == Material.PLAYER_HEAD) {
            infoIcon.setPlayerProfile(PlayerProfiles.createProfile(player.getUniqueId(), player.getName()));
        }
        java.util.List<String> infoLore = new java.util.ArrayList<>();
        if (!profile.getDescription().isBlank()) infoLore.add(WHITE.wrap(profile.getDescription()));
        infoLore.add(GRAY.wrap("Id: " + WHITE.wrap(profile.getId())));
        infoLore.add(GRAY.wrap("Last played: " + WHITE.wrap(manager.formatLastPlayed(profile.getLastPlayed()))));
        infoLore.add(GRAY.wrap("Status: " + WHITE.wrap(active ? "active" : "inactive")));
        infoIcon.setDisplayName(YELLOW.wrap(profile.getName())).setLore(infoLore);

        list.add(MenuItem.builder()
            .defaultState(infoIcon, ctx -> {})
            .slots(SLOT_INFO)
            .build());

        if (!active) {
            list.add(MenuItem.builder()
                .defaultState(NightItem.fromType(Material.LIME_DYE)
                        .setDisplayName(GREEN.wrap("Switch"))
                        .setLore(List.of(GRAY.wrap("Play on this profile."))),
                    ctx -> {
                        if (this.module.getSettings().isSwitchConfirmGui()) {
                            new ProfileConfirmMenu(this.plugin, this.module, profile.getId()).open(ctx.getPlayer());
                            return;
                        }
                        ctx.getPlayer().closeInventory();
                        this.module.requestSwitch(ctx.getPlayer(), profile.getId());
                    })
                .slots(SLOT_SWITCH)
                .build());
        }

        if (this.module.getSettings().isRenameAllowed()
            && context.getPlayer().hasPermission(ProfilesPerms.COMMAND_RENAME)) {
            list.add(MenuItem.builder()
                .defaultState(NightItem.fromType(Material.NAME_TAG)
                        .setDisplayName(YELLOW.wrap("Rename"))
                        .setLore(List.of(GRAY.wrap("Change the profile name."))),
                    ctx -> this.plugin.showDialog(ctx.getPlayer(), ProfilesDialogKeys.PROFILE_RENAME,
                        profile, () -> this.module.openProfilesMenu(ctx.getPlayer())))
                .slots(SLOT_RENAME)
                .build());
        }

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.PAINTING)
                    .setDisplayName(YELLOW.wrap("Icon"))
                    .setLore(List.of(GRAY.wrap("Change the menu icon."))),
                ctx -> this.plugin.showDialog(ctx.getPlayer(), ProfilesDialogKeys.PROFILE_ICON,
                    profile, () -> this.module.openProfileOptions(ctx.getPlayer(), profile.getId())))
            .slots(SLOT_ICON)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.WRITABLE_BOOK)
                    .setDisplayName(YELLOW.wrap("Description"))
                    .setLore(List.of(GRAY.wrap("Change the menu description."))),
                ctx -> this.plugin.showDialog(ctx.getPlayer(), ProfilesDialogKeys.PROFILE_DESCRIBE,
                    profile, () -> this.module.openProfileOptions(ctx.getPlayer(), profile.getId())))
            .slots(SLOT_DESCRIBE)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.RED_DYE)
                    .setDisplayName(RED.wrap("Delete"))
                    .setLore(List.of(
                        GRAY.wrap("Permanently delete this profile."),
                        GRAY.wrap("Click twice to confirm."))),
                ctx -> this.onClickDelete(ctx.getPlayer(), profile))
            .slots(SLOT_DELETE)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.ARROW)
                    .setDisplayName(GRAY.wrap("Back"))
                    .setLore(List.of(GRAY.wrap("Back to your profiles."))),
                ctx -> this.module.openProfilesMenu(ctx.getPlayer()))
            .slots(SLOT_BACK)
            .build());
    }

    private void onClickDelete(Player player, PlayerProfile profile) {
        ProfileManager manager = this.module.getManager();
        SunUser user = this.module.userManager().getOrFetch(player);
        String key = player.getUniqueId() + ":" + profile.getId();

        Long armedAt = this.deleteArmed.get(key);
        if (armedAt == null || System.currentTimeMillis() - armedAt > DELETE_ARM_MS) {
            this.deleteArmed.put(key, System.currentTimeMillis());
            this.module.sendPrefixed(ProfilesLang.MENU_DELETE_ARM, player,
                builder -> builder.with(su.nightexpress.sunlight.SLPlaceholders.GENERIC_NAME, () -> profile.getName()));
            return;
        }
        this.deleteArmed.remove(key);

        switch (manager.delete(player, user, profile.getId())) {
            case OK -> {
                player.closeInventory();
                this.module.sendPrefixed(ProfilesLang.MENU_DELETED, player,
                    builder -> builder.with(su.nightexpress.sunlight.SLPlaceholders.GENERIC_NAME, () -> profile.getName()));
                this.module.openProfilesMenu(player);
            }
            case ACTIVE -> this.module.sendPrefixed(ProfilesLang.DELETE_ACTIVE, player);
            case LAST -> this.module.sendPrefixed(ProfilesLang.DELETE_LAST, player);
            case NOT_FOUND -> this.module.sendPrefixed(ProfilesLang.NOT_FOUND, player,
                builder -> builder.with(su.nightexpress.sunlight.SLPlaceholders.GENERIC_NAME, () -> profile.getName()));
        }
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
