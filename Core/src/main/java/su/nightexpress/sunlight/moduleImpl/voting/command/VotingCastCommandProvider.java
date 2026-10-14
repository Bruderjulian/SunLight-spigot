package su.nightexpress.sunlight.moduleImpl.voting.command;

import java.util.Collection;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.executors.CommandArguments;

import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.command.CommandArgumentConstants;
import su.nightexpress.sunlight.command.CommandProvider;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingLang;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingPerms;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteOption;

/**
 * The player-facing half of the module.
 * <p>
 * Every route here ends in {@link VotingModule#cast}, so none of them can vote around a rule that
 * the others obey. This class only decides which poll and which choice were meant.
 */
public class VotingCastCommandProvider extends CommandProvider<VotingModule> {

    private static final String ARGUMENT_POLL   = "poll";
    private static final String ARGUMENT_OPTION = "option";

    private static final String COMMAND_ROOT      = VotingCommands.ROOT_NAME;
    private static final String COMMAND_VOTE      = VotingCommands.VOTE_NAME;
    private static final String COMMAND_YES       = VotingCommands.YES_NAME;
    private static final String COMMAND_NO        = VotingCommands.NO_NAME;
    private static final String COMMAND_NEUTRAL   = VotingCommands.NEUTRAL_NAME;
    private static final String COMMAND_MY_VOTES  = VotingCommands.MY_VOTES_NAME;

    public VotingCastCommandProvider(final VotingModule module) {
        super(module, "voting-cast");
    }

    @Override
    public void setup() {
        this.register(COMMAND_VOTE, builder -> builder
                .withFullDescription(VotingLang.COMMAND_VOTE_DESC.text())
                .withPermission(VotingPerms.COMMAND_VOTE)
                .withRequirement(sender -> sender instanceof Player)
                .withOptionalArguments(this.voteArgument(), this.optionArgument())
                .executes(this::vote))
                .aliases("v")
                .under(COMMAND_ROOT);

        this.register(COMMAND_YES, builder -> builder
                .withFullDescription(VotingLang.COMMAND_YES_DESC.text())
                .withPermission(VotingPerms.COMMAND_VOTE)
                .withRequirement(sender -> sender instanceof Player)
                .executes((sender, arguments) -> {
                    return this.castBuiltIn(sender, VoteOption.YES);
                }))
                .under(COMMAND_ROOT);

        this.register(COMMAND_NO, builder -> builder
                .withFullDescription(VotingLang.COMMAND_NO_DESC.text())
                .withPermission(VotingPerms.COMMAND_VOTE)
                .withRequirement(sender -> sender instanceof Player)
                .executes((sender, arguments) -> {
                    return this.castBuiltIn(sender, VoteOption.NO);
                }))
                .under(COMMAND_ROOT);

        this.register(COMMAND_NEUTRAL, builder -> builder
                .withFullDescription(VotingLang.COMMAND_NEUTRAL_DESC.text())
                .withPermission(VotingPerms.COMMAND_VOTE)
                .withRequirement(sender -> sender instanceof Player)
                .executes((sender, arguments) -> {
                    return this.castBuiltIn(sender, VoteOption.NEUTRAL);
                }))
                .under(COMMAND_ROOT);

        this.register(COMMAND_MY_VOTES, builder -> builder
                .withFullDescription(VotingLang.COMMAND_MY_DESC.text())
                .withPermission(VotingPerms.COMMAND_MY)
                .withRequirement(sender -> sender instanceof Player)
                .executes((sender, arguments) -> this.module.openMyVotes((Player) sender) ? 1 : 0))
                .under(COMMAND_ROOT);

        this.registerRoot(COMMAND_ROOT, builder -> builder
                .withFullDescription(VotingLang.COMMAND_VOTING_DESC.text())
                .withPermission(VotingPerms.COMMAND_LIST)
                .withOptionalArguments(this.voteArgument())
                .executes(this::root));
    }

    private Argument<String> voteArgument() {
        return CommandArgumentConstants.string(ARGUMENT_POLL, info -> this.voteSuggestions());
    }

    private Argument<String> optionArgument() {
        // The suggestion callback only sees the option fragment being typed, not the poll typed
        // before it, so looking the poll up from it always misses. Suggest the union of every
        // open poll's choices instead: custom options show up, and the worst case is a name that
        // belongs to another poll, which cast() then reports properly.
        return CommandArgumentConstants.string(ARGUMENT_OPTION, info -> this.optionSuggestions());
    }

    private Collection<String> voteSuggestions() {
        return this.module.getOpenVotes().stream().map(Vote::getKey).toList();
    }

    private Collection<String> optionSuggestions() {
        final java.util.Set<String> options = new java.util.LinkedHashSet<>(
                List.of(VoteOption.YES, VoteOption.NO, VoteOption.NEUTRAL));
        this.module.getOpenVotes().forEach(vote -> options.addAll(vote.getOptionKeys()));
        return options;
    }

    /**
     * Bare {@code /voting}: the poll list for a player, and a plain listing for the console, which
     * cannot open a menu.
     */
    private int root(final CommandSender sender, final CommandArguments arguments) {
        final Object pollObj = arguments.getOrDefaultUnchecked(ARGUMENT_POLL, null);
        if (pollObj instanceof final String key && !key.isBlank()) {
            return this.view(sender, key);
        }

        if (sender instanceof final Player player && this.module.settings().commandGui.get()) {
            return this.module.openMenu(player) ? 1 : 0;
        }

        final List<Vote> open = this.module.getOpenVotes();
        if (open.isEmpty()) {
            this.module.sendPrefixed(VotingLang.ERROR_NO_VOTES, sender);
            return 0;
        }

        open.forEach(vote -> sender.sendMessage("<gray>• <white>" + vote.getDisplay()
                + " <dark_gray>(" + vote.getKey() + ")"));
        return 1;
    }

    private int view(final CommandSender sender, final String key) {
        final Vote vote = this.module.findVote(key);
        if (vote == null) {
            this.module.sendPrefixed(VotingLang.ERROR_UNKNOWN_VOTE, sender,
                    b -> b.with(SLPlaceholders.GENERIC_TARGET, () -> key));
            return 0;
        }

        if (sender instanceof final Player player && this.module.settings().commandGui.get()) {
            return this.module.openView(player, vote) ? 1 : 0;
        }

        this.module.showInfo(sender, vote);
        return 1;
    }

    /**
     * {@code /vote <poll> <choice>}, or {@code /vote <choice>} when only one poll is open.
     * <p>
     * The short form never guesses between several polls. Casting a vote in the wrong poll is
     * worse than asking which one was meant, and the poll cannot be inferred from the choice
     * because several polls usually offer yes.
     */
    private int vote(final CommandSender sender, final CommandArguments arguments) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final Object pollObj = arguments.getOrDefaultUnchecked(ARGUMENT_POLL, null);
        final Object optionObj = arguments.getOrDefaultUnchecked(ARGUMENT_OPTION, null);

        if (!(pollObj instanceof final String pollKey) || pollKey.isBlank()) {
            this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
            return 0;
        }

        if (optionObj instanceof final String optionKey && !optionKey.isBlank()) {
            final Vote vote = this.module.findVote(pollKey);
            if (vote == null) {
                this.module.sendPrefixed(VotingLang.ERROR_UNKNOWN_VOTE, sender,
                        b -> b.with(SLPlaceholders.GENERIC_TARGET, () -> pollKey));
                return 0;
            }
            return this.module.cast(player, vote, optionKey) ? 1 : 0;
        }

        // Single snapshot for both the decision and the error message: the open set can change
        // between two separate reads and the message would then describe polls that were not the
        // ones just decided on.
        final List<Vote> open = this.module.getOpenVotes();
        if (open.size() != 1) {
            if (open.isEmpty()) {
                this.module.sendPrefixed(VotingLang.ERROR_NO_VOTES, sender);
            } else {
                this.module.sendPrefixed(VotingLang.ERROR_AMBIGUOUS, sender,
                        b -> b.with(SLPlaceholders.GENERIC_LIST,
                                () -> String.join(", ", open.stream().map(Vote::getKey).toList())));
            }
            return 0;
        }
        final Vote sole = open.getFirst();
        if (sole.getKey().equalsIgnoreCase(pollKey) && this.module.findVote(pollKey) != null) {
            // "/vote event" where event is the sole poll's own key and the choice was forgotten:
            // that is a usage mistake, not an unknown choice called "event".
            this.module.sendPrefixed(CoreLang.COMMAND_SYNTAX_GENERIC_ERROR, sender);
            return 0;
        }
        return this.module.cast(player, sole, pollKey) ? 1 : 0;
    }

    /**
     * {@code /yes}, {@code /no} and {@code /neutral}. These need exactly one open poll, because
     * there is no way to say which poll they mean.
     */
    private int castBuiltIn(final CommandSender sender, final String optionKey) {
        if (!(sender instanceof final Player player)) {
            this.module.sendPrefixed(CoreLang.COMMAND_EXECUTION_PLAYER_ONLY, sender);
            return 0;
        }

        final List<Vote> open = this.module.getOpenVotes();
        if (open.size() != 1) {
            if (open.isEmpty()) {
                this.module.sendPrefixed(VotingLang.ERROR_NO_VOTES, sender);
            } else {
                this.module.sendPrefixed(VotingLang.ERROR_AMBIGUOUS, sender,
                        b -> b.with(SLPlaceholders.GENERIC_LIST,
                                () -> String.join(", ", open.stream().map(Vote::getKey).toList())));
            }
            return 0;
        }
        final Vote sole = open.getFirst();

        if (!sole.hasOption(optionKey)) {
            // Not an unknown choice so much as a poll that never had that one: the poll's owner
            // switched the built-ins off, and the reason is worth saying.
            this.module.sendPrefixed(VoteOption.isBuiltInKey(optionKey) ? VotingLang.ERROR_BUILTIN_DISABLED
                    : VotingLang.ERROR_UNKNOWN_OPTION, sender, b -> b
                    .with(sole.placeholders())
                    .with(SLPlaceholders.GENERIC_TARGET, () -> optionKey)
                    .with(SLPlaceholders.GENERIC_LIST, () -> String.join(", ", sole.getOptionKeys())));
            return 0;
        }

        return this.module.cast(player, sole, optionKey) ? 1 : 0;
    }
}