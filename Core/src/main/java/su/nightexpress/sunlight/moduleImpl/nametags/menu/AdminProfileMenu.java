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
import su.nightexpress.nightcore.ui.inventory.action.ActionContext;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.dialog.DefinitionFieldDialog;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;
import su.nightexpress.sunlight.moduleImpl.nametags.model.RankDefinition;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Admin editor for a single profile. The forced rank and tag are cycled rather than typed,
 * because they must reference an existing id and a stale reference would silently degrade.
 */
public class AdminProfileMenu extends AbstractMenu {

    private static final int SLOT_DISPLAY = 10;
    private static final int SLOT_DESCRIPTION = 11;
    private static final int SLOT_ICON = 14;
    private static final int SLOT_WORLDS = 21;
    private static final int SLOT_PRIORITY = 22;
    private static final int SLOT_RANK = 23;
    private static final int SLOT_TAG = 25;

    private final SunLightPlugin plugin;
    private final NametagsModule module;
    private final String profileId;

    public AdminProfileMenu(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module,
            @NotNull String profileId
    ) {
        super(MenuType.GENERIC_9X3, NametagsLang.MENU_ADMIN_PROFILE_TITLE.text());
        this.plugin = plugin;
        this.module = module;
        this.profileId = profileId;
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
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                17, 18, 19, 20, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35);
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
        Profile profile = this.module.getCatalog().getProfile(this.profileId);
        if (profile == null) {
            context.getPlayer().closeInventory();
            return;
        }

        list.add(this.textItem(SLOT_DISPLAY, Material.NAME_TAG, profile.getDisplay(),
            NametagsLang.MENU_FIELD_DISPLAY.text(),
            player -> this.openField(player, profile, "profile_display", NametagsLang.MENU_FIELD_DISPLAY.text(),
                profile.getDisplay(), Profile::setDisplay)));
        list.add(this.textItem(SLOT_DESCRIPTION, Material.PAPER, NametagsLang.MENU_FIELD_DESCRIPTION.text(),
            NametagsLang.MENU_FIELD_DESCRIPTION.text(),
            player -> this.openListField(player, profile, "profile_description", NametagsLang.MENU_FIELD_DESCRIPTION.text(),
                profile.getDescription(), profile::setDescription)));
        list.add(this.textItem(SLOT_ICON, Material.ITEM_FRAME, profile.getIconMaterial(), NametagsLang.MENU_FIELD_ICON.text(),
            player -> this.openField(player, profile, "profile_icon", NametagsLang.MENU_FIELD_ICON.text(),
                profile.getIconMaterial(), Profile::setIconMaterial)));
        list.add(this.textItem(SLOT_WORLDS, Material.GRASS_BLOCK, this.worldsOf(profile),
            NametagsLang.MENU_WORLDS_HINT.text(),
            player -> this.openListField(player, profile, "profile_worlds", NametagsLang.MENU_FIELD_WORLDS.text(),
                new ArrayList<>(profile.getWorlds()), values -> profile.setWorlds(new ArrayList<>(values)))));

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.COMPARATOR)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_FIELD_PRIORITY.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(String.valueOf(profile.getPriority())),
                        "",
                        GRAY.wrap(NametagsLang.MENU_PRIORITY_HINT.text()),
                        GRAY.wrap(NametagsLang.MENU_CLICK_LEFT.text() + " ") + WHITE.wrap("-1"),
                        GRAY.wrap(NametagsLang.MENU_CLICK_RIGHT.text() + " ") + WHITE.wrap("+1"))),
                ctx -> this.adjustPriority(ctx, profile))
            .slots(SLOT_PRIORITY)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.PLAYER_HEAD)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_FIELD_RANK.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(this.rankLabel(profile)),
                        "",
                        GRAY.wrap(NametagsLang.MENU_CLICK_CYCLE.text()))),
                ctx -> this.cycleRank(ctx.getPlayer(), profile))
            .slots(SLOT_RANK)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.PAPER)
                    .setDisplayName(YELLOW.wrap(NametagsLang.MENU_FIELD_TAG.text()))
                    .setLore(List.of(
                        GRAY.wrap(NametagsLang.MENU_CURRENT.text() + " ") + WHITE.wrap(this.tagLabel(profile)),
                        "",
                        GRAY.wrap(NametagsLang.MENU_CLICK_CYCLE.text()))),
                ctx -> this.cycleTag(ctx.getPlayer(), profile))
            .slots(SLOT_TAG)
            .build());
    }

    private @NotNull String worldsOf(@NotNull Profile profile) {
        if (profile.getWorlds().isEmpty()) return NametagsLang.MENU_VALUE_NONE.text();
        return String.join(", ", profile.getWorlds());
    }

    private @NotNull String rankLabel(@NotNull Profile profile) {
        if (!profile.hasRank()) return NametagsLang.MENU_FIELD_INHERIT.text();
        RankDefinition rank = this.module.getCatalog().getRank(profile.getRankId());
        return rank == null ? profile.getRankId() : rank.getDisplay();
    }

    private @NotNull String tagLabel(@NotNull Profile profile) {
        if (!profile.hasTag()) return NametagsLang.MENU_FIELD_INHERIT.text();
        TagDefinition tag = this.module.getCatalog().getTag(profile.getTagId());
        return tag == null ? profile.getTagId() : tag.getDisplay();
    }

    private @NotNull MenuItem textItem(int slot, @NotNull Material icon, @NotNull String value,
            @NotNull String description, @NotNull Consumer<Player> action) {
        return MenuItem.builder()
            .defaultState(NightItem.fromType(icon)
                    .setDisplayName(WHITE.wrap(value.isBlank() ? NametagsLang.MENU_VALUE_NONE.text() : value))
                    .setLore(List.of(description, "", GRAY.wrap(NametagsLang.MENU_CLICK_EDIT.text()))),
                ctx -> action.accept(ctx.getPlayer()))
            .slots(slot)
            .build();
    }

    private void openField(@NotNull Player player, @NotNull Profile profile,
            @NotNull String field, @NotNull String title, @NotNull String value,
            @NotNull java.util.function.BiConsumer<Profile, String> applier
    ) {
        new DefinitionFieldDialog<>(this.module, field, title, value, null, applier, this::commitProfile)
                .show(player, profile, null);
    }

    private void openListField(@NotNull Player player, @NotNull Profile profile,
            @NotNull String field, @NotNull String title, @NotNull List<String> values,
            @NotNull Consumer<List<String>> applier
    ) {
        new DefinitionFieldDialog<Profile>(this.module, field, title,
                DefinitionFieldDialog.joinList(values), NametagsLang.MENU_HINT_COMMA_SEPARATED.text(),
                (definition, raw) -> applier.accept(DefinitionFieldDialog.splitList(raw)),
                this::commitProfile)
                        .show(player, profile, null);
    }

    private void adjustPriority(@NotNull ActionContext context, @NotNull Profile profile) {
        ClickType click = context.getEvent().getClick();
        if (click == ClickType.RIGHT) {
            profile.setPriority(profile.getPriority() + 1);
        } else if (click == ClickType.LEFT) {
            profile.setPriority(profile.getPriority() - 1);
        }
        this.commit(context.getPlayer(), profile);
    }

    /**
     * Cycles through every rank plus an "inherit" entry. {@code null} in the list is the
     * unset sentinel, which is what the profile stores to defer to normal resolution.
     */
    private void cycleRank(@NotNull Player player, @NotNull Profile profile) {
        List<String> ids = new ArrayList<>();
        ids.add(null);
        this.module.getCatalog().getRanksSorted().forEach(rank -> ids.add(rank.getId()));

        int current = ids.indexOf(profile.hasRank() ? profile.getRankId() : null);
        int next = (current + 1) % ids.size();
        profile.setRankId(ids.get(next));
        this.commit(player, profile);
    }

    private void cycleTag(@NotNull Player player, @NotNull Profile profile) {
        List<String> ids = new ArrayList<>();
        ids.add(null);
        this.module.getCatalog().getTagsSorted().forEach(tag -> ids.add(tag.getId()));

        int current = ids.indexOf(profile.hasTag() ? profile.getTagId() : null);
        int next = (current + 1) % ids.size();
        profile.setTagId(ids.get(next));
        this.commit(player, profile);
    }

    private void commit(@NotNull Player player, @NotNull Profile profile) {
        this.commitProfile(profile);
        this.show(this.plugin, player);
    }

    private void commitProfile(@NotNull Profile profile) {
        this.module.getCatalog().putProfile(profile);
        this.module.saveCatalog();
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
