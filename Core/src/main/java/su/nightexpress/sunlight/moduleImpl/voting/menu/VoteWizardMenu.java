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
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.ui.dialog.wrap.DialogKey;
import su.nightexpress.nightcore.ui.inventory.action.ObjectActionContext;
import su.nightexpress.nightcore.ui.inventory.item.ItemState;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import su.nightexpress.nightcore.ui.inventory.menu.AbstractObjectMenu;
import su.nightexpress.nightcore.ui.inventory.viewer.ViewerContext;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.voting.dialog.VoteWizardDialogKeys;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteDraft;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

/**
 * Guided vote creation: key, question, duration and choices without memorizing
 * the {@code /voting create} + {@code /voting option} + {@code /voting rules} flow.
 * <p>
 * The draft is never persisted until confirm: this menu is a different door into the
 * same {@code VotingModule#create} validation path, not a second implementation.
 */
public class VoteWizardMenu extends AbstractObjectMenu<VoteDraft> {

    private static final int[] OPTION_SLOTS = {19, 20, 21, 22, 23, 24, 25};
    private static final int SLOT_KEY = 10;
    private static final int SLOT_QUESTION = 11;
    private static final int SLOT_DESCRIPTION = 12;
    private static final int SLOT_DURATION = 13;
    private static final int SLOT_SILENT = 14;
    private static final int SLOT_ADD_OPTION = 15;
    private static final int SLOT_CREATE = 16;
    private static final int SLOT_CANCEL = 40;

    private final VotingModule module;

    public VoteWizardMenu(final VotingModule module) {
        super(MenuType.GENERIC_9X5, "Vote Wizard", VoteDraft.class);
        this.module = module;
    }

    public boolean open(final Player player, final VoteDraft draft) {
        return this.show(player, draft);
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

        this.addDefaultButton("key", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.NAME_TAG)
                                .setDisplayName(GOLD.wrap("Key"))
                                .setLore(Lists.newList(
                                        GRAY.wrap("Current: ") + WHITE.wrap("draft-key"),
                                        "",
                                        GOLD.wrap("→ Click to edit")))
                                .hideAllComponents())
                        .displayModifier((context, item) -> item.replace(builder -> builder
                                .with("draft-key", () -> {
                                    final String key = this.getObject(context).getKey();
                                    return key.isBlank() ? "—" : key;
                                })))
                        .action(this.createObjectAction(ctx -> this.openDialog(ctx, VoteWizardDialogKeys.WIZARD_KEY)))
                        .build())
                .slots(SLOT_KEY)
                .build());

        this.addDefaultButton("question", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.WRITABLE_BOOK)
                                .setDisplayName(GOLD.wrap("Question"))
                                .setLore(Lists.newList(
                                        GRAY.wrap("Current: ") + WHITE.wrap("draft-question"),
                                        "",
                                        GOLD.wrap("→ Click to edit")))
                                .hideAllComponents())
                        .displayModifier((context, item) -> item.replace(builder -> builder
                                .with("draft-question", () -> {
                                    final String question = this.getObject(context).getQuestion();
                                    return question.isBlank() ? "—" : question;
                                })))
                        .action(this.createObjectAction(ctx -> this.openDialog(ctx, VoteWizardDialogKeys.WIZARD_QUESTION)))
                        .build())
                .slots(SLOT_QUESTION)
                .build());

        this.addDefaultButton("description", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.PAPER)
                                .setDisplayName(GOLD.wrap("Description"))
                                .setLore(Lists.newList(
                                        GRAY.wrap("Current: ") + WHITE.wrap("draft-description"),
                                        "",
                                        GOLD.wrap("→ Click to edit")))
                                .hideAllComponents())
                        .displayModifier((context, item) -> item.replace(builder -> builder
                                .with("draft-description", () -> {
                                    final String description = this.getObject(context).getDescription();
                                    return description.isBlank() ? "—" : description;
                                })))
                        .action(this.createObjectAction(ctx -> this.openDialog(ctx, VoteWizardDialogKeys.WIZARD_DESCRIPTION)))
                        .build())
                .slots(SLOT_DESCRIPTION)
                .build());

        this.addDefaultButton("duration", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.CLOCK)
                                .setDisplayName(GOLD.wrap("Duration"))
                                .setLore(Lists.newList(
                                        GRAY.wrap("Current: ") + WHITE.wrap("draft-duration"),
                                        "",
                                        GOLD.wrap("→ Click to edit")))
                                .hideAllComponents())
                        .displayModifier((context, item) -> item.replace(builder -> builder
                                .with("draft-duration", () -> this.getObject(context).getDurationSeconds() + "s")))
                        .action(this.createObjectAction(ctx -> this.openDialog(ctx, VoteWizardDialogKeys.WIZARD_DURATION)))
                        .build())
                .slots(SLOT_DURATION)
                .build());

        this.addDefaultButton("silent", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.GRAY_DYE)
                                .setDisplayName(GOLD.wrap("Silent"))
                                .setLore(Lists.newList(
                                        GRAY.wrap("Suppress announcements: ") + WHITE.wrap("draft-silent"),
                                        "",
                                        GOLD.wrap("→ Click to toggle")))
                                .hideAllComponents())
                        .displayModifier((context, item) -> item.replace(builder -> builder
                                .with("draft-silent", () -> CoreLang.STATE_YES_NO.get(this.getObject(context).isSilent()))))
                        .action(this.createObjectAction(ctx -> {
                            final VoteDraft draft = ctx.getObject();
                            draft.setSilent(!draft.isSilent());
                            ctx.getViewer().refresh();
                        }))
                        .build())
                .slots(SLOT_SILENT)
                .build());

        this.addDefaultButton("add_option", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.EMERALD)
                                .setDisplayName(GREEN.wrap("Add Choice"))
                                .setLore(Lists.newList(GOLD.wrap("→ Click to add"))))
                        .action(this.createObjectAction(ctx -> this.openDialog(ctx, VoteWizardDialogKeys.WIZARD_OPTION)))
                        .build())
                .slots(SLOT_ADD_OPTION)
                .build());

        this.addDefaultButton("create", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.LIME_DYE)
                                .setDisplayName(GREEN.wrap("Create Vote"))
                                .setLore(Lists.newList(
                                        GRAY.wrap("Creates the vote from this draft."),
                                        "",
                                        GOLD.wrap("→ Click to confirm")))
                                .hideAllComponents())
                        .action(this.createObjectAction(ctx -> {
                            final Player player = ctx.getPlayer();
                            if (!this.module.completeWizard(player)) {
                                ctx.getViewer().refresh();
                            }
                        }))
                        .build())
                .slots(SLOT_CREATE)
                .build());

        this.addDefaultButton("cancel", MenuItem.button()
                .defaultState(ItemState.builder()
                        .icon(NightItem.fromType(Material.BARRIER)
                                .setDisplayName(RED.wrap("Cancel"))
                                .hideAllComponents())
                        .action(this.createObjectAction(ctx -> this.module.cancelWizard(ctx.getPlayer())))
                        .build())
                .slots(SLOT_CANCEL)
                .build());
    }

    private void openDialog(final ObjectActionContext<VoteDraft> context, final DialogKey<VoteDraft> key) {
        this.plugin.showDialog(context.getPlayer(), key, context.getObject(), () -> context.getViewer().refresh());
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
        final VoteDraft draft = this.getObject(context);
        int index = 0;
        for (final var entry : draft.getOptions().entrySet()) {
            if (index >= OPTION_SLOTS.length) {
                break;
            }
            final String key = entry.getKey();
            final String display = entry.getValue();
            items.add(MenuItem.button()
                    .defaultState(ItemState.builder()
                            .icon(NightItem.fromType(Material.PAPER)
                                    .setDisplayName(WHITE.wrap(display))
                                    .setLore(Lists.newList(
                                            DARK_GRAY.wrap("Key: " + GRAY.wrap(key)),
                                            "",
                                            RED.wrap("→ Click to remove")))
                                    .hideAllComponents())
                            .action(ctx -> {
                                draft.removeOption(key);
                                ctx.getViewer().refresh();
                            })
                            .build())
                    .slots(OPTION_SLOTS[index++])
                    .build());
        }

        if (!draft.isValid()) {
            final List<String> lore = new ArrayList<>(Lists.newList(
                    GRAY.wrap(draft.validationError()),
                    "",
                    GRAY.wrap("Key: ") + WHITE.wrap(draft.getKey().isBlank() ? "—" : draft.getKey()),
                    GRAY.wrap("Question: ") + WHITE.wrap(draft.getQuestion().isBlank() ? "—" : draft.getQuestion()),
                    GRAY.wrap("Duration: ") + WHITE.wrap(draft.getDurationSeconds() + "s"),
                    GRAY.wrap("Choices: ") + WHITE.wrap(String.valueOf(draft.getOptions().size()))));
            items.add(MenuItem.button()
                    .defaultState(ItemState.builder()
                            .icon(NightItem.fromType(Material.RED_DYE)
                                    .setDisplayName(RED.wrap("Not ready yet"))
                                    .setLore(lore)
                                    .hideAllComponents())
                            .build())
                    .slots(31)
                    .build());
        }
    }

    @Override
    public void onReady(final ViewerContext context, final InventoryView view, final Inventory inventory) {
    }

    @Override
    public void onRender(final ViewerContext context, final InventoryView view, final Inventory inventory) {
    }
}
