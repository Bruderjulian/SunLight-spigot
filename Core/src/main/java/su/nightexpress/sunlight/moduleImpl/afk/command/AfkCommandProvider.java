package su.nightexpress.sunlight.moduleImpl.afk.command;

import java.util.ArrayList;
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
import su.nightexpress.sunlight.moduleImpl.afk.core.AfkPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class AfkCommandProvider extends CommandProvider<AfkModule> {

    private static final String COMMAND_TOGGLE = "toggle";
    private static final String COMMAND_ON = "on";
    private static final String COMMAND_OFF = "off";

    private static final String LIST_KEYWORD = "list";

    public AfkCommandProvider(final AfkModule module) {
        super(module, "afk");
    }

    @Override
    public void setup() {
        this.register(COMMAND_TOGGLE, List.of(), command -> this.buildCommand(command, ToggleMode.TOGGLE));
        this.register(COMMAND_ON, List.of(), command -> this.buildCommand(command, ToggleMode.ON));
        this.register(COMMAND_OFF, List.of(), command -> this.buildCommand(command, ToggleMode.OFF));
    }

    private void buildCommand(final dev.jorel.commandapi.CommandAPICommand command, final ToggleMode mode) {
        command
                .withFullDescription(switch (mode) {
                    case ON -> AfkLang.COMMAND_AFK_ON_DESC.text();
                    case OFF -> AfkLang.COMMAND_AFK_OFF_DESC.text();
                    case TOGGLE -> AfkLang.COMMAND_AFK_TOGGLE_DESC.text();
                })
                .withPermission(AfkPerms.COMMAND_AFK.getName())
                .withOptionalArguments(CommandArgumentConstants.targetArgument(info -> this.afkTargetSuggestions()))
                .executes((sender, arguments) -> {
                    return this.toggleAfkMode(sender, arguments, mode);
                });
    }

    private List<String> afkTargetSuggestions() {
        final List<String> suggestions = new ArrayList<>(CommandArgumentConstants.onlinePlayerNames());
        suggestions.removeIf(LIST_KEYWORD::equalsIgnoreCase);
        suggestions.addFirst(LIST_KEYWORD);
        return suggestions;
    }

    private int toggleAfkMode(final CommandSender sender, final CommandArguments arguments, final ToggleMode mode) {
        final CommandArgumentConstants.Target target = CommandArgumentConstants.target(arguments);
        if (target == null) {
            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
            return 0;
        }

        if (target.playerName() != null && target.playerName().equalsIgnoreCase(LIST_KEYWORD)) {
            return this.showList(sender);
        }

        return target.runAs(this.module, sender, AfkPerms.COMMAND_AFK_OTHERS.getName(), (user, targetPlayer) -> {
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
                        + DARK_GRAY.wrap("(") + ORANGE.wrap(TimeFormats.formatSince(this.module.getAfkEnterTimestamp(player),
                                TimeFormatType.LITERAL)) + DARK_GRAY.wrap(")"))
                .collect(Collectors.joining(DARK_GRAY.wrap(", ")));

        final PlaceholderContext contextBuilder = PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_LIST, () -> list)
                .build();
        this.module.sendPrefixed(AfkLang.AFK_LIST, sender, contextBuilder);
        return 1;
    }
}
