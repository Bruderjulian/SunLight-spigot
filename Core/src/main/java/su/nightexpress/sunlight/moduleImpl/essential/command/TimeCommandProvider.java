package su.nightexpress.sunlight.moduleImpl.essential.command;

import org.bukkit.World;
import org.bukkit.permissions.Permission;

import su.nightexpress.nightcore.commands.Arguments;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.BukkitThing;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;
import su.nightexpress.nightcore.util.placeholder.Replacer;
import su.nightexpress.sunlight.SLUtils;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.config.Lang;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialModule;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialPerms;
import su.nightexpress.sunlight.moduleImpl.essential.EssentialSettings;
import su.nightexpress.sunlight.moduleImpl.essential.object.TimeAlias;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;

public class TimeCommandProvider extends CommandProvider {
        public static final long MODIFIER = 1000L;
        public static final long MAX_TICKS = 24L * MODIFIER;
        public static final long MIN_TICKS = 0L;

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

        private static final String COMMAND_SHOW = "show";
        private static final String COMMAND_SET = "set";

        private final EssentialModule module;
        private final EssentialSettings settings;
        private final Set<TimeAlias> timeAliases;

        public TimeCommandProvider(SunLightPlugin plugin, EssentialModule module, EssentialSettings settings) {
                super(plugin);
                this.module = module;
                this.settings = settings;
                this.timeAliases = new LinkedHashSet<>();

                this.settings.timeAliases.get().forEach((name, gameTime) -> {
                        this.timeAliases.add(new TimeAlias(Utils.lowercase(name), gameTime));
                });
        }

        @Override
        public void registerDefaults() {
                Map<String, String> rootChildrens = new LinkedHashMap<>();
                rootChildrens.put(COMMAND_SHOW, "show");

                this.timeAliases.forEach(timeAlias -> {
                        this.registerLiteral(timeAlias.name(), true, new String[] { timeAlias.name() },
                                        builder -> builder
                                                        .description(DESCRIPTION_SET_TIME.text().formatted(
                                                                        String.valueOf(timeAlias.gameTime())))
                                                        .permission(PERMISSION_SET)
                                                        .withArguments(Arguments.world(CommandArguments.WORLD)
                                                                        .optional())
                                                        .executes((context, arguments) -> this.setWorldTime(context,
                                                                        arguments, timeAlias.gameTime())));
                        rootChildrens.put(timeAlias.name(), timeAlias.name());
                });

                this.registerLiteral(COMMAND_SHOW, true, new String[] { "worldtime" }, builder -> builder
                                .description(DESCRIPTION_SHOW.text())
                                .permission(PERMISSION_SHOW)
                                .withArguments(Arguments.world(CommandArguments.WORLD).optional())
                                .executes(this::displayWorldTime));

                this.registerLiteral(COMMAND_SET, true, new String[] { "setworldtime" }, builder -> builder
                                .description(DESCRIPTION_SET_TICKS)
                                .permission(PERMISSION_SET)
                                .withArguments(
                                                Arguments.integer(CommandArguments.TIME, (int) MIN_TICKS,
                                                                (int) MAX_TICKS)
                                                                .localized(Lang.COMMAND_ARGUMENT_NAME_TIME)
                                                                .suggestions((reader, context) -> IntStream.range(0, 25)
                                                                                .boxed()
                                                                                .map(hour -> hour * MODIFIER)
                                                                                .map(String::valueOf).toList()),
                                                Arguments.world(CommandArguments.WORLD).optional())
                                .executes((context, arguments) -> this.setWorldTime(context, arguments,
                                                arguments.getInt(CommandArguments.TIME))));

                this.registerRoot("Time", true, new String[] { "time" }, rootChildrens, builder -> builder
                                .description(DESCRIPTION_ROOT)
                                .permission(PERMISSION_ROOT));
        }

        private boolean setWorldTime(CommandContext context, ParsedArguments arguments, long ticks) {
                World world = this.getWorld(context, arguments, CommandArguments.WORLD);
                if (world == null)
                        return false;

                long worldTime = clampTicks(ticks);
                world.setTime(worldTime);
                LocalTime localTime = getTimeOfTicks(world.getTime());

                this.module.sendPrefixed(MESSAGE_SET_FEEDBACK, context.getSender(), replacer -> replacer
                                .with(GENERIC_WORLD, () -> BukkitThing.getValue(world))
                                .with(GENERIC_TIME, () -> SLUtils.formatTime(localTime))
                                .with(GENERIC_TOTAL, () -> NumberUtil.format(worldTime)));
                return true;
        }

        private boolean displayWorldTime(CommandContext context, ParsedArguments arguments) {
                World world = this.getWorld(context, arguments, CommandArguments.WORLD);
                if (world == null)
                        return false;

                long worldTicks = world.getTime();
                Replacer replacer = Replacer.create()
                                .replace(GENERIC_WORLD, BukkitThing.getValue(world))
                                .replace(GENERIC_TIME, SLUtils.formatTime(getTimeOfTicks(worldTicks)))
                                .replace(GENERIC_TICKS, NumberUtil.format(worldTicks))
                                .replace(GENERIC_GLOBAL, SLUtils.formatTime(TimeUtil.getCurrentTime()));

                String text = String.join("\n", replacer.apply(this.settings.timeDisplayFormat.get()));
                Players.sendMessage(context.getSender(), text);
                return true;
        }

        public static long clampTicks(long ticks) {
                return Math.clamp(ticks, MIN_TICKS, MAX_TICKS);
        }

        public static LocalTime getTimeOfTicks(long ticks) {
                double point = ticks * 3.6;

                int hours = (int) (point / 60D / 60D);
                int minutes = (int) ((point / 60D) % 60);
                int seconds = (int) (point % 60);
                return LocalTime.of(hours, minutes, seconds).plusHours(6);
        }
}
