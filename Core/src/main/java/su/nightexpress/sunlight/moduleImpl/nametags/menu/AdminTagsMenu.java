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
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagAccessMode;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;
import su.nightexpress.sunlight.utils.EconomyUtils;

import java.util.List;
import java.util.Locale;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Admin overview of every tag. Clicking a tag opens its editor, and the bottom row can
 * create a new tag.
 */
public class AdminTagsMenu extends AbstractMenu {

    private static final int PAGE_SIZE = 28;
    private static final int FIRST_SLOT = 0;
    private static final int SLOT_CREATE = 40;
    private static final int SLOT_CLOSE = 41;

    private final SunLightPlugin plugin;
    private final NametagsModule module;

    public AdminTagsMenu(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module) {
        super(MenuType.GENERIC_9X5, DARK_PURPLE.wrap("Nametags Admin"));
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
        Player player = context.getPlayer();
        MenuViewer viewer = context.getViewer();

        List<TagDefinition> tags = this.module.getCatalog().getTagsSorted();
        viewer.setTotalPages(Math.max(1, (tags.size() + PAGE_SIZE - 1) / PAGE_SIZE));

        int fromIndex = (viewer.getCurrentPage() - 1) * PAGE_SIZE;
        if (fromIndex >= tags.size()) return;

        int toIndex = Math.min(tags.size(), fromIndex + PAGE_SIZE);
        for (int index = fromIndex; index < toIndex; index++) {
            TagDefinition tag = tags.get(index);
            Material icon = Material.matchMaterial(tag.getIconMaterial());

            list.add(MenuItem.builder()
                .defaultState(NightItem.fromType(icon == null ? Material.NAME_TAG : icon)
                        .setDisplayName(tag.getDisplay())
                        .setLore(java.util.List.of(
                            GRAY.wrap("Id: ") + WHITE.wrap(tag.getId()),
                            GRAY.wrap("Access: ") + WHITE.wrap(pretty(tag.getAccessMode())),
                            tag.hasPrice() ? GRAY.wrap("Price: ") + WHITE.wrap(EconomyUtils.format(tag.getPrice())) : "",
                            "",
                            GRAY.wrap("Shift + right click to delete"))),
                    ctx -> this.onClickTag(ctx.getPlayer(), tag,
                        ctx.getEvent().getClick() == ClickType.SHIFT_RIGHT))
                .slots(FIRST_SLOT + index - fromIndex)
                .build());
        }

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.LIME_DYE)
                    .setDisplayName(GREEN.wrap("Create tag"))
                    .setLore(java.util.List.of(GRAY.wrap("Click to add a new tag."))),
                ctx -> this.createTag(ctx.getPlayer()))
            .slots(SLOT_CREATE)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BARRIER)
                    .setDisplayName(RED.wrap("Close"))
                    .setLore(java.util.List.of(GRAY.wrap("Click to close."))),
                ctx -> ctx.getPlayer().closeInventory())
                .slots(SLOT_CLOSE)
                .build());
    }

    private void onClickTag(@NotNull Player player, @NotNull TagDefinition tag, boolean delete) {
        if (delete) {
            this.module.getCatalog().removeTag(tag.getId());
            this.module.saveCatalog();
            this.show(this.plugin, player);
            return;
        }
        new AdminTagMenu(this.plugin, this.module, tag.getId()).open(player);
    }

    /**
     * Creates a tag with a unique id and opens it in the editor, so the admin only has to
     * fill in what they care about.
     */
    private void createTag(@NotNull Player player) {
        String id = "tag";
        int counter = 1;
        while (this.module.getCatalog().hasTag(id)) {
            id = "tag" + (++counter);
        }

        TagDefinition tag = new TagDefinition(id);
        tag.setAccessMode(TagAccessMode.FREE);
        this.module.getCatalog().putTag(tag);
        this.module.saveCatalog();

        new AdminTagMenu(this.plugin, this.module, id).open(player);
    }

    private static @NotNull String pretty(@NotNull TagAccessMode mode) {
        String name = mode.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
