package su.nightexpress.sunlight.moduleImpl.voting.menu;

import java.util.ArrayList;
import java.util.List;
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
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingLang;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteOption;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteTally;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.moduleImpl.voting.VotingPlaceholders.*;

/**
 * The list of open votes.
 * <p>
 * Whether it shows every open vote or only the ones the viewer has voted in is carried as the
 * bound object, exactly as the Reports menu carries its filter: that is view state, not a database
 * row.
 */
public class VotesMenu extends AbstractObjectMenu<VotesMenu.Data> {

    /**
     * The mode button sits on the bottom background row with the page buttons, not among the
     * items: slot 22 is inside the 9–35 item area, so with ~14+ votes an item would land on it
     * and the All/Mine toggle would disappear from the GUI.
     */
    private static final int SLOT_MODE = 40;
    private static final int FIRST_SLOT = 9;
    private static final int LAST_SLOT  = 36;

    /**
     * The bound object is view state, not a database row, exactly as in the Reports punishments
     * menu.
     */
    public record Data(boolean mineOnly) {
    }

    private final VotingModule module;

    public VotesMenu(final VotingModule module) {
        super(MenuType.GENERIC_9X5, VotingLang.MENU_LIST_TITLE.text(), Data.class);
        this.module = module;
    }

    public boolean open(final Player player) {
        return this.open(player, false);
    }

    public boolean openMyVotes(final Player player) {
        return this.open(player, true);
    }

    public boolean open(final Player player, final boolean mineOnly) {
        return this.show(player, new Data(mineOnly));
    }

    @Override
    protected String getRawTitle(final ViewerContext context) {
        return PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_MODE, () -> this.mode(this.getObject(context)))
                .build()
                .apply(super.getRawTitle(context));
    }

    private String mode(final Data data) {
        return data.mineOnly() ? VotingLang.MODE_MINE.text() : VotingLang.MODE_OPEN.text();
    }

    @Override
    public void registerActions() {

    }

    @Override
    public void registerConditions() {

    }

    @Override
    public void defineDefaultLayout() {
        this.addNextPageButton(41);
        this.addPreviousPageButton(39);

        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(0, 9).toArray());
        this.addBackgroundItem(Material.BLACK_STAINED_GLASS_PANE, IntStream.range(36, 45).toArray());

        this.addDefaultButton("mode", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.COMPARATOR)
                                .setDisplayName(VotingLang.BUTTON_FILTER.text())
                                .setLore(Lists.newList(
                                        DARK_GRAY.wrap("» " + GRAY.wrap("Showing: ") + WHITE.wrap(GENERIC_MODE)),
                                        "",
                                        GOLD.wrap("→ " + UNDERLINED.wrap("Click to switch"))))
                        .hideAllComponents())
                        .displayModifier((context, item) -> item.replace(builder -> builder
                                .with(SLPlaceholders.GENERIC_MODE, () -> this.mode(this.getObject(context)))))
                        .action(context -> this.open(context.getPlayer(), !this.getObject(context).mineOnly()))
                        .build())
                .slots(SLOT_MODE)
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
        final Player player = context.getPlayer();
        final List<Vote> votes = this.module.getVisibleVotes(player, this.getObject(context).mineOnly());

        final var viewer = context.getViewer();
        final int perPage = LAST_SLOT - FIRST_SLOT;
        viewer.setTotalPages(Math.max(1, (int) Math.ceil((double) votes.size() / perPage)));
        viewer.setCurrentPage(Math.min(viewer.getCurrentPage(), viewer.getTotalPages()));

        final int skip = (viewer.getCurrentPage() - 1) * perPage;
        int slot = FIRST_SLOT;

        for (final Vote vote : votes.stream().skip(skip).limit(perPage).toList()) {
            items.add(MenuItem.custom()
                    .defaultState(ItemState.builder()
                            .icon(this.icon(player, vote))
                            .action(ctx -> this.module.openView(ctx.getPlayer(), vote))
                            .build())
                    .slots(slot++)
                    .build());
        }
    }

    private NightItem icon(final Player player, final Vote vote) {
        final VoteTally tally = this.module.tally(vote);
        final var leader = vote.getOption(tally.leader());

        final List<String> lore = new ArrayList<>(Lists.newList(
                DARK_GRAY.wrap("» " + GRAY.wrap("Closes in: ") + WHITE.wrap(VOTE_TIME_LEFT)),
                DARK_GRAY.wrap("» " + GRAY.wrap("People voting: ") + WHITE.wrap(VOTE_PARTICIPANTS)),
                "",
                GRAY.wrap("Leading: ") + WHITE.wrap(leader == null ? VotingLang.TEXT_NO_LEADER.text()
                        : leader.getDisplay() + " (" + VotingModule.format(tally.share(leader.getKey())) + "%)")
        ));

        final String mine = this.myOptions(player, vote);
        if (mine != null) {
            lore.add("");
            lore.add(GREEN.wrap("You voted: ") + WHITE.wrap(mine));
        }
        lore.add("");
        lore.add(GOLD.wrap("→ " + UNDERLINED.wrap("Click to open")));

        final PlaceholderContext context = PlaceholderContext.builder()
                .with(vote.placeholders())
                .with(VOTE_PARTICIPANTS, () -> String.valueOf(tally.getParticipantCount()))
                .build();

        return NightItem.fromType(this.material(vote))
                .setDisplayName(context.apply(WHITE.wrap(VOTE_NAME)))
                .setLore(lore.stream().map(context::apply).toList())
                .hideAllComponents();
    }

    /**
     * @return what this player has voted for, or {@code null} when they have not voted at all.
     *         Showing only the "mine" filter would not make the others any less confusing.
     */
    private String myOptions(final Player player, final Vote vote) {
        return this.module.getBallotRepository()
                .getBallots(vote.getId(), player.getUniqueId()).stream()
                .map(ballot -> vote.getOption(ballot.optionKey()))
                .filter(java.util.Objects::nonNull)
                .map(VoteOption::getDisplay)
                .reduce((first, second) -> first + ", " + second)
                .orElse(null);
    }

    private Material material(final Vote vote) {
        return vote.getOptionList().stream()
                .map(VoteOption::getIcon)
                .map(Material::matchMaterial)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(Material.PAPER);
    }

    @Override
    public void onReady(final ViewerContext context, final InventoryView view, final Inventory inventory) {

    }

    @Override
    public void onRender(final ViewerContext context, final InventoryView view, final Inventory inventory) {

    }
}