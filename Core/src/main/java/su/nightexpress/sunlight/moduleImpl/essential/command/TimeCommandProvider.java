package su.nightexpress.sunlight.moduleImpl.essential.command;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.WorldArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.BukkitThing;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.Replacer;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.SLUtils;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.moduleImpl.essential.object.TimeAlias;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class TimeCommandProvider extends CommandProvider<EssentialModule> {
    public static final long MODIFIER = 1000L;
    public static final long MAX_TICKS = 24L * MODIFIER;
    public static final long MIN_TICKS = 0L;

    private static final String COMMAND_SHOW = "show";
    private static final String COMMAND_SET = "set";

    private static final Permission PERMISSION_ROOT = EssentialPerms.COMMAND.permission("time.root");
    private static final Permission PERMISSION_SHOW = EssentialPerms.COMMAND.permission("time.show");
    private static final Permission PERMISSION_SET = EssentialPerms.COMMAND.permission("time.set");

    private static final TextLocale DESCRIPTION_ROOT = LangEntry.builder("Command.Time.Root.Desc")
            .text("World time commands.");
    private static final TextLocale DESCRIPTION_SHOW = LangEntry.builder("Command.Time.Show.Desc")
            .text("Display current world time.");
    private static final TextLocale DESCRIPTION_SET_TIME = LangEntry.builder("Command.Time.SetTime.Desc")
            .text("Set world's time to %s ticks.");
    private static final TextLocale DESCRIPTION_SET_TICKS = LangEntry.builder("Command.Time.SetTicks.Desc")
            .text("Change time in a world.");

    private static final MessageLocale MESSAGE_SET_FEEDBACK = LangEntry.builder("Command.Time.Set.Done")
            .chatMessage(
                    GRAY.wrap("You have set " + WHITE.wrap(GENERIC_WORLD) + "'s time to "
                            + SOFT_YELLOW.wrap(GENERIC_TIME)
                            + " (" + WHITE.wrap(GENERIC_TOTAL + " ticks") + ")" + "."));

    private final Set<TimeAlias> timeAliases;

    public TimeCommandProvider(final EssentialModule module) {
        super(module, "time");
        this.timeAliases = new LinkedHashSet<>();

        this.module.settings().timeAliases.get().forEach((name, gameTime) -> {
            this.timeAliases.add(new TimeAlias(Utils.lowercase(name), gameTime));
        });
    }

    @Override
    public void setup() {
        this.timeAliases.forEach(timeAlias -> {
            this.register(timeAlias.name(), List.of(), command -> command
                    .withFullDescription(DESCRIPTION_SET_TIME.text().formatted(
                            String.valueOf(timeAlias.gameTime())))
                    .withPermission(PERMISSION_SET.getName())
                    .withOptionalArguments(new WorldArgument(CommandArgumentConstants.WORLD))
                    .executes((sender, arguments) -> {
                        return this.setWorldTime(sender, arguments, timeAlias.gameTime());
                    }));
        });

        this.register(COMMAND_SHOW, List.of(), command -> command
                .withFullDescription(DESCRIPTION_SHOW.text())
                .withPermission(PERMISSION_SHOW.getName())
                .withOptionalArguments(new WorldArgument(CommandArgumentConstants.WORLD))
                .executes((sender, arguments) -> {
                    return this.displayWorldTime(sender, arguments);
                }));

        this.register(COMMAND_SET, List.of(), command -> command
                .withFullDescription(DESCRIPTION_SET_TICKS.text())
                .withPermission(PERMISSION_SET.getName())
                .withArguments(new IntegerArgument(CommandArgumentConstants.TIME, (int) MIN_TICKS, (int) MAX_TICKS)
                        .replaceSuggestions(ArgumentSuggestions.stringCollection(
                                info -> IntStream.range(0, 25)
                                        .boxed()
                                        .map(hour -> hour * MODIFIER)
                                        .map(String::valueOf).toList())))
                .withOptionalArguments(new WorldArgument(CommandArgumentConstants.WORLD))
                .executes((sender, arguments) -> {
                    final Object timeArg = arguments.get(CommandArgumentConstants.TIME);
                    if (!(timeArg instanceof final Integer ticks)) {
                        return 0;
                    }
                    return this.setWorldTime(sender, arguments, ticks);
                }));

        this.registerRoot("time", command -> command
                .withFullDescription(DESCRIPTION_ROOT.text())
                .withPermission(PERMISSION_ROOT.getName()));
    }

    private int setWorldTime(final CommandSender sender, final CommandArguments arguments, final long ticks) {
        final World world = this.getWorld(sender, arguments);
        if (world == null) {
            return 0;
        }

        final long worldTime = clampTicks(ticks);
        world.setTime(worldTime);
        final LocalTime localTime = getTimeOfTicks(world.getTime());

        this.module.sendPrefixed(MESSAGE_SET_FEEDBACK, sender, replacer -> replacer
                .with(GENERIC_WORLD, () -> BukkitThing.getValue(world))
                .with(GENERIC_TIME, () -> SLUtils.formatTime(localTime))
                .with(GENERIC_TOTAL, () -> NumberUtil.format(worldTime)));
        return 1;
    }

    private int displayWorldTime(final CommandSender sender, final CommandArguments arguments) {
        final World world = this.getWorld(sender, arguments);
        if (world == null) {
            return 0;
        }

        final long worldTicks = world.getTime();
        final Replacer replacer = Replacer.create()
                .replace(GENERIC_WORLD, BukkitThing.getValue(world))
                .replace(GENERIC_TIME, SLUtils.formatTime(getTimeOfTicks(worldTicks)))
                .replace(GENERIC_TICKS, NumberUtil.format(worldTicks))
                .replace(GENERIC_GLOBAL, SLUtils.formatTime(TimeUtil.getCurrentTime()));

        final String text = String.join("\n",
                replacer.apply(this.module.settings().timeDisplayFormat.get()));
        Players.sendMessage(sender, text);
        return 1;
    }

    /**
     * Resolves the {@code world} argument, falling back to the sender's world.
     *
     * @return {@code null} when the sender is not a player and no world was given.
     */
    private World getWorld(final CommandSender sender, final CommandArguments arguments) {
        final Object worldArg = arguments.get(CommandArgumentConstants.WORLD);
        if (worldArg instanceof final World world) {
            return world;
        }

        if (sender instanceof final Player player) {
            return player.getWorld();
        }

        CoreLang.COMMAND_EXECUTION_MISSING_ARGUMENTS.withPrefix(this.module.plugin().getPrefix()).send(sender,
                replacer -> replacer.replace(CommonPlaceholders.GENERIC_COMMAND, "/" + this.getId()));
        return null;
    }

    public static long clampTicks(long ticks) {
        return Math.clamp(ticks, MIN_TICKS, MAX_TICKS);
    }

    public static LocalTime getTimeOfTicks(long ticks) {
        final double point = ticks * 3.6;

        final int hours = (int) (point / 60D / 60D);
        final int minutes = (int) ((point / 60D) % 60);
        final int seconds = (int) (point % 60);
        return LocalTime.of(hours, minutes, seconds).plusHours(6);
    }
}