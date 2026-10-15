package su.nightexpress.sunlight.moduleImpl.voting.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingLang;
import su.nightexpress.sunlight.moduleImpl.voting.model.GateType;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteOption;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteTally;
import su.nightexpress.sunlight.utils.EconomyUtils;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.moduleImpl.voting.VotingPlaceholders.*;

/**
 * One poll, with a row of clickable choices.
 * <p>
 * Every route out of this menu goes through {@link VotingModule#cast}, so a choice that the
 * command line would refuse is refused here too, with the same explanation. The lore states the
 * requirement up front rather than letting a player click first and find out afterwards.
 */
public class VoteViewMenu extends AbstractObjectMenu<VoteViewMenu.Data> {

    /**
     * Choices fill two rows; the back button sits on the bottom background row, not among them.
     * Slot 22 is inside the second option row, so a seventh choice would have landed on the back
     * button and polls with more than six choices hid the rest entirely.
     */
    private static final int[] OPTION_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int SLOT_BACK = 40;

    public record Data(Vote vote) {
    }

    private final VotingModule module;

    public VoteViewMenu(final VotingModule module) {
        super(MenuType.GENERIC_9X5, VotingLang.MENU_VIEW_TITLE.text(), Data.class);
        this.module = module;
    }

    public boolean open(final Player player, final Vote vote) {
        return this.show(player, new Data(vote));
    }

    @Override
    protected String getRawTitle(final ViewerContext context) {
        return PlaceholderContext.builder()
                .with(this.getObject(context).vote().placeholders())
                .build()
                .apply(super.getRawTitle(context));
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
        this.addBackgroundItem(Material.GRAY_STAINED_GLASS_PANE, IntStream.range(9, 45).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(45, 54).toArray());

        this.addNextPageButton(43);
        this.addPreviousPageButton(39);

        this.addDefaultButton("back", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.ARROW)
                                .setDisplayName(VotingLang.BUTTON_BACK.text())
                                .hideAllComponents())
                        .action(context -> this.module.openMenu(context.getPlayer()))
                        .build())
                .slots(SLOT_BACK)
                .build());
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
            final List<MenuItem> items) {
        final Vote vote = this.getObject(context).vote();
        final Player player = context.getPlayer();
        final VoteTally tally = this.module.tally(vote);

        final List<VoteOption> options = vote.getOptionList();
        final var viewer = context.getViewer();

        // A description is the poll's own explanation of the question, so it goes into the
        // window title rather than sitting on one item that a multi-page poll scrolls away.
        if (vote.hasDescription() && viewer.getCurrentPage() <= 1) {
            view.setTitle(view.getTitle() + "\n" + GRAY.wrap(vote.getDescription()));
        }

        viewer.setTotalPages(Math.max(1, (int) Math.ceil((double) options.size() / OPTION_SLOTS.length)));
        viewer.setCurrentPage(Math.min(viewer.getCurrentPage(), viewer.getTotalPages()));

        final int skip = (viewer.getCurrentPage() - 1) * OPTION_SLOTS.length;
        final List<VoteOption> page = options.stream().skip(skip).limit(OPTION_SLOTS.length).toList();

        for (int index = 0; index < page.size(); index++) {
            final VoteOption option = page.get(index);
            items.add(MenuItem.custom()
                    .defaultState(ItemState.builder()
                            .icon(this.icon(player, vote, option, tally))
                            .action(ctx -> this.module.cast(ctx.getPlayer(), vote, option.getKey()))
                            .build())
                    .slots(OPTION_SLOTS[index])
                    .build());
        }
    }

    private NightItem icon(final Player player, final Vote vote, final VoteOption option, final VoteTally tally) {
        final boolean held = this.isHeld(player, vote, option);
        final boolean exposed = vote.isExposed();

        final List<String> lore = new ArrayList<>(Lists.newList(
                DARK_GRAY.wrap("• ") + WHITE.wrap(exposed ? COUNT + " "
                        + (tally.count(option.getKey()) == 1 ? "vote" : "votes")
                        : VotingLang.SECRET_HIDDEN.text())
                        + (exposed ? GRAY.wrap(" (" + SCORE + " weight)") : ""),
                DARK_GRAY.wrap("• ") + (exposed ? GRAY.wrap(PERCENT + "% of the vote")
                        : GRAY.wrap(VotingLang.SECRET_HIDDEN.text()))));

        this.requirement(player, option).ifPresent(lore::add);
        lore.add("");
        lore.add(this.hint(player, vote, held));

        final PlaceholderContext context = PlaceholderContext.builder()
                .with(vote.placeholders())
                .with(option.placeholders())
                .with(COUNT, () -> String.valueOf(tally.count(option.getKey())))
                .with(SCORE, () -> VotingModule.format(tally.score(option.getKey())))
                .with(PERCENT, () -> VotingModule.format(tally.share(option.getKey())))
                .build();

        return NightItem.fromType(this.material(option))
                .setDisplayName(context.apply(held ? GREEN.wrap(OPTION_DISPLAY) : WHITE.wrap(OPTION_DISPLAY)))
                .setLore(lore.stream().map(context::apply).toList())
                .hideAllComponents();
    }

    /**
     * States what this player would have to satisfy to pick this choice, before they click. An
     * empty result means there is nothing to say: either the choice is open to them, or they have
     * already met it.
     */
    private Optional<String> requirement(final Player player, final VoteOption option) {
        final List<String> lines = new ArrayList<>();

        // Only states a cost the player will actually pay: exempt and bypassing players are
        // charged nothing, so showing them the list price would be wrong.
        if (option.hasGate(GateType.PAYMENT) && this.module.resolveCost(option, player) > 0D) {
            lines.add(YELLOW.wrap("Cost: ") + WHITE.wrap(COST));
        }
        if (option.hasGate(GateType.PERMISSION) && option.requiresPermission()) {
            lines.add(GRAY.wrap("Requires permission ") + WHITE.wrap(option.getPermission()));
        }
        if (option.hasGate(GateType.RANK) && option.requiresRank()) {
            lines.add(GRAY.wrap("Requires rank ") + WHITE.wrap(option.getRank()));
        }
        if (option.hasGate(GateType.WORLD) && !option.getWorlds().isEmpty()) {
            lines.add(GRAY.wrap("Only in: ") + WHITE.wrap(String.join(", ", option.getWorlds())));
        }
        if (option.hasGate(GateType.PLAYTIME) && option.getMinPlaytimeHours() > 0) {
            lines.add(GRAY.wrap("Requires ")
                    + WHITE.wrap(option.getMinPlaytimeHours() + "h") + GRAY.wrap(" played"));
        }
        if (option.hasGate(GateType.PAYMENT) && !option.isFree() && EconomyUtils.hasCurrency()
                && !EconomyUtils.canAfford(player, this.module.resolveCost(option, player))) {
            lines.add(RED.wrap("You cannot afford this yet."));
        }

        if (lines.isEmpty()) {
            return Optional.empty();
        }
        lines.add(0, "");
        return Optional.of(String.join("\n", lines));
    }

    private String hint(final Player player, final Vote vote, final boolean held) {
        final boolean free = vote.isChangeAllowed() || this.module.canChange(player);
        if (!free && (held || this.heldCount(player, vote) > 0)) {
            return RED.wrap("→ Changes are locked");
        }
        if (held) {
            return RED.wrap("→ " + UNDERLINED.wrap("Click to take your vote back"));
        }
        // A click in a single-vote poll that already holds a ballot elsewhere replaces it,
        // so advertising a plain "vote" would understate what the click actually does.
        if (!vote.getRules().allowsMultipleVotes() && this.heldCount(player, vote) > 0) {
            return RED.wrap("→ " + UNDERLINED.wrap("Click to change your vote"));
        }
        return this.holds(player, vote);
    }

    /**
     * In a poll that allows more than one vote, saying how many slots the player has left is the
     * difference between the cap feeling arbitrary and feeling like the rule it is.
     */
    private String holds(final Player player, final Vote vote) {
        if (!vote.getRules().allowsMultipleVotes()) {
            return GOLD.wrap("→ " + UNDERLINED.wrap("Click to vote"));
        }

        final int left = vote.getRules().votesLeft(this.heldCount(player, vote));
        if (left == Integer.MAX_VALUE) {
            return GOLD.wrap("→ " + UNDERLINED.wrap("Click to vote (unlimited)"));
        }
        return GOLD.wrap("→ " + UNDERLINED.wrap("Click to vote (") + WHITE.wrap(String.valueOf(left))
                + GOLD.wrap(" left)"));
    }

    private int heldCount(final Player player, final Vote vote) {
        return this.module.getBallotRepository().getBallots(vote.getId(), player.getUniqueId()).size();
    }

    private boolean isHeld(final Player player, final Vote vote, final VoteOption option) {
        return this.module.getBallotRepository().getBallots(vote.getId(), player.getUniqueId()).stream()
                .anyMatch(ballot -> ballot.optionKey().equalsIgnoreCase(option.getKey()));
    }

    private Material material(final VoteOption option) {
        final Material material = Material.matchMaterial(option.getIcon());
        return material == null ? Material.PAPER : material;
    }

    @Override
    public void onReady(final ViewerContext context, final InventoryView view, final Inventory inventory) {

    }

    @Override
    public void onRender(final ViewerContext context, final InventoryView view, final Inventory inventory) {

    }
}