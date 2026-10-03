package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class CondenseCommandProvider extends CommandProvider<EssentialModule> {

    private static final Permission PERMISSION = EssentialPerms.COMMAND.permission("condense");
    private static final TextLocale DESCRIPTION = LangEntry.builder("Command.Condense.Desc")
            .text("Condense items into blocks.");

    private static final MessageLocale MESSAGE_NOTHING = LangEntry.builder("Command.Condense.Error.Nothing")
            .chatMessage(
                    Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("Nothing to condense."));

    private static final MessageLocale MESSAGE_NOT_ENOUGH = LangEntry.builder("Command.Condense.Error.NotEnough")
            .chatMessage(
                    GRAY.wrap("Not enough items to convert " + RED.wrap(GENERIC_SOURCE) + " to "
                            + RED.wrap(GENERIC_RESULT) + ". Need at least " + RED.wrap(GENERIC_AMOUNT) + "."));

    private static final MessageLocale MESSAGE_DONE = LangEntry.builder("Command.Condense.Done").chatMessage(
            GRAY.wrap("Converted " + SOFT_YELLOW.wrap("x" + GENERIC_TOTAL + " " + GENERIC_SOURCE) + " to "
                    + SOFT_YELLOW.wrap("x" + GENERIC_AMOUNT + " " + GENERIC_RESULT) + "."));

    public CondenseCommandProvider(final EssentialModule module) {
        super(module, "condense");
    }

    @Override
    public void setup() {
        this.register("condense", List.of(), command -> command
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION.getName())
                .withRequirement(sender -> sender instanceof Player)
                .executes((sender, arguments) -> {
                    return this.condense(sender, arguments);
                }));
    }

    private int condense(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        boolean done = false;
        final Set<Material> userItems = new HashSet<>();

        // Put materials to set to avoid duplicates and 'double' converts
        for (final ItemStack userItem : player.getInventory().getContents()) {
            if (userItem == null || userItem.getType().isAir()) {
                continue;
            }
            userItems.add(userItem.getType());
        }

        for (final Material userMaterial : userItems) {
            final ItemStack userItem = new ItemStack(userMaterial);
            int amountPerCraft = 0;

            ItemStack recipeResult = null;

            final Iterator<Recipe> iter = this.module.plugin().getServer().recipeIterator();

            Label_Recipe: while (iter.hasNext()) {
                final Recipe recipe = iter.next();
                if (!(recipe instanceof final ShapedRecipe shapedRecipe)) {
                    continue;
                }

                final Collection<ItemStack> recipeItems = shapedRecipe.getIngredientMap().values();

                // Only 'cuboid' crafts.
                final String[] shape = shapedRecipe.getShape();
                if (shape.length < 2) {
                    continue;
                }
                for (final String line : shape) {
                    if (line.length() != shape.length) {
                        continue Label_Recipe;
                    }
                }

                // Check for same ingredients
                int amountPerRecipe = 0;
                for (final ItemStack srcItem : recipeItems) {
                    if (srcItem == null || srcItem.getType().isAir()) {
                        continue;
                    }
                    if (!srcItem.isSimilar(userItem)) {
                        continue Label_Recipe;
                    }

                    amountPerRecipe += srcItem.getAmount();
                }

                // Get the greater recipe
                if (amountPerRecipe > amountPerCraft) {
                    amountPerCraft = amountPerRecipe;
                    recipeResult = recipe.getResult();
                }
            }

            // Check for valid recipe
            if (amountPerCraft <= 1/* || recipeResult == null */) {
                continue;
            }

            final int amountUserHas = Players.countItem(player, userItem);
            final int amountCraftCan = (int) ((double) amountUserHas / (double) amountPerCraft);
            final int amountCraftMin = recipeResult.getAmount();

            if (amountCraftCan < amountCraftMin) {
                final int finalAmountPerCraft = amountPerCraft;
                final ItemStack finalRecipeResult = recipeResult;
                this.module.sendPrefixed(MESSAGE_NOT_ENOUGH, sender, builder -> builder
                        .with(SLPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(finalAmountPerCraft))
                        .with(SLPlaceholders.GENERIC_SOURCE, () -> ItemUtil.getItemName(userItem))
                        .with(SLPlaceholders.GENERIC_RESULT, () -> ItemUtil.getItemName(finalRecipeResult)));
                continue;
            }

            for (int craft = 0; craft < amountCraftCan; craft++) {
                Players.takeItem(player, userItem, amountPerCraft);
                Players.addItem(player, recipeResult);
            }

            final ItemStack doneRecipeResult = recipeResult;
            final int totalAmountPerCraft = amountPerCraft;
            this.module.sendPrefixed(MESSAGE_DONE, sender, builder -> builder
                    .with(SLPlaceholders.GENERIC_SOURCE, () -> ItemUtil.getItemName(userItem))
                    .with(SLPlaceholders.GENERIC_RESULT, () -> ItemUtil.getItemName(doneRecipeResult))
                    .with(SLPlaceholders.GENERIC_TOTAL, () -> String.valueOf(amountCraftCan * totalAmountPerCraft))
                    .with(SLPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(amountCraftMin * amountCraftCan)));
            done = true;
        }

        if (!done) {
            this.module.sendPrefixed(MESSAGE_NOTHING, sender);
        }

        return 1;
    }
}
