package su.nightexpress.sunlight.moduleImpl.playtime.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
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
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeModule;
import su.nightexpress.sunlight.moduleImpl.playtime.PlaytimeProperties;
import su.nightexpress.sunlight.moduleImpl.playtime.config.PlaytimeLang;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimeMilestone;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;
import su.nightexpress.sunlight.user.SunUser;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Player-facing statistics overview: totals per period, session, streaks,
 * goal progress bars and milestone progress.
 */
public class PlaytimeStatsMenu extends AbstractMenu {

    private final PlaytimeModule module;
    private final UUID targetId;

    public PlaytimeStatsMenu(PlaytimeModule module, UUID targetId) {
        super(MenuType.GENERIC_9X5, PlaytimeLang.MENU_TITLE_STATS.text());
        this.module = module;
        this.targetId = targetId;
    }

    public boolean open(SunLightPlugin plugin, Player viewer) {
        return this.show(plugin, viewer);
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
        Player viewer = context.getPlayer();
        SunUser user = this.module.userManager().getOrFetch(this.targetId).orElse(null);
        if (user == null) {
            list.add(MenuItem.builder()
                .defaultState(NightItem.fromType(Material.BARRIER)
                    .setDisplayName(RED.wrap("Data unavailable"))
                    .setLore(List.of(GRAY.wrap("This player has no stored data.")))
                    .hideAllComponents())
                .slots(22)
                .build());
            return;
        }
        this.module.ensureRollover(user);

        list.add(this.headItem(user, 4));

        list.add(this.periodItem(Material.ENDER_EYE, 11, "Year", this.module.getEffectivePlaytime(user, PlaytimePeriod.YEAR)));
        list.add(this.periodItem(Material.MAP, 12, "Month", this.module.getEffectivePlaytime(user, PlaytimePeriod.MONTH)));
        list.add(this.periodItem(Material.CLOCK, 13, "Week", this.module.getEffectivePlaytime(user, PlaytimePeriod.WEEK)));
        list.add(this.periodItem(Material.SUNFLOWER, 14, "Day", this.module.getEffectivePlaytime(user, PlaytimePeriod.DAY)));
        list.add(this.sessionItem(user, 15));

        list.add(this.streakItem(Material.FIREWORK_STAR, 29, "Daily streak",
            Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.DAILY_STREAK)), "day"));
        list.add(this.streakItem(Material.FIREWORK_STAR, 30, "Weekly streak",
            Math.max(0, user.getPropertyOrDefault(PlaytimeProperties.WEEKLY_STREAK)), "week"));
        list.add(this.milestoneItem(user, 31));

        list.add(this.goalItem(32, "Daily", this.module.getEffectivePlaytime(user, PlaytimePeriod.DAY),
            this.module.getEffectiveGoalMs(user, true, false, false)));
        list.add(this.goalItem(33, "Weekly", this.module.getEffectivePlaytime(user, PlaytimePeriod.WEEK),
            this.module.getEffectiveGoalMs(user, false, true, false)));
        list.add(this.goalItem(34, "Monthly", this.module.getEffectivePlaytime(user, PlaytimePeriod.MONTH),
            this.module.getEffectiveGoalMs(user, false, false, true)));

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.GOLD_INGOT)
                .setDisplayName(GOLD.wrap(PlaytimeLang.MENU_BUTTON_TOP.text()))
                .setLore(List.of(GRAY.wrap("View the leaderboard."), "", GOLD.wrap("→ " + UNDERLINED.wrap("Click to open."))))
                .hideAllComponents(),
                actionContext -> this.module.openTopMenu(actionContext.getPlayer(), PlaytimePeriod.ALLTIME))
            .slots(39)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.SUNFLOWER)
                .setDisplayName(GRAY.wrap("Refresh"))
                .setLore(List.of(GRAY.wrap("Update all numbers."), "", GOLD.wrap("→ " + UNDERLINED.wrap("Click to refresh."))))
                .hideAllComponents(),
                actionContext -> this.module.openStatsMenu(actionContext.getPlayer(), user))
            .slots(40)
            .build());

        list.add(MenuItem.builder()
            .defaultState(NightItem.fromType(Material.BARRIER)
                .setDisplayName(GRAY.wrap(PlaytimeLang.MENU_BUTTON_CLOSE.text()))
                .hideAllComponents(),
                actionContext -> actionContext.getViewer().closeMenu())
            .slots(41)
            .build());

        if (!viewer.getUniqueId().equals(user.getId())) {
            list.add(MenuItem.builder()
                .defaultState(NightItem.fromType(Material.NAME_TAG)
                    .setDisplayName(GRAY.wrap("Viewing: ") + WHITE.wrap(user.getName()))
                    .hideAllComponents())
                .slots(0)
                .build());
        }
    }

    private MenuItem headItem(SunUser user, int slot) {
        NightItem icon = NightItem.fromType(Material.PLAYER_HEAD)
            .setDisplayName(GOLD.wrap(user.getName()))
            .setLore(List.of(
                GRAY.wrap("Total: ") + WHITE.wrap(this.module.format(this.module.getEffectivePlaytime(user, PlaytimePeriod.ALLTIME))),
                "",
                GRAY.wrap("Session: ") + WHITE.wrap(this.module.format(this.module.getSessionPlaytime(user)))))
            .hideAllComponents();
        try {
            icon.setSkullOwner(Bukkit.getOfflinePlayer(user.getId()));
        } catch (Exception ignored) {
        }
        return MenuItem.builder().defaultState(icon).slots(slot).build();
    }

    private MenuItem periodItem(Material material, int slot, String name, long ms) {
        return MenuItem.builder()
            .defaultState(NightItem.fromType(material)
                .setDisplayName(GOLD.wrap(name))
                .setLore(List.of(WHITE.wrap(this.module.format(ms))))
                .hideAllComponents())
            .slots(slot)
            .build();
    }

    private MenuItem sessionItem(SunUser user, int slot) {
        long session = this.module.getSessionPlaytime(user);
        return MenuItem.builder()
            .defaultState(NightItem.fromType(Material.FEATHER)
                .setDisplayName(GOLD.wrap("Session"))
                .setLore(List.of(WHITE.wrap(this.module.format(session))))
                .hideAllComponents())
            .slots(slot)
            .build();
    }

    private MenuItem streakItem(Material material, int slot, String name, int value, String unit) {
        List<String> lore = new ArrayList<>();
        lore.add(WHITE.wrap(String.valueOf(value)) + GRAY.wrap(" " + unit + (value == 1 ? "" : "s")));
        if (value > 1) {
            lore.add("");
            lore.add(GREEN.wrap("Keep it going!"));
        } else {
            lore.add("");
            lore.add(GRAY.wrap("Play consecutive " + unit + "s to grow it."));
        }
        return MenuItem.builder()
            .defaultState(NightItem.fromType(material)
                .setDisplayName(GOLD.wrap(name))
                .setLore(lore)
                .hideAllComponents())
            .slots(slot)
            .build();
    }

    private MenuItem goalItem(int slot, String name, long current, long goal) {
        List<String> lore = new ArrayList<>();
        if (goal <= 0L) {
            lore.add(SOFT_RED.wrap("No goal set"));
        } else {
            long percent = current <= 0L ? 0L : Math.min(100L, current * 100L / Math.max(1L, goal));
            lore.add(WHITE.wrap(this.module.format(Math.max(0L, current))) + GRAY.wrap(" / ") + WHITE.wrap(this.module.format(goal)));
            lore.add(this.progressBar(percent) + GRAY.wrap(" " + percent + "%"));
        }
        return MenuItem.builder()
            .defaultState(NightItem.fromType(Material.EXPERIENCE_BOTTLE)
                .setDisplayName(GOLD.wrap(name + " goal"))
                .setLore(lore)
                .hideAllComponents())
            .slots(slot)
            .build();
    }

    private MenuItem milestoneItem(SunUser user, int slot) {
        Map<String, PlaytimeMilestone> all = this.module.getMilestones();
        Map<String, Integer> progress = user.getPropertyOrDefault(PlaytimeProperties.MILESTONE_PROGRESS);
        long total = this.module.getEffectivePlaytime(user, PlaytimePeriod.ALLTIME);

        List<String> lore = new ArrayList<>();
        if (all.isEmpty()) {
            lore.add(GRAY.wrap("No milestones configured."));
        } else {
            long claimed = all.values().stream().filter(m -> !m.repeatable() && progress.getOrDefault(m.id(), 0) > 0).count();
            long oneShots = all.values().stream().filter(m -> !m.repeatable()).count();
            lore.add(GRAY.wrap("Claimed: ") + WHITE.wrap(claimed + "/" + oneShots));

            PlaytimeMilestone next = all.values().stream()
                .filter(m -> !m.repeatable() && progress.getOrDefault(m.id(), 0) <= 0)
                .sorted((first, second) -> Long.compare(first.thresholdMs(), second.thresholdMs()))
                .findFirst().orElse(null);
            if (next != null) {
                long percent = Math.min(100L, total * 100L / Math.max(1L, next.thresholdMs()));
                lore.add("");
                lore.add(GRAY.wrap("Next: ") + WHITE.wrap(next.name()));
                lore.add(WHITE.wrap(this.module.format(total)) + GRAY.wrap(" / ") + WHITE.wrap(this.module.format(next.thresholdMs())));
                lore.add(this.progressBar(percent) + GRAY.wrap(" " + percent + "%"));
            } else {
                lore.add("");
                lore.add(GREEN.wrap("All milestones complete!"));
            }
        }
        return MenuItem.builder()
            .defaultState(NightItem.fromType(Material.NETHER_STAR)
                .setDisplayName(GOLD.wrap("Milestones"))
                .setLore(lore)
                .hideAllComponents())
            .slots(slot)
            .build();
    }

    private String progressBar(long percent) {
        int filled = (int) Math.min(20L, Math.max(0L, percent / 5L));
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < filled; index++) builder.append("▓");
        for (int index = filled; index < 20; index++) builder.append("░");
        return GREEN.wrap(builder.substring(0, filled)) + DARK_GRAY.wrap(builder.substring(filled));
    }

    @Override
    public void onReady(ViewerContext context, InventoryView view, Inventory inventory) {

    }

    @Override
    public void onRender(ViewerContext context, InventoryView view, Inventory inventory) {

    }
}
