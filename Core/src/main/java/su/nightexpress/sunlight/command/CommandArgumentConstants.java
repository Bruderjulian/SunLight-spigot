package su.nightexpress.sunlight.command;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.Utils;
import su.nightexpress.sunlight.module.Module;
import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;

public final class CommandArgumentConstants {

    public static final String PLAYER = "player";
    public static final String NAME = "name";
    public static final String ACTION = "action";
    public static final String DATA = "data";
    public static final String PRESET = "preset";
    public static final String AMOUNT = "amount";
    public static final String TIME = "time";
    public static final String VALUE = "value";
    public static final String WORLD = "world";
    public static final String TYPE = "type";
    public static final String MODE = "mode";
    public static final String STATE = "state";
    public static final String ADDRESS = "address";
    public static final String ITEM = "item";
    public static final String RADIUS = "radius";
    public static final String LEVEL = "level";
    public static final String ENCHANT = "enchant";
    public static final String TEXT = "text";
    public static final String INDEX = "index";

    public static final String TARGET = "target";
    public static final String DESTINATION = "destination";
    public static final String INET_ADDRESS = "address";
    public static final String POSITION = "position";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String Z = "z";

    public static final String FLAG_SILENT = "-s";
    public static final String FLAG_SILENT_LONG = "--silent";
    public static final String FLAG_FORCE = "-f";

    private CommandArgumentConstants() {
    }

    public static Argument<String> string(final String name,
            final Function<SuggestionInfo<CommandSender>, Collection<String>> suggestions) {
        return new StringArgument(name).replaceSuggestions(ArgumentSuggestions.stringCollection(suggestions));
    }

    public static Argument<String> greedy(final String name,
            final Function<SuggestionInfo<CommandSender>, Collection<String>> suggestions) {
        return new GreedyStringArgument(name).replaceSuggestions(ArgumentSuggestions.stringCollection(suggestions));
    }

    public static List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
    }

    public static Argument<PlaytimePeriod> periodArgument() {
        return new CustomArgument<PlaytimePeriod, String>(new StringArgument("period"), info -> {
            try {
                return PlaytimePeriod.valueOf(info.currentInput().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                CoreLang.COMMAND_SYNTAX_GENERIC_ERROR.value().sendWith(info.sender(), replacer -> replacer
                        .with("argument", () -> info.currentInput()));
                return null;
            }
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> Utils.getEnumNamesArray(PlaytimePeriod.class)));
    }

    /**
     * The optional {@code [player] [-s]} part of a command, as typed after the
     * required
     * arguments.
     */
    public static Argument<String> targetArgument() {
        return targetArgument(info -> onlinePlayerNames());
    }

    public static Argument<String> targetArgument(
            final Function<SuggestionInfo<CommandSender>, Collection<String>> suggestions) {
        return greedy("target", suggestions);
    }

    public static Target target(final CommandArguments arguments) {
        final Object rawObj = arguments.get("target");
        if (rawObj == null || !(rawObj instanceof final String raw) || raw.isBlank()) {
            return new Target(null, false);
        }

        String playerName = null;
        boolean silent = false;
        for (final String token : raw.trim().split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            final String lower = Utils.lowercase(token);
            if (lower.equals(FLAG_SILENT) || lower.equals(FLAG_SILENT_LONG)) {
                silent = true;
                continue;
            }
            if (playerName != null) {
                return null; // Ambiguous: two targets.
            }
            playerName = token;
        }

        return new Target(playerName, silent);
    }

    public record Target(String playerName, boolean silent) {

        public boolean hasTarget() {
            return this.playerName != null;
        }

        /**
         * Resolves the target of a command and runs the given consumer on the main
         * thread with the target's loaded data.
         * <p>
         * Mirrors the legacy {@code CommandProvider} behaviour: no target and a
         * non-player sender is a usage error, a target other than the sender requires
         * {@code othersPermission}, vanished players are hidden, and offline targets
         * are saved again after the change.
         *
         * @return false when the command could not be dispatched.
         */
        public int runAs(final Module module,
                final CommandSender sender, final String othersPermission,
                final BiConsumer<SunUser, Player> consumer) {

            String playername2 = playerName();
            if (playername2 == null) {
                if (!(sender instanceof Player)) {
                    module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
                    return 0;
                }
                playername2 = sender.getName();
            } else if (!sender.hasPermission(othersPermission)) {
                module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                return 0;
            }

            module.userManager().loadByNameAsync(playername2).thenCompose(userOptional -> {
                final SunUser user = userOptional.orElse(null);
                if (user == null) {
                    module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                    return CompletableFuture.completedFuture(null);
                }

                return module.userManager().loadTargetPlayer(user).thenComposeAsync(targetUser -> {
                    if (targetUser == null
                            || !(!(sender instanceof final Player player) || player.canSee(targetUser))) {
                        module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, sender);
                        return CompletableFuture.completedFuture(null);
                    }

                    consumer.accept(user, targetUser);

                    return targetUser.isOnline() ? CompletableFuture.completedFuture(null)
                            : CompletableFuture.runAsync(targetUser::saveData);
                }, (executor) -> module.plugin().runTask(executor));
            }).whenComplete(Utils::printStacktrace);

            return 1;
        }
    }
}
