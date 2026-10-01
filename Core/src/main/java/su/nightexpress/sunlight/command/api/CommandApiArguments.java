package su.nightexpress.sunlight.command.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.utils.Utils;
import su.nightexpress.sunlight.module.Module;

public final class CommandApiArguments {

    public static final String PLAYER = "player";
    public static final String NAME = "name";
    public static final String ACTION = "action";
    public static final String DATA = "data";
    public static final String PRESET = "preset";
    public static final String TAIL = "tail";

    private static final Set<String> SILENT_TOKENS = Set.of("-s", "--silent");

    private CommandApiArguments() {
    }

    public static Argument<String> string(final String name) {
        return new StringArgument(name);
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

    public static Argument<String> targetArgument() {
        return CommandApiArguments.greedy("target", info -> CommandApiArguments.onlinePlayerNames());
    }

    public static Target target(final CommandArguments arguments) {
        final Object rawObj = arguments.get("target");
        if (rawObj == null || !(rawObj instanceof final String raw) || raw == null || raw.isBlank()) {
            return new Target(null, false);
        }

        String playerName = null;
        boolean silent = false;
        for (final String token : raw.trim().split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (SILENT_TOKENS.contains(Utils.lowercase(token))) {
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

    /**
     * The optional target/silent part of a command, as typed after the required
     * arguments.
     *
     * @param playerName The target player name, or null to target the sender.
     * @param silent     Whether output to the target should be suppressed.
     */
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
                    if (targetUser == null || !(!(sender instanceof Player player) || player.canSee(targetUser))) {
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
