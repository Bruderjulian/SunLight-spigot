package su.nightexpress.sunlight.moduleImpl.afk.command;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.afk.AfkModule;
import su.nightexpress.sunlight.moduleImpl.afk.core.AfkLang;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class AfkCommandProvider extends CommandProvider<AfkModule> {

    public AfkCommandProvider(final AfkModule module) {
        super(module, "afk");
    }

    @Override
    public void setup() {
        this.register("on", command -> command
                .withFullDescription(AfkLang.COMMAND_AFK_ON_DESC.text())
                .withPermission("sunlight.command.afk")
                .withOptionalArguments(
                        CommandArgumentConstants.targetArgument().withPermission("sunlight.command.afk.others"))
                .executes((sender, arguments) -> {
                    toggleAfkMode(sender, arguments, ToggleMode.ON);
                    return 1;
                }))
                .aliases("afk-on")
                .under("afk");

        this.register("off", command -> command
                .withFullDescription(AfkLang.COMMAND_AFK_OFF_DESC.text())
                .withPermission("sunlight.command.afk")
                .withOptionalArguments(
                        CommandArgumentConstants.targetArgument().withPermission("sunlight.command.afk.others"))
                .executes((sender, arguments) -> {
                    toggleAfkMode(sender, arguments, ToggleMode.OFF);
                    return 1;
                }))
                .aliases("afk-off")
                .under("afk");
        this.register("toggle", command -> command
                .withFullDescription(AfkLang.COMMAND_AFK_TOGGLE_DESC.text())
                .withPermission("sunlight.command.afk")
                .withOptionalArguments(
                        CommandArgumentConstants.targetArgument().withPermission("sunlight.command.afk.others"))
                .executes((sender, arguments) -> {
                    toggleAfkMode(sender, arguments, ToggleMode.TOGGLE);
                    return 1;
                }))
                .under("afk");
        this.register("list", command -> command
                .withFullDescription(AfkLang.COMMAND_AFK_LIST_DESC.text())
                .withPermission("sunlight.command.afk.list")
                .executes((sender, arguments) -> {
                    showList(sender);
                    return 1;
                }))
                .under("afk");

        this.registerRoot("afk", command -> command
                .withFullDescription(AfkLang.COMMAND_AFK_TOGGLE_DESC.text())
                .withPermission("sunlight.command.afk")
                .executes((sender, arguments) -> {
                    return this.toggleAfkMode(sender, arguments, ToggleMode.TOGGLE);
                }));
    }

    private int toggleAfkMode(final CommandSender sender, final CommandArguments arguments, final ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        return target.runAs(this.module, sender, "sunlight.command.afk.others", (user, targetPlayer) -> {
            final boolean state = mode.apply(this.module.isAfk(targetPlayer));
            if (state) {
                this.module.enterAfk(targetPlayer, false);
            } else {
                this.module.exitAfk(targetPlayer, false);
            }

            if (sender != targetPlayer) {
                this.module.sendPrefixed(AfkLang.AFK_TOGGLE_FEEDBACK, sender, builder -> builder
                        .with(CommonPlaceholders.PLAYER.resolver(targetPlayer)));
            }
        });
    }

    private int showList(final CommandSender sender) {
        final List<Player> afkPlayers = this.module.getAfkPlayers();

        if (afkPlayers.isEmpty()) {
            this.module.sendPrefixed(AfkLang.AFK_LIST_EMPTY, sender);
            return 1;
        }

        final String list = afkPlayers.stream()
                .map(player -> GREEN.wrap(player.getName()) + " "
                        + DARK_GRAY.wrap("(")
                        + ORANGE.wrap(TimeFormats.formatSince(this.module.getAfkEnterTimestamp(player),
                                TimeFormatType.LITERAL))
                        + DARK_GRAY.wrap(")"))
                .collect(Collectors.joining(DARK_GRAY.wrap(", ")));

        final PlaceholderContext contextBuilder = PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_LIST, () -> list)
                .build();
        this.module.sendPrefixed(AfkLang.AFK_LIST, sender, contextBuilder);
        return 1;
    }
}
