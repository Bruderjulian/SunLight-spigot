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
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.MenuViewer;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;

import java.util.ArrayList;
import java.util.List;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Admin overview of every profile. Clicking a profile opens its editor, and the bottom row
 * can create a new one.
 */
public class AdminProfilesMenu extends AbstractMenu {

    private static final int PAGE_SIZE = 28;
    private static final int FIRST_SLOT = 0;
    private static final int SLOT_CREATE = 40;
    private static final int SLOT_CLOSE = 41;

    private final SunLightPlugin plugin;
    private final NametagsModule module;

    public AdminProfilesMenu(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module) {
        super(MenuType.GENERIC_9X5, NametagsLang.MENU_ADMIN_PROFILES_TITLE.text());
        this.plugin = plugin;
        this.module = module;
    }

    public boolean open(@NotNull Player player) {
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
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, 36, 37, 38, 39, 42, 43, 44);
        this.addNextPageItem(Material.ARROW, 39);
        this.addPreviousPageItem(Material.ARROW, 36);
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
        MenuViewer viewer = context.getViewer();

        List<Profile> profiles = this.module.getCatalog().getProfilesSorted();
        viewer.setTotalPages(Math.max(1, (profiles.size() + PAGE_SIZE - 1) / PAGE_SIZE));

        int fromIndex = (viewer.getCurrentPage() - 1) * PAGE_SIZE;
        if (fromIndex >= profiles.size()) return;

        int toIndex = Math.min(profiles.size(), fromIndex + PAGE_SIZE);
        for (int index = fromIndex; index < toIndex; index++) {
            Profile profile = profiles.get(index);
            Material icon = Material.matchMaterial(profile.getIconMaterial());

            List<String> lore = new ArrayList<>(profile.getDescription());
            lore.add("");
            lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_ID.text() + " ") + WHITE.wrap(profile.getId()));
            lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_WORLDS.text() + " ") + WHITE.wrap(
                profile.getWorlds().isEmpty() ? NametagsLang.MENU_VALUE_NONE.text()
                    : String.join(", ", profile.getWorlds())));
            lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_RANK.text() + " ") + WHITE.wrap(
                profile.hasRank() ? profile.getRankId() : NametagsLang.MENU_FIELD_INHERIT.text()));
            lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_TAG.text() + " ") + WHITE.wrap(
                profile.hasTag() ? profile.getTagId() : NametagsLang.MENU_FIELD_INHERIT.text()));
            lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_PRIORITY.text() + " ") + WHITE.wrap(String.valueOf(profile.getPriority())));
            lore.add("");
            lore.add(GRAY.wrap(NametagsLang.MENU_RIGHT_CLICK_DELETE.text()));

            list.add(MenuItem.builder()
                .defaultState(NightItem.fromType(icon == null ? Material.NAME_TAG : icon)
                        .setDisplayName(profile.getDisplay())
                        .setLore(lore),
                    ctx -> this.onClickProfile(ctx.getPlayer(), profile,
                        ctx.getEvent().getClick() == ClickType.RIGHT))
                .slots(FIRST_SLOT + index - fromIndex)
                .build());
        }

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.LIME_DYE)
                    .setDisplayName(GREEN.wrap(NametagsLang.MENU_CREATE_PROFILE.text()))
                    .setLore(List.of(GRAY.wrap(NametagsLang.MENU_CREATE_PROFILE_HINT.text()))),
                ctx -> this.createProfile(ctx.getPlayer()))
            .slots(SLOT_CREATE)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BARRIER)
                    .setDisplayName(RED.wrap(NametagsLang.MENU_CLOSE.text()))
                    .setLore(List.of(GRAY.wrap(NametagsLang.MENU_CLICK_CLOSE.text()))),
                ctx -> ctx.getPlayer().closeInventory())
            .slots(SLOT_CLOSE)
            .build());
    }

    private void onClickProfile(@NotNull Player player, @NotNull Profile profile, boolean delete) {
        if (delete) {
            this.module.getCatalog().removeProfile(profile.getId());
            this.module.saveCatalog();
            this.show(this.plugin, player);
            return;
        }
        new AdminProfileMenu(this.plugin, this.module, profile.getId()).open(player);
    }

    private void createProfile(@NotNull Player player) {
        String id = AdminTagsMenu.uniqueId("profile", this.module.getCatalog()::hasProfile);

        this.module.getCatalog().putProfile(new Profile(id));
        this.module.saveCatalog();

        new AdminProfileMenu(this.plugin, this.module, id).open(player);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
