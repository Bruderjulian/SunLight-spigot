package su.nightexpress.sunlight.moduleImpl.links.menu;

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
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.links.Link;
import su.nightexpress.sunlight.moduleImpl.links.LinksFiles;
import su.nightexpress.sunlight.moduleImpl.links.LinksModule;
import su.nightexpress.sunlight.moduleImpl.links.config.LinksLang;
import su.nightexpress.sunlight.moduleImpl.links.dialog.LinksDialogKeys;

import java.util.List;

public class LinkSettingsMenu extends AbstractObjectMenu<Link> {

    // No background row: the controls own the middle rows and the navigation buttons the bottom one, so
    // nothing is layered on the same slot.
    private static final int SLOT_ENABLED  = 10;
    private static final int SLOT_NAME     = 11;
    private static final int SLOT_ICON     = 12;
    private static final int SLOT_URL      = 13;
    private static final int SLOT_COMMAND  = 14;
    private static final int SLOT_EXECUTOR = 19;
    private static final int SLOT_PERM     = 20;
    private static final int SLOT_PRIORITY = 21;
    private static final int SLOT_CLICKS   = 22;
    private static final int SLOT_RETURN   = 40;
    private static final int SLOT_DELETE   = 44;

    private final LinksModule module;

    public LinkSettingsMenu(SunLightPlugin plugin, LinksModule module) {
        super(plugin, MenuType.GENERIC_9X5, LinksLang.MENU_TITLE_SETTINGS.text(), Link.class);
        this.module = module;

        this.load(plugin, FileConfig.load(module.getLocalUIPath(), LinksFiles.FILE_MENU_SETTINGS));
    }

    @Override
    public void defineDefaultLayout() {
        this.addDefaultButton("enabled", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.LIME_DYE)
                                .localized(LinksLang.ICON_SETTINGS_ENABLED)
                                .hideAllComponents())
                        .displayModifier((context, item) -> item
                                .replace(builder -> builder.with(this.getObject(context).placeholders())))
                        .action(context -> {
                            Link link = this.getObject(context);
                            link.setEnabled(!link.isEnabled());
                            this.module.saveLink(link);
                            context.getViewer().refresh();
                        })
                        .build())
                .slots(SLOT_ENABLED)
                .build());

        this.addDefaultButton("name", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.NAME_TAG)
                                .localized(LinksLang.ICON_SETTINGS_NAME)
                                .hideAllComponents())
                        .displayModifier((context, item) -> item
                                .replace(builder -> builder.with(this.getObject(context).placeholders())))
                        .action(context -> this.plugin.showDialog(context.getPlayer(),
                                LinksDialogKeys.LINK_DISPLAY_NAME, this.getObject(context),
                                () -> context.getViewer().refresh()))
                        .build())
                .slots(SLOT_NAME)
                .build());

        this.addDefaultButton("icon", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.ITEM_FRAME)
                                .localized(LinksLang.ICON_SETTINGS_ICON)
                                .hideAllComponents())
                        .action(context -> this.module.openIconSelection(context.getPlayer(),
                                this.getObject(context)))
                        .build())
                .slots(SLOT_ICON)
                .build());

        this.addDefaultButton("url", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.PAPER)
                                .localized(LinksLang.ICON_SETTINGS_URL)
                                .hideAllComponents())
                        .displayModifier((context, item) -> item
                                .replace(builder -> builder.with(this.getObject(context).placeholders())))
                        .action(context -> this.plugin.showDialog(context.getPlayer(), LinksDialogKeys.LINK_URL,
                                this.getObject(context), () -> context.getViewer().refresh()))
                        .build())
                .slots(SLOT_URL)
                .build());

        this.addDefaultButton("command", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.COMPARATOR)
                                .localized(LinksLang.ICON_SETTINGS_COMMAND)
                                .hideAllComponents())
                        .displayModifier((context, item) -> item
                                .replace(builder -> builder.with(this.getObject(context).placeholders())))
                        .action(context -> this.plugin.showDialog(context.getPlayer(), LinksDialogKeys.LINK_COMMAND,
                                this.getObject(context), () -> context.getViewer().refresh()))
                        .build())
                .slots(SLOT_COMMAND)
                .build());

        this.addDefaultButton("executor", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.COMMAND_BLOCK)
                                .localized(LinksLang.ICON_SETTINGS_EXECUTOR)
                                .hideAllComponents())
                        .displayModifier((context, item) -> item
                                .replace(builder -> builder.with(this.getObject(context).placeholders())))
                        .action(context -> {
                            Link link = this.getObject(context);
                            link.setExecutor(Lists.next(link.getExecutor()));
                            this.module.saveLink(link);
                            context.getViewer().refresh();
                        })
                        .build())
                .slots(SLOT_EXECUTOR)
                .build());

        this.addDefaultButton("permission", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.SHIELD)
                                .localized(LinksLang.ICON_SETTINGS_PERMISSION)
                                .hideAllComponents())
                        .displayModifier((context, item) -> item
                                .replace(builder -> builder.with(this.getObject(context).placeholders())))
                        .action(context -> this.plugin.showDialog(context.getPlayer(),
                                LinksDialogKeys.LINK_PERMISSION, this.getObject(context),
                                () -> context.getViewer().refresh()))
                        .build())
                .slots(SLOT_PERM)
                .build());

        this.addDefaultButton("priority", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.NETHER_STAR)
                                .localized(LinksLang.ICON_SETTINGS_PRIORITY)
                                .hideAllComponents())
                        .displayModifier((context, item) -> item
                                .replace(builder -> builder.with(this.getObject(context).placeholders())))
                        .action(context -> this.plugin.showDialog(context.getPlayer(), LinksDialogKeys.LINK_PRIORITY,
                                this.getObject(context), () -> context.getViewer().refresh()))
                        .build())
                .slots(SLOT_PRIORITY)
                .build());

        this.addDefaultButton("clicks", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.CLOCK)
                                .localized(LinksLang.ICON_SETTINGS_CLICKS)
                                .hideAllComponents())
                        .displayModifier((context, item) -> item
                                .replace(builder -> builder.with(this.getObject(context).placeholders())))
                        .action(context -> {
                            Link link = this.getObject(context);
                            link.resetClicks();
                            this.module.saveLink(link);
                            context.getViewer().refresh();
                        })
                        .build())
                .slots(SLOT_CLICKS)
                .build());

        this.addDefaultButton("return", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.ARROW)
                                .localized(LinksLang.ICON_EDITOR_RETURN)
                                .hideAllComponents())
                        .action(context -> this.module.openEditor(context.getPlayer()))
                        .build())
                .slots(SLOT_RETURN)
                .build());

        this.addDefaultButton("delete", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.BARRIER)
                                .localized(LinksLang.ICON_SETTINGS_DELETE)
                                .hideAllComponents())
                        .action(context -> {
                            Player player = context.getPlayer();
                            this.plugin.showDialog(player, LinksDialogKeys.LINK_DELETION, this.getObject(context),
                                    () -> this.module.openEditor(player));
                        })
                        .build())
                .slots(SLOT_DELETE)
                .build());
    }

    @Override
    public void registerActions() {

    }

    @Override
    public void registerConditions() {

    }

    @Override
    protected void onLoad(@NotNull FileConfig config) {

    }

    @Override
    protected void onClick(@NotNull ViewerContext context, @NotNull InventoryClickEvent event) {

    }

    @Override
    protected void onDrag(@NotNull ViewerContext context, @NotNull InventoryDragEvent event) {

    }

    @Override
    protected void onClose(@NotNull ViewerContext context, @NotNull InventoryCloseEvent event) {

    }

    @Override
    public void onPrepare(@NotNull ViewerContext context, @NotNull InventoryView view, @NotNull Inventory inventory,
            @NotNull List<MenuItem> items) {

    }

    @Override
    public void onReady(@NotNull ViewerContext context, @NotNull InventoryView view, @NotNull Inventory inventory) {

    }

    @Override
    public void onRender(@NotNull ViewerContext context, @NotNull InventoryView view, @NotNull Inventory inventory) {

    }
}
