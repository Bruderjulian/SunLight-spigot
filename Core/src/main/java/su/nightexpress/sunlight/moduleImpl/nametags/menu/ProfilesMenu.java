package su.nightexpress.sunlight.moduleImpl.nametags.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
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
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Player-facing profile browser. Locked profiles stay visible but greyed out, so a player
 * can tell "this exists but is not for me" apart from "this is misconfigured", which is the
 * same reasoning the tag browser uses.
 */
public class ProfilesMenu extends AbstractMenu {

    private static final int PAGE_SIZE = 27;
    private static final int FIRST_SLOT = 9;
    private static final int SLOT_NONE = 4;

    private final SunLightPlugin plugin;
    private final NametagsModule module;

    public ProfilesMenu(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module) {
        super(MenuType.GENERIC_9X5, NametagsLang.MENU_PROFILES_TITLE.text());
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
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(0, 9).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(36, 45).toArray());
        this.addNextPageItem(Material.ARROW, 41);
        this.addPreviousPageItem(Material.ARROW, 39);
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

        Profile selected = this.module.getSelectedProfile(player);
        List<Profile> profiles = this.module.getCatalog().getProfilesSorted();

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BARRIER)
                    .setDisplayName(RED.wrap(NametagsLang.MENU_VALUE_NONE.text()))
                    .setLore(List.of(
                        "",
                        selected == null ? GRAY.wrap(NametagsLang.MENU_SELECTED.text())
                            : GRAY.wrap(NametagsLang.MENU_CLICK_SELECT.text()))),
                ctx -> this.onSelect(ctx.getPlayer(), null))
            .slots(SLOT_NONE)
            .build());

        viewer.setTotalPages(Math.max(1, (profiles.size() + PAGE_SIZE - 1) / PAGE_SIZE));

        int fromIndex = (viewer.getCurrentPage() - 1) * PAGE_SIZE;
        if (fromIndex >= profiles.size()) return;

        int toIndex = Math.min(profiles.size(), fromIndex + PAGE_SIZE);
        for (int index = fromIndex; index < toIndex; index++) {
            Profile profile = profiles.get(index);
            boolean available = this.module.getCatalog().isProfileAvailable(player, profile);
            boolean isSelected = selected != null && selected.getId().equals(profile.getId());
            Material icon = Material.matchMaterial(profile.getIconMaterial());

            list.add(MenuItem.builder()
                .defaultState(NightItem.fromType(icon == null ? Material.NAME_TAG : icon)
                        .setDisplayName(available ? profile.getDisplay() : GRAY.wrap(profile.getDisplay()))
                        .setLore(this.buildLore(profile, available, isSelected)),
                    ctx -> this.onClickProfile(ctx.getPlayer(), profile, available))
                .slots(FIRST_SLOT + index - fromIndex)
                .build());
        }
    }

    private @NotNull List<String> buildLore(@NotNull Profile profile, boolean available, boolean isSelected) {
        List<String> lore = new ArrayList<>(profile.getDescription());

        lore.add("");
        lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_WORLD_LABEL.text() + " ") + WHITE.wrap(
            profile.getWorlds().isEmpty() ? NametagsLang.MENU_WORLDS_HINT.text()
                : String.join(", ", profile.getWorlds())));
        lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_RANK.text() + " ") + WHITE.wrap(
            profile.hasRank() ? profile.getRankId() : NametagsLang.MENU_FIELD_INHERIT.text()));
        lore.add(GRAY.wrap(NametagsLang.MENU_FIELD_TAG.text() + " ") + WHITE.wrap(
            profile.hasTag() ? profile.getTagId() : NametagsLang.MENU_FIELD_INHERIT.text()));
        lore.add("");

        if (!available) {
            lore.add(SOFT_RED.wrap(NametagsLang.MENU_LOCKED.text()));
        } else if (isSelected) {
            lore.add(GREEN.wrap(NametagsLang.MENU_SELECTED.text()));
        } else {
            lore.add(GOLD.wrap("→ " + UNDERLINED.wrap(NametagsLang.MENU_CLICK_SELECT.text())));
        }
        return lore;
    }

    private void onClickProfile(@NotNull Player player, @NotNull Profile profile, boolean available) {
        if (!available) {
            this.module.sendPrefixed(NametagsLang.COMMAND_PROFILE_ERROR_LOCKED, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, profile::getDisplay));
            return;
        }
        this.onSelect(player, profile.getId());
    }

    private void onSelect(@NotNull Player player, String profileId) {
        this.module.selectProfile(player, profileId);

        if (profileId == null) {
            this.module.sendPrefixed(NametagsLang.COMMAND_PROFILE_CLEARED, player);
        } else {
            Profile profile = this.module.getCatalog().getProfile(profileId);
            this.module.sendPrefixed(NametagsLang.COMMAND_PROFILE_SELECTED, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, () -> profile == null ? profileId : profile.getDisplay()));
        }
        this.show(this.plugin, player);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
