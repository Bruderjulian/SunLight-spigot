package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntityTypeArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.LangUtil;
import su.nightexpress.nightcore.util.LocationUtil;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GRAY;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SOFT_YELLOW;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_AMOUNT;
import static su.nightexpress.sunlight.SLPlaceholders.GENERIC_TYPE;

@Deprecated
public class SpawnMobCommandProvider extends CommandProvider<EssentialModule> {

    public static final String PERMISSION = EssentialPerms.COMMAND + ".spawnmob";

    public static final TextLocale DESCRIPTION = LangEntry.builder("Command.Mob.Spawn.Desc").text("Spawn a mob.");

    public static final MessageLocale MESSAGE_SPAWNED_FEEDBACK = LangEntry.builder("Command.Mob.Spawn.Done")
            .chatMessage(
                    GRAY.wrap("You have spawned "
                            + SOFT_YELLOW.wrap(GENERIC_AMOUNT + "x " + GENERIC_TYPE)
                            + "."));

    private int spawnLimit;

    public SpawnMobCommandProvider(final EssentialModule module) {
        super(module, "spawnmob");
    }

    /*
     * @Override
     * protected void loadSettings( FileConfig config, String path) {
     * this.spawnLimit = Math.max(1, config.get(ConfigTypes.INT, path +
     * ".SpawnLimit", 10,
     * "Sets max. allowed mobs amount value per command execution."
     * ));
     * }
     */

    @Override
    public void setup() {
        this.register("spawnmob", command -> command
                .withRequirement(sender -> sender instanceof Player)
                .withFullDescription(DESCRIPTION.text())
                .withPermission(PERMISSION)
                .withArguments(new EntityTypeArgument(CommandArgumentConstants.TYPE)
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(
                                info -> spawnableTypes())))
                .withOptionalArguments(new IntegerArgument(CommandArgumentConstants.AMOUNT, 1)
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(
                                info -> IntStream.range(1, this.spawnLimit).boxed().map(String::valueOf).toList())))
                .executes((sender, arguments) -> {
                    return this.spawnMob(sender, arguments);
                }));
    }

    private static List<String> spawnableTypes() {
        return Arrays.stream(EntityType.values())
                .filter(EntityType::isSpawnable)
                .map(Enum::name)
                .map(String::toLowerCase)
                .toList();
    }

    private int spawnMob(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final Object typeArg = arguments.get(CommandArgumentConstants.TYPE);
        if (!(typeArg instanceof final EntityType entityType) || !entityType.isSpawnable()) {
            return 0;
        }

        final Object amountArg = arguments.get(CommandArgumentConstants.AMOUNT);
        final int amount = Math.min(this.spawnLimit, amountArg instanceof final Integer value ? value : 1);

        final Location location = LocationUtil
                .setCenter2D(player.getTargetBlock(null, 100).getRelative(BlockFace.UP).getLocation());
        for (int count = 0; count < amount; count++) {
            player.getWorld().spawnEntity(location, entityType);
        }

        MESSAGE_SPAWNED_FEEDBACK.message().send(sender, replacer -> replacer
                .replace(SLPlaceholders.GENERIC_AMOUNT, String.valueOf(amount))
                .replace(SLPlaceholders.GENERIC_TYPE, LangUtil.getSerializedName(entityType)));
        return 1;
    }
}