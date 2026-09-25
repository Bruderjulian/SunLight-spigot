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
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.config.NametagsLang;
import su.nightexpress.sunlight.moduleImpl.nametags.model.GrantRecord;
import su.nightexpress.sunlight.moduleImpl.nametags.model.PriceMode;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.EconomyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Player-facing tag browser. Locked tags stay visible so players can see what exists and
 * why it is locked, and paid ones turn into a purchase or renew action.
 */
public class TagsMenu extends AbstractMenu {

    private static final int PAGE_SIZE = 27;
    private static final int FIRST_SLOT = 9;

    private final SunLightPlugin plugin;
    private final NametagsModule module;

    public TagsMenu(@NotNull SunLightPlugin plugin, @NotNull NametagsModule module) {
        super(MenuType.GENERIC_9X5, BLACK.wrap("Tags"));
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

        SunUser user = this.module.userManager().getOrFetch(player);
        long now = System.currentTimeMillis();
        TagDefinition selected = this.module.getSelectedTag(player);

        List<TagDefinition> tags = this.module.getCatalog().getTagsSorted();
        viewer.setTotalPages(Math.max(1, (tags.size() + PAGE_SIZE - 1) / PAGE_SIZE));
        if (tags.isEmpty()) return;

        int fromIndex = (viewer.getCurrentPage() - 1) * PAGE_SIZE;
        if (fromIndex >= tags.size()) return;

        int toIndex = Math.min(tags.size(), fromIndex + PAGE_SIZE);
        for (int index = fromIndex; index < toIndex; index++) {
            TagDefinition tag = tags.get(index);
            boolean isSelected = selected != null && selected.getId().equals(tag.getId());
            boolean hasAccess = this.module.getAccess().hasAccess(player, user, tag, now);

            NightItem icon = NightItem.fromType(this.iconOf(tag))
                .setDisplayName(tag.getDisplay())
                .setLore(this.buildLore(player, user, tag, now, hasAccess, isSelected));

            list.add(MenuItem.builder()
                .defaultState(icon, actionContext -> this.onTagClick(actionContext.getPlayer(), user, tag, now))
                .slots(FIRST_SLOT + index - fromIndex)
                .build());
        }
    }

    private @NotNull Material iconOf(@NotNull TagDefinition tag) {
        Material material = Material.matchMaterial(tag.getIconMaterial());
        return material == null ? Material.NAME_TAG : material;
    }

    private @NotNull List<String> buildLore(@NotNull Player player,
            @NotNull SunUser user,
            @NotNull TagDefinition tag,
            long now,
            boolean hasAccess,
            boolean isSelected
    ) {
        List<String> lore = new ArrayList<>(tag.getDescription());

        lore.add("");
        lore.add(GRAY.wrap("Access: ") + this.accessLabel(tag, hasAccess));

        if (tag.hasPrice() && tag.getPriceMode() == PriceMode.INTERNAL) {
            lore.add(GRAY.wrap("Price: ") + GOLD.wrap(EconomyUtils.format(tag.getPrice())));
            if (tag.isSubscription()) {
                lore.add(GRAY.wrap("Period: ") + WHITE.wrap(tag.getSubscriptionPeriod().getId()));
            }
        } else if (tag.getPriceMode() == PriceMode.EXTERNAL) {
            lore.add(GRAY.wrap("Managed by another plugin."));
        }

        GrantRecord grant = this.module.getAccess().getGrant(user, tag.getId());
        if (grant != null && grant.isActive(now) && grant.getMode() == su.nightexpress.sunlight.moduleImpl.nametags.model.TagAccessMode.SUBSCRIPTION) {
            lore.add(GRAY.wrap("Renews: ") + WHITE.wrap(this.formatExpiry(grant)));
        }

        lore.add("");
        if (isSelected) {
            lore.add(GREEN.wrap("✔ Selected"));
        } else if (hasAccess) {
            lore.add(GOLD.wrap("→ " + UNDERLINED.wrap("Click to select")));
        } else {
            lore.add(this.lockedHint(tag));
        }
        return lore;
    }

    private @NotNull String accessLabel(@NotNull TagDefinition tag, boolean hasAccess) {
        if (hasAccess && tag.requiresGrant()) {
            return NametagsLang.COMMAND_TAG_ACCESS_OWNED.get(true);
        }
        return switch (tag.getAccessMode()) {
            case FREE -> NametagsLang.COMMAND_TAG_ACCESS_FREE.get(true);
            case PERMISSION -> NametagsLang.COMMAND_TAG_ACCESS_PERMISSION.get(hasAccess);
            case PURCHASE -> NametagsLang.COMMAND_TAG_ACCESS_PURCHASE.get(hasAccess);
            case SUBSCRIPTION -> NametagsLang.COMMAND_TAG_ACCESS_SUBSCRIPTION.get(hasAccess);
        };
    }

    private @NotNull String lockedHint(@NotNull TagDefinition tag) {
        if (tag.getPriceMode() == PriceMode.EXTERNAL) {
            return RED.wrap("Get it from the shop.");
        }
        if (tag.isSubscription()) {
            return GOLD.wrap("→ " + UNDERLINED.wrap("Click to subscribe"));
        }
        if (tag.requiresGrant()) {
            return GOLD.wrap("→ " + UNDERLINED.wrap("Click to purchase"));
        }
        return SOFT_RED.wrap("You do not have access.");
    }

    private @NotNull String formatExpiry(@NotNull GrantRecord grant) {
        if (grant.isPermanent()) return "never";
        return su.nightexpress.sunlight.SLUtils.formatDate(grant.getExpiresAt());
    }

    private void onTagClick(@NotNull Player player, @NotNull SunUser user, @NotNull TagDefinition tag, long now) {
        boolean hasAccess = this.module.getAccess().hasAccess(player, user, tag, now);

        if (hasAccess) {
            this.module.selectTag(player, tag.getId());
            this.module.recompute(player);
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_SELECTED, player,
                replacer -> replacer.with(su.nightexpress.sunlight.SLPlaceholders.GENERIC_NAME, tag::getDisplay));
            this.show(this.plugin, player);
            return;
        }

        if (!tag.requiresGrant()) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_NO_ACCESS, player,
                replacer -> replacer.with(su.nightexpress.sunlight.SLPlaceholders.GENERIC_NAME, tag::getDisplay));
            return;
        }

        if (tag.getPriceMode() == PriceMode.EXTERNAL) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_EXTERNAL, player);
            return;
        }

        var granted = this.module.purchaseTag(player, tag);
        if (granted == null) {
            this.module.sendPrefixed(NametagsLang.COMMAND_TAG_ERROR_ECONOMY, player);
            return;
        }

        this.module.selectTag(player, tag.getId());
        this.module.recompute(player);
        this.module.sendPrefixed(
            tag.isSubscription() ? NametagsLang.COMMAND_TAG_SUBSCRIBED : NametagsLang.COMMAND_TAG_BOUGHT, player,
            replacer -> replacer.with(su.nightexpress.sunlight.SLPlaceholders.GENERIC_NAME, tag::getDisplay));

        this.show(this.plugin, player);
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
