package su.nightexpress.sunlight.moduleImpl.glow.menu;

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
import su.nightexpress.nightcore.ui.inventory.viewer.MenuViewer;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.glow.GlowEffect;
import su.nightexpress.sunlight.moduleImpl.glow.GlowEffect.GlowType;
import su.nightexpress.sunlight.moduleImpl.glow.GlowHandler;
import su.nightexpress.sunlight.moduleImpl.glow.GlowModule;
import su.nightexpress.sunlight.moduleImpl.glow.GlowPhase;
import su.nightexpress.sunlight.moduleImpl.glow.config.GlowLang;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Player-facing glow browser. Bare {@code /glow} opens this menu.
 */
public class GlowMenu extends AbstractMenu {

    private static final int PAGE_SIZE = 27;
    private static final int FIRST_SLOT = 9;

    private static final int SLOT_DISABLE = 36;
    private static final int SLOT_TOGGLE = 40;
    private static final int SLOT_CLOSE = 44;

    private final SunLightPlugin plugin;
    private final GlowModule module;

    public GlowMenu(final SunLightPlugin plugin, final GlowModule module) {
        super(MenuType.GENERIC_9X5, GlowLang.MENU_TITLE.text());
        this.plugin = plugin;
        this.module = module;
    }

    public boolean open(final Player player) {
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
        this.addNextPageButton(41);
        this.addPreviousPageButton(39);
    }

    @Override
    protected void onLoad(final FileConfig config) {

    }

    @Override
    protected void onClick(final ViewerContext context, final InventoryClickEvent event) {

    }

    @Override
    protected void onDrag(final ViewerContext context, final InventoryDragEvent event) {

    }

    @Override
    protected void onClose(final ViewerContext context, final InventoryCloseEvent event) {

    }

    @Override
    public void onPrepare(final ViewerContext context, final InventoryView view, final Inventory inventory,
            final List<MenuItem> list) {
        final Player player = context.getPlayer();
        final MenuViewer viewer = context.getViewer();

        final GlowEffect selectedEffect = this.module.handler().getEffectiveEffect(player);
        final String selected = selectedEffect == null ? null : selectedEffect.getId();
        final boolean enabled = this.module.handler().isGlowEnabled(player);
        final boolean custom = selectedEffect != null && !this.module.handler().getEffects()
                .containsKey(selectedEffect.getId());

        final List<GlowEffect> effects = this.module.handler().getEffects().values().stream()
                .sorted(Comparator.comparing(GlowEffect::getId))
                .toList();

        viewer.setTotalPages(Math.max(1, (effects.size() + PAGE_SIZE - 1) / PAGE_SIZE));

        final int fromIndex = (viewer.getCurrentPage() - 1) * PAGE_SIZE;
        if (fromIndex < effects.size()) {
            final int toIndex = Math.min(effects.size(), fromIndex + PAGE_SIZE);
            for (int index = fromIndex; index < toIndex; index++) {
                final GlowEffect effect = effects.get(index);
                final boolean isSelected = !custom && selected != null && selected.equals(effect.getId());
                final boolean hasAccess = hasColorAccess(player, effect.getId());

                final NightItem icon = NightItem.fromType(effect.material())
                        .setDisplayName(effect.getName())
                        .setLore(this.buildLore(effect, hasAccess, isSelected));

                list.add(MenuItem.button()
                        .defaultState(icon,
                                actionContext -> this.onEffectClick(actionContext.getPlayer(), effect, hasAccess))
                        .slots(FIRST_SLOT + index - fromIndex)
                        .build());
            }
        }

        final boolean hasSelection = selected != null;
        final NightItem disableIcon = NightItem.fromType(Material.MILK_BUCKET)
                .setDisplayName(GRAY.wrap("Disable glow"))
                .setLore(hasSelection
                        ? List.of(GRAY.wrap("Currently: ") + WHITE.wrap(this.describeSelection(player, selected)),
                                "",
                                GOLD.wrap("→ " + UNDERLINED.wrap("Click to remove your glow.")))
                        : List.of(GRAY.wrap("You have no glow selected.")));
        list.add(MenuItem.button()
                .defaultState(disableIcon, actionContext -> {
                    final Player clicker = actionContext.getPlayer();
                    this.module.handler().setEffect(clicker, null);
                    this.module.sendPrefixed(GlowLang.COMMAND_GLOW_CLEAR_DONE, clicker);
                    this.show(this.plugin, clicker);
                })
                .slots(SLOT_DISABLE)
                .build());

        final NightItem toggleIcon = NightItem.fromType(enabled ? Material.GLOWSTONE_DUST : Material.GUNPOWDER)
                .setDisplayName(GRAY.wrap("Glow: ") + (enabled ? GREEN.wrap("ON") : RED.wrap("OFF")))
                .setLore(hasSelection
                        ? List.of(GRAY.wrap("Click to turn your glow " + (enabled ? "off." : "on.")))
                        : List.of(SOFT_RED.wrap("Select a color first.")));
        list.add(MenuItem.button()
                .defaultState(toggleIcon, actionContext -> {
                    final Player clicker = actionContext.getPlayer();
                    final boolean next = !this.module.handler().isGlowEnabled(clicker);
                    if (next && this.module.handler().getEffectiveEffect(clicker) == null) {
                        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_SELECTION, clicker);
                        return;
                    }
                    this.module.handler().setGlowEnabled(clicker, next);
                    this.module.sendPrefixed(next ? GlowLang.COMMAND_GLOW_ON_DONE : GlowLang.COMMAND_GLOW_OFF_DONE,
                            clicker);
                    this.show(this.plugin, clicker);
                })
                .slots(SLOT_TOGGLE)
                .build());

        final NightItem closeIcon = NightItem.fromType(Material.BARRIER)
                .setDisplayName(GRAY.wrap("Close"));
        list.add(MenuItem.button()
                .defaultState(closeIcon, actionContext -> actionContext.getViewer().closeMenu())
                .slots(SLOT_CLOSE)
                .build());
    }

    private static boolean hasColorAccess(final Player player, final String effectId) {
        if (player.hasPermission("sunlight.glow.bypass.color"))
            return true;
        return player.hasPermission("sunlight.glow.color.*")
                || player.hasPermission("sunlight.glow.color." + effectId);
    }

    private void onEffectClick(final Player player, final GlowEffect effect,
            final boolean hasAccess) {
        if (!hasAccess) {
            this.module.sendPrefixed(GlowLang.COMMAND_GLOW_ERROR_NO_ACCESS, player,
                    replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, effect::getName));
            return;
        }
        this.module.handler().setEffect(player, effect);
        this.module.sendPrefixed(GlowLang.COMMAND_GLOW_MENU_SELECTED, player,
                replacer -> replacer.with(SLPlaceholders.GENERIC_NAME, effect::getName));
        this.show(this.plugin, player);
    }

    private List<String> buildLore(final GlowEffect effect, final boolean hasAccess,
            final boolean isSelected) {
        final List<String> lore = new ArrayList<>();
        lore.add(GRAY.wrap("Type: ") + WHITE.wrap(effect.getType().name()));

        if (effect.getType() == GlowType.PHASED) {
            lore.add(GRAY.wrap("Phases: ") + WHITE.wrap(String.valueOf(effect.phaseCount())));
            final StringBuilder builder = new StringBuilder();
            for (int index = 0; index < effect.phaseCount(); index++) {
                final GlowPhase phase = effect.getPhase(index);
                builder.append(GRAY.wrap(" - ") + WHITE.wrap(
                        phase.color().toString().toLowerCase() + " (" + phase.durationTicks() + "t)"));
            }
            lore.add(builder.toString());
        } else {
            final String baseColorName = effect.getBaseColor().toString();
            lore.add(GRAY.wrap("Color: ") + "<" + baseColorName.toString() + ">" + baseColorName + "</" + baseColorName
                    + ">");
        }
        lore.add("");
        if (isSelected) {
            lore.add(GREEN.wrap("Currently selected."));
        } else if (!hasAccess) {
            lore.add(SOFT_RED.wrap("Locked."));
        } else {
            lore.add(GOLD.wrap("→ " + UNDERLINED.wrap("Click to select.")));
        }
        return lore;
    }

    private String describeSelection(final Player player, final String selected) {
        if (selected == null) {
            return "None";
        }
        final GlowEffect effect = this.module.handler().getEffect(selected);
        return effect == null ? selected : effect.getName();
    }

    @Override
    public void onReady(final ViewerContext context, final InventoryView view, final Inventory inventory) {

    }

    @Override
    public void onRender(final ViewerContext context, final InventoryView view, final Inventory inventory) {

    }
}
