package su.nightexpress.sunlight.moduleImpl.afk.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.commands.Commands;
import su.nightexpress.nightcore.commands.builder.ArgumentNodeBuilder;
import su.nightexpress.nightcore.commands.builder.LiteralNodeBuilder;
import su.nightexpress.nightcore.commands.context.CommandContext;
import su.nightexpress.nightcore.commands.context.ParsedArguments;
import su.nightexpress.nightcore.commands.exceptions.CommandSyntaxException;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.SunLightPlugin;
import su.nightexpress.sunlight.command.CommandArguments;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.command.mode.ToggleMode;
import su.nightexpress.sunlight.moduleImpl.afk.AfkModule;
import su.nightexpress.sunlight.moduleImpl.afk.core.AfkLang;
import su.nightexpress.sunlight.moduleImpl.afk.core.AfkPerms;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class AfkCommandProvider extends CommandProvider {

    private static final String COMMAND_TOGGLE = "toggle";
    private static final String COMMAND_ON = "on";
    private static final String COMMAND_OFF = "off";

    private static final String LIST_KEYWORD = "list";

    private final AfkModule module;

    public AfkCommandProvider(SunLightPlugin plugin, AfkModule module) {
        super(plugin);
        this.module = module;
    }

    @Override
    public void registerDefaults() {
        this.registerLiteral(COMMAND_TOGGLE, true, new String[] { "afk" },
                builder -> this.buildCommand(builder, ToggleMode.TOGGLE));
        this.registerLiteral(COMMAND_ON, false, new String[] { "afk-on" },
                builder -> this.buildCommand(builder, ToggleMode.ON));
        this.registerLiteral(COMMAND_OFF, false, new String[] { "afk-off" },
                builder -> this.buildCommand(builder, ToggleMode.OFF));
    }

    private void buildCommand(LiteralNodeBuilder builder, ToggleMode mode) {
        TextLocale description = switch (mode) {
            case ON -> AfkLang.COMMAND_AFK_ON_DESC;
            case OFF -> AfkLang.COMMAND_AFK_OFF_DESC;
            case TOGGLE -> AfkLang.COMMAND_AFK_TOGGLE_DESC;
        };

        builder
                .description(description)
                .permission(AfkPerms.COMMAND_AFK)
                .withArguments(
                        this.targetArgument().permission(AfkPerms.COMMAND_AFK_OTHERS))
                .withFlags(CommandArguments.FLAG_SILENT)
                .executes((context, arguments) -> this.toggleAfkMode(context, arguments, mode));
    }

    private ArgumentNodeBuilder<String> targetArgument() {
        return Commands.argument(CommandArguments.PLAYER, (context, string) -> {
            if (string.equalsIgnoreCase(LIST_KEYWORD))
                return LIST_KEYWORD;

            Player target = Bukkit.getServer().getPlayer(string);
            if (target == null) {
                throw CommandSyntaxException.custom(CoreLang.ERROR_INVALID_PLAYER);
            }
            return string;
        })
                .localized(CoreLang.COMMAND_ARGUMENT_NAME_PLAYER)
                .optional()
                .suggestions((reader, context) -> {
                    List<String> suggestions = new ArrayList<>(Players.playerNames(context.getPlayer()));
                    suggestions.removeIf(LIST_KEYWORD::equalsIgnoreCase);
                    suggestions.addFirst(LIST_KEYWORD);
                    return suggestions;
                });
    }

    private boolean toggleAfkMode(CommandContext context, ParsedArguments arguments, ToggleMode mode) {
        if (arguments.contains(CommandArguments.PLAYER)
                && arguments.getString(CommandArguments.PLAYER, "").equalsIgnoreCase(LIST_KEYWORD)) {
            return this.showList(context);
        }

        return this.runForOnlinePlayerOrSender(context, arguments, this.module, target -> {
            boolean state = mode.apply(this.module.isAfk(target));
            if (state) {
                this.module.enterAfk(target, false);
            } else {
                this.module.exitAfk(target, false);
            }

            if (context.getSender() != target) {
                this.module.sendPrefixed(AfkLang.AFK_TOGGLE_FEEDBACK, context.getSender(), builder -> builder
                        .with(CommonPlaceholders.PLAYER.resolver(target)));
            }

            return true;
        });
    }

    private boolean showList(CommandContext context) {
        List<Player> afkPlayers = this.module.getAfkPlayers();

        if (afkPlayers.isEmpty()) {
            this.module.sendPrefixed(AfkLang.AFK_LIST_EMPTY, context.getSender());
            return true;
        }

        String list = afkPlayers.stream()
                .map(player -> GREEN.wrap(player.getName()) + " "
                        + DARK_GRAY.wrap("(") + ORANGE.wrap(TimeFormats.formatSince(this.module.getAfkEnterTimestamp(player),
                                TimeFormatType.LITERAL)) + DARK_GRAY.wrap(")"))
                .collect(Collectors.joining(DARK_GRAY.wrap(", ")));

        PlaceholderContext contextBuilder = PlaceholderContext.builder()
                .with(SLPlaceholders.GENERIC_LIST, () -> list)
                .build();
        this.module.sendPrefixed(AfkLang.AFK_LIST, context.getSender(), contextBuilder);
        return true;
    }
}