package su.nightexpress.sunlight.moduleImpl.voting.config;

import org.bukkit.Sound;

import su.nightexpress.nightcore.locale.LangContainer;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.BooleanLocale;
import su.nightexpress.nightcore.locale.entry.EnumLocale;
import su.nightexpress.nightcore.locale.entry.MessageLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.locale.message.MessageData;
import su.nightexpress.sunlight.moduleImpl.voting.command.VotingCommands;
import su.nightexpress.sunlight.moduleImpl.voting.model.GateType;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteStatus;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;
import static su.nightexpress.sunlight.SLPlaceholders.*;
import static su.nightexpress.sunlight.moduleImpl.voting.VotingPlaceholders.*;

public class VotingLang implements LangContainer {

    // -- Enumerated values ----------------------------------------------------------------------

    public static final EnumLocale<VoteStatus> STATUS = LangEntry.builder("Voting.Status")
            .enumeration(VoteStatus.class);

    public static final EnumLocale<GateType> GATE = LangEntry.builder("Voting.Gate")
            .enumeration(GateType.class);

    public static final BooleanLocale STATE_YES_NO = LangEntry.builder("Voting.State.YesNo")
            .bool(GREEN.wrap("yes"), RED.wrap("no"));

    public static final TextLocale TEXT_UNLIMITED = LangEntry.builder("Voting.Text.Unlimited")
            .text(GRAY.wrap("unlimited"));

    public static final TextLocale TEXT_FREE = LangEntry.builder("Voting.Text.Free")
            .text(GREEN.wrap("free"));

    public static final TextLocale TEXT_NO_LEADER = LangEntry.builder("Voting.Text.NoLeader")
            .text(GRAY.wrap("nothing yet"));

    public static final TextLocale MODE_OPEN = LangEntry.builder("Voting.Mode.Open")
            .text(ORANGE.wrap("Open Votes"));

    public static final TextLocale MODE_MINE = LangEntry.builder("Voting.Mode.Mine")
            .text(ORANGE.wrap("My Votes"));

    public static final TextLocale MODE_HISTORY = LangEntry.builder("Voting.Mode.History")
            .text(ORANGE.wrap("Finished Votes"));

    // -- Command descriptions -------------------------------------------------------------------

    public static final TextLocale COMMAND_VOTING_DESC = LangEntry.builder("Voting.Command.Root.Desc")
            .text("Browse and vote in the server's open votes.");

    public static final TextLocale COMMAND_LIST_DESC = LangEntry.builder("Voting.Command.List.Desc")
            .text("Open the list of votes you can take part in.");

    public static final TextLocale COMMAND_MY_DESC = LangEntry.builder("Voting.Command.MyVotes.Desc")
            .text("Open the votes you have already voted in.");

    public static final TextLocale COMMAND_VOTE_DESC = LangEntry.builder("Voting.Command.Vote.Desc")
            .text("Vote in a poll. Voting again on the same choice takes it back.");

    public static final TextLocale COMMAND_YES_DESC = LangEntry.builder("Voting.Command.Yes.Desc")
            .text("Vote 'yes' in the open poll.");

    public static final TextLocale COMMAND_NO_DESC = LangEntry.builder("Voting.Command.No.Desc")
            .text("Vote 'no' in the open poll.");

    public static final TextLocale COMMAND_NEUTRAL_DESC = LangEntry.builder("Voting.Command.Neutral.Desc")
            .text("Vote 'neutral' in the open poll.");

    public static final TextLocale COMMAND_CREATE_DESC = LangEntry.builder("Voting.Command.Create.Desc")
            .text("Open a new vote.");

    public static final TextLocale COMMAND_OPTION_ADD_DESC = LangEntry.builder("Voting.Command.Option.Add.Desc")
            .text("Add a choice to a vote.");

    public static final TextLocale COMMAND_OPTION_REMOVE_DESC = LangEntry.builder("Voting.Command.Option.Remove.Desc")
            .text("Remove a choice from a vote.");

    public static final TextLocale COMMAND_OPTION_LIST_DESC = LangEntry.builder("Voting.Command.Option.List.Desc")
            .text("List the choices of a vote.");

    public static final TextLocale COMMAND_OPTION_GATE_DESC = LangEntry.builder("Voting.Command.Option.Gate.Desc")
            .text("Configure who may pick a choice.");

    public static final TextLocale COMMAND_RULES_DESC = LangEntry.builder("Voting.Command.Rules.Desc")
            .text("Change how a vote counts and decides its winner.");

    public static final TextLocale COMMAND_INFO_DESC = LangEntry.builder("Voting.Command.Info.Desc")
            .text("Show the full result of a vote.");

    public static final TextLocale COMMAND_CLOSE_DESC = LangEntry.builder("Voting.Command.Close.Desc")
            .text("End a vote early and announce its result.");

    public static final TextLocale COMMAND_CLONE_DESC = LangEntry.builder("Voting.Command.Clone.Desc")
            .text("Copy a vote, its choices and its rules under a new key.");

    public static final TextLocale COMMAND_RENAME_DESC = LangEntry.builder("Voting.Command.Rename.Desc")
            .text("Change the display name of a vote.");

    public static final TextLocale COMMAND_DESCRIBE_DESC = LangEntry.builder("Voting.Command.Describe.Desc")
            .text("Change the description of a vote.");

    public static final TextLocale COMMAND_HISTORY_DESC = LangEntry.builder("Voting.Command.History.Desc")
            .text("Browse finished votes.");

    public static final TextLocale COMMAND_START_DESC = LangEntry.builder("Voting.Command.Start.Desc")
            .text("Open a scheduled vote early.");

    public static final TextLocale COMMAND_CANCEL_DESC = LangEntry.builder("Voting.Command.Cancel.Desc")
            .text("Kill a vote without producing a result.");

    public static final TextLocale COMMAND_DELETE_DESC = LangEntry.builder("Voting.Command.Delete.Desc")
            .text("Delete a vote and its votes permanently.");

    public static final TextLocale COMMAND_EXTEND_DESC = LangEntry.builder("Voting.Command.Extend.Desc")
            .text("Push the end of a vote further out.");

    public static final TextLocale COMMAND_ALL_DESC = LangEntry.builder("Voting.Command.All.Desc")
            .text("List every vote, including finished ones.");

    public static final TextLocale COMMAND_BROADCAST_DESC = LangEntry.builder("Voting.Command.Broadcast.Desc")
            .text("Re-announce an open vote to everybody.");

    public static final TextLocale ARGUMENT_POLL = LangEntry.builder("Voting.Argument.Poll")
            .text("vote");

    public static final TextLocale ARGUMENT_OPTION = LangEntry.builder("Voting.Argument.Option")
            .text("choice");

    public static final TextLocale ARGUMENT_DURATION = LangEntry.builder("Voting.Argument.Duration")
            .text("duration");

    // -- Casting --------------------------------------------------------------------------------

    public static final MessageLocale VOTE_CAST = LangEntry.builder("Voting.Cast.Done")
            .chatMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("You voted " + ORANGE.wrap(OPTION_DISPLAY) + " in " + ORANGE.wrap(VOTE_NAME) + "."),
                    DARK_GRAY.wrap("Weight: " + GRAY.wrap(SCORE)));

    public static final MessageLocale VOTE_WITHDRAWN = LangEntry.builder("Voting.Cast.Withdrawn")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_BASS,
                    GRAY.wrap("You took back your vote for " + ORANGE.wrap(OPTION_DISPLAY) + "."));

    public static final MessageLocale VOTE_CHANGED = LangEntry.builder("Voting.Cast.Changed")
            .chatMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("You changed your vote to " + ORANGE.wrap(OPTION_DISPLAY) + "."),
                    DARK_GRAY.wrap("Weight: " + GRAY.wrap(SCORE)));

    public static final MessageLocale VOTE_REFUNDED = LangEntry.builder("Voting.Cast.Refunded")
            .chatMessage(GREEN.wrap("Refunded " + ORANGE.wrap(GENERIC_AMOUNT) + " for taking back your vote."));

    /**
     * The announcement shown when a vote opens. Mirrors PTP's request message: clickable commands
     * rather than a wall of text, because the player has to know how to answer before they can.
     */
    public static final MessageLocale ANNOUNCEMENT = LangEntry.builder("Voting.Announcement")
            .message(MessageData.chat().usePrefix(false).sound(Sound.BLOCK_NOTE_BLOCK_PLING).build(),
                    " ",
                    ORANGE.wrap(BOLD.wrap("Vote Open:")),
                    " ",
                    WHITE.wrap(VOTE_NAME),
                    GRAY.wrap(" by " + ORANGE.wrap(VOTE_CREATOR)),
                    " ",
                    GRAY.wrap("Closes in " + ORANGE.wrap(VOTE_TIME_LEFT)),
                    " ",
                    ORANGE.wrap(" ".repeat(12)
                            + SHOW_TEXT.with(GRAY.wrap("Open the vote")).wrap(
                                    RUN_COMMAND.with("/" + VotingCommands.VOTE_NAME + " " + VOTE_KEY)
                                            .wrap(ORANGE.wrap(BOLD.wrap("[Open]"))))
                            + " ".repeat(4)
                            + SHOW_TEXT.with(GRAY.wrap("Vote yes")).wrap(
                                    RUN_COMMAND.with("/" + VotingCommands.YES_NAME).wrap(GREEN.wrap(BOLD.wrap("[Yes]"))))
                            + " ".repeat(4)
                            + SHOW_TEXT.with(GRAY.wrap("Vote no")).wrap(
                                    RUN_COMMAND.with("/" + VotingCommands.NO_NAME).wrap(RED.wrap(BOLD.wrap("[No]"))))
                    + " ".repeat(6)
                    + SHOW_TEXT.with(GRAY.wrap("Vote neutral")).wrap(
                            RUN_COMMAND.with("/" + VotingCommands.NEUTRAL_NAME).wrap(GRAY.wrap(BOLD.wrap("[Neutral]"))))
                    ),
                    " ");

    // -- Results --------------------------------------------------------------------------------

    public static final MessageLocale RESULT_PASSED = LangEntry.builder("Voting.Result.Passed")
            .chatMessage(Sound.UI_TOAST_CHALLENGE_COMPLETE,
                    GRAY.wrap(VOTE_NAME + ": " + ORANGE.wrap(OPTION_DISPLAY) + WON + "."),
                    DARK_GRAY.wrap("Share: " + GRAY.wrap(PERCENT) + DARK_GRAY.wrap(" of " + GRAY.wrap(VOTE_TOTAL_SCORE))));

    public static final MessageLocale RESULT_FAILED = LangEntry.builder("Voting.Result.Failed")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_BASS,
                    GRAY.wrap(VOTE_NAME + ": no choice reached " + ORANGE.wrap(VOTE_PASS_PCT) + "%."));

    public static final MessageLocale RESULT_CANCELLED = LangEntry.builder("Voting.Result.Cancelled")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_BASS,
                    GRAY.wrap(VOTE_NAME + " was cancelled."));

    public static final MessageLocale RESULT_QUORUM = LangEntry.builder("Voting.Result.Quorum")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_BASS,
                    GRAY.wrap(VOTE_NAME + " was cancelled: not enough people voted."));

    public static final MessageLocale RESULT_TIED = LangEntry.builder("Voting.Result.Tied")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_BASS,
                    GRAY.wrap(VOTE_NAME + " ended in a tie: " + ORANGE.wrap(GENERIC_LIST) + "."));

    public static final MessageLocale RESULT_RUNOFF = LangEntry.builder("Voting.Result.Runoff")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_PLING,
                    GRAY.wrap(VOTE_NAME + " produced no winner, so a runoff is open: "
                            + ORANGE.wrap(GENERIC_TARGET) + "."));

    public static final MessageLocale REMINDER = LangEntry.builder("Voting.Reminder")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_PLING,
                    GRAY.wrap("Vote open: " + ORANGE.wrap(VOTE_NAME)
                            + GRAY.wrap(" closes in " + ORANGE.wrap(VOTE_TIME_LEFT) + ".")));

    public static final MessageLocale JOIN_NUDGE = LangEntry.builder("Voting.JoinNudge")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_PLING,
                    GRAY.wrap("Open votes waiting for you: " + ORANGE.wrap(GENERIC_LIST) + "."));

    public static final TextLocale SECRET_HIDDEN = LangEntry.builder("Voting.Text.SecretHidden")
            .text(GRAY.wrap("hidden"));

    public static final TextLocale INFO_HEADER = LangEntry.builder("Voting.Info.Header")
            .text(BOLD.wrap(VOTE_NAME)
                    + GRAY.wrap(" by " + ORANGE.wrap(VOTE_CREATOR)
                    + DARK_GRAY.wrap(" · ")
                    + GRAY.wrap("closes in " + ORANGE.wrap(VOTE_TIME_LEFT)
                    + DARK_GRAY.wrap(" · ")
                    + GRAY.wrap(VOTE_PARTICIPANTS + " voted")
                    + DARK_GRAY.wrap(" · needs " + ORANGE.wrap(VOTE_PASS_PCT) + "% to pass"))));

    public static final TextLocale RESULT_LINE = LangEntry.builder("Voting.Result.Line")
            .text(DARK_GRAY.wrap("• ") + WHITE.wrap(OPTION_DISPLAY) + DARK_GRAY.wrap(" — ")
                    + GRAY.wrap(COUNT) + DARK_GRAY.wrap(" votes, ") + GRAY.wrap(PERCENT) + DARK_GRAY.wrap(" (")
                    + GRAY.wrap(SCORE) + DARK_GRAY.wrap(" weight)"));

    // -- Errors ---------------------------------------------------------------------------------

    public static final MessageLocale ERROR_DATA_NOT_LOADED = LangEntry.builder("Voting.Error.DataNotLoaded")
            .chatMessage(Sound.ENTITY_VILLAGER_NO, GRAY.wrap("Voting data is still loading, try again in a moment."));

    public static final MessageLocale ERROR_NO_VOTES = LangEntry.builder("Voting.Error.NoVotes")
            .chatMessage(Sound.ENTITY_VILLAGER_NO, GRAY.wrap("There are no open votes right now."));

    public static final MessageLocale ERROR_UNKNOWN_VOTE = LangEntry.builder("Voting.Error.UnknownVote")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("There is no vote called " + ORANGE.wrap(GENERIC_TARGET) + "."));

    public static final MessageLocale ERROR_UNKNOWN_OPTION = LangEntry.builder("Voting.Error.UnknownOption")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(VOTE_NAME + " has no choice called " + ORANGE.wrap(GENERIC_TARGET) + "."),
                    DARK_GRAY.wrap("Choices: " + GRAY.wrap(GENERIC_LIST)));

    public static final MessageLocale ERROR_AMBIGUOUS = LangEntry.builder("Voting.Error.Ambiguous")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("There is more than one open vote. Name the one you mean."),
                    DARK_GRAY.wrap("Open votes: " + GRAY.wrap(GENERIC_LIST)));

    public static final MessageLocale ERROR_NOT_OPEN = LangEntry.builder("Voting.Error.NotOpen")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(VOTE_NAME + " is not open. It is " + ORANGE.wrap(VOTE_STATUS) + "."));

    public static final MessageLocale ERROR_NOT_STARTED = LangEntry.builder("Voting.Error.NotStarted")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(VOTE_NAME + " opens in " + ORANGE.wrap(VOTE_TIME_TO_START) + "."));

    public static final MessageLocale ERROR_NO_CHANGE = LangEntry.builder("Voting.Error.NoChange")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("Your vote in " + ORANGE.wrap(VOTE_NAME) + " is final."));

    public static final MessageLocale ERROR_CHANGE_LOCKED = LangEntry.builder("Voting.Error.ChangeLocked")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("Vote changes in " + ORANGE.wrap(VOTE_NAME) + " are locked."));

    public static final MessageLocale ERROR_BUILTIN_DISABLED = LangEntry.builder("Voting.Error.BuiltInDisabled")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(VOTE_NAME + " does not have a " + ORANGE.wrap(GENERIC_TARGET) + " choice."));

    public static final MessageLocale ERROR_DUPLICATE_KEY = LangEntry.builder("Voting.Error.DuplicateKey")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("A vote called " + ORANGE.wrap(GENERIC_TARGET) + " already exists."));

    public static final MessageLocale ERROR_TOO_MANY_VOTES = LangEntry.builder("Voting.Error.TooManyVotes")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("You already hold " + ORANGE.wrap(VOTE_MAX_VOTES) + " votes in " + ORANGE.wrap(VOTE_NAME) + "."));

    public static final MessageLocale ERROR_TOO_MANY_OPTIONS = LangEntry.builder("Voting.Error.TooManyOptions")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(VOTE_NAME + " already has " + ORANGE.wrap(GENERIC_MAX) + " choices."));

    public static final MessageLocale ERROR_OPTION_EXISTS = LangEntry.builder("Voting.Error.OptionExists")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(VOTE_NAME + " already has a choice called " + ORANGE.wrap(GENERIC_TARGET) + "."));

    public static final MessageLocale ERROR_OPTION_IN_USE = LangEntry.builder("Voting.Error.OptionInUse")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("People have already voted for " + ORANGE.wrap(GENERIC_TARGET) + "."),
                    DARK_GRAY.wrap("Use " + ORANGE.wrap("/" + VotingCommands.REMOVE_NAME) + " <vote> <choice> -f")
                            + " to move their votes to neutral.");

    public static final MessageLocale ERROR_OPTION_BUILTIN = LangEntry.builder("Voting.Error.OptionBuiltIn")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(ORANGE.wrap(GENERIC_TARGET) + " is one of the built-in choices and cannot be removed."));

    public static final MessageLocale ERROR_GATE_UNAVAILABLE = LangEntry.builder("Voting.Error.GateUnavailable")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(ORANGE.wrap(GENERIC_TARGET) + " cannot be used as a requirement on this server."));

    public static final MessageLocale ERROR_LAST_OPTION = LangEntry.builder("Voting.Error.LastOption")
            .chatMessage(Sound.ENTITY_VILLAGER_NO, GRAY.wrap("A vote needs at least two choices."));

    // Requirements.

    // Exempt players are never refused: they skip the requirements rather than losing access, so
    // there is no "you cannot vote" message for that case.

    public static final MessageLocale ERROR_WORLD = LangEntry.builder("Voting.Error.World")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("You cannot vote for this from " + ORANGE.wrap(GENERIC_WORLD) + "."));

    public static final MessageLocale ERROR_PLAYTIME = LangEntry.builder("Voting.Error.Playtime")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("You need " + ORANGE.wrap(GENERIC_MIN) + " hours played before your vote counts."));

    public static final MessageLocale ERROR_PARTICIPATION = LangEntry.builder("Voting.Error.Participation")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("You are already taking part in " + ORANGE.wrap(GENERIC_MAX) + " open votes."));

    public static final MessageLocale ERROR_NO_CURRENCY = LangEntry.builder("Voting.Error.NoCurrency")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("This vote needs an economy, and the server has none."));

    public static final MessageLocale ERROR_MIN_BALANCE = LangEntry.builder("Voting.Error.MinBalance")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("This vote is only open to players with " + ORANGE.wrap(GENERIC_MIN) + "."));

    public static final MessageLocale ERROR_MAX_BALANCE = LangEntry.builder("Voting.Error.MaxBalance")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("This vote is not open to players with " + ORANGE.wrap(GENERIC_MAX) + " or more."));

    public static final MessageLocale ERROR_COST_FUNDS = LangEntry.builder("Voting.Error.CostFunds")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("This choice costs " + ORANGE.wrap(GENERIC_AMOUNT) + ".")
                            + "\n" + DARK_GRAY.wrap("You only have " + ORANGE.wrap(GENERIC_TOTAL) + "."));

    // Option gates.

    public static final MessageLocale ERROR_GATE_PERMISSION = LangEntry.builder("Voting.Error.Gate.Permission")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("You do not have access to " + ORANGE.wrap(OPTION_DISPLAY) + "."));

    public static final MessageLocale ERROR_GATE_RANK = LangEntry.builder("Voting.Error.Gate.Rank")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(OPTION_DISPLAY + " is reserved for " + ORANGE.wrap(GENERIC_TARGET) + " ranks."));

    public static final MessageLocale ERROR_GATE_WORLD = LangEntry.builder("Voting.Error.Gate.World")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("You cannot pick " + ORANGE.wrap(OPTION_DISPLAY) + " from " + ORANGE.wrap(GENERIC_WORLD) + "."));

    public static final MessageLocale ERROR_GATE_PLAYTIME = LangEntry.builder("Voting.Error.Gate.Playtime")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap(OPTION_DISPLAY + " needs " + ORANGE.wrap(GENERIC_MIN) + " hours played."));

    // Limits.

    public static final MessageLocale ERROR_LIMIT_GLOBAL = LangEntry.builder("Voting.Error.Limit.Global")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("The server already has " + ORANGE.wrap(GENERIC_MAX) + " votes open."));

    public static final MessageLocale ERROR_LIMIT_CREATOR = LangEntry.builder("Voting.Error.Limit.Creator")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("You already have " + ORANGE.wrap(GENERIC_MAX) + " votes open."));

    public static final MessageLocale ERROR_BAD_DURATION = LangEntry.builder("Voting.Error.BadDuration")
            .chatMessage(Sound.ENTITY_VILLAGER_NO,
                    GRAY.wrap("'" + ORANGE.wrap(GENERIC_TEXT) + "' is not a duration."),
                    DARK_GRAY.wrap("Use seconds, or a value like " + GRAY.wrap("30s") + ", " + GRAY.wrap("5m")
                            + " or " + GRAY.wrap("1h") + "."));

    // -- Staff feedback --------------------------------------------------------------------------

    public static final MessageLocale CREATED = LangEntry.builder("Voting.Created")
            .chatMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("Opened " + ORANGE.wrap(VOTE_NAME) + " until " + ORANGE.wrap(VOTE_TIME_END) + "."));

    public static final MessageLocale CLOSED = LangEntry.builder("Voting.Closed")
            .chatMessage(Sound.UI_TOAST_CHALLENGE_COMPLETE, GRAY.wrap("Closed " + ORANGE.wrap(VOTE_NAME) + "."));

    public static final MessageLocale CANCELLED = LangEntry.builder("Voting.Cancelled")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_BASS, GRAY.wrap("Cancelled " + ORANGE.wrap(VOTE_NAME) + "."));

    public static final MessageLocale DELETED = LangEntry.builder("Voting.Deleted")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_BASS,
                    GRAY.wrap("Deleted " + ORANGE.wrap(VOTE_NAME) + " and everything in it."));

    public static final MessageLocale EXTENDED = LangEntry.builder("Voting.Extended")
            .chatMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap(VOTE_NAME + " now closes at " + ORANGE.wrap(VOTE_TIME_END) + "."));

    public static final MessageLocale OPTION_ADDED = LangEntry.builder("Voting.Option.Added")
            .chatMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("Added " + ORANGE.wrap(OPTION_DISPLAY) + " to " + ORANGE.wrap(VOTE_NAME) + "."));

    public static final MessageLocale OPTION_REMOVED = LangEntry.builder("Voting.Option.Removed")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_BASS,
                    GRAY.wrap("Removed " + ORANGE.wrap(OPTION_DISPLAY) + " from " + ORANGE.wrap(VOTE_NAME) + "."));

    public static final MessageLocale OPTION_MOVED = LangEntry.builder("Voting.Option.Moved")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_BASS,
                    GRAY.wrap("Moved the votes on " + ORANGE.wrap(GENERIC_TARGET) + " to neutral."));

    public static final MessageLocale GATE_SET = LangEntry.builder("Voting.Option.Gate.Set")
            .chatMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap(OPTION_DISPLAY + " now needs: " + ORANGE.wrap(GENERIC_LIST) + "."));

    public static final MessageLocale RULES_SET = LangEntry.builder("Voting.Rules.Set")
            .chatMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("Updated the rules of " + ORANGE.wrap(VOTE_NAME) + "."));

    public static final MessageLocale CLONED = LangEntry.builder("Voting.Cloned")
            .chatMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("Copied " + ORANGE.wrap(VOTE_NAME) + " to " + ORANGE.wrap(GENERIC_TARGET) + "."));

    public static final MessageLocale RENAMED = LangEntry.builder("Voting.Renamed")
            .chatMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("Renamed " + ORANGE.wrap(VOTE_KEY) + " to " + ORANGE.wrap(GENERIC_TARGET) + "."));

    public static final MessageLocale DESCRIBED = LangEntry.builder("Voting.Described")
            .chatMessage(Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    GRAY.wrap("Updated the description of " + ORANGE.wrap(VOTE_NAME) + "."));

    public static final MessageLocale STARTED = LangEntry.builder("Voting.Started")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_PLING,
                    GRAY.wrap("Opened " + ORANGE.wrap(VOTE_NAME) + " early."));

    public static final MessageLocale BROADCAST = LangEntry.builder("Voting.Broadcast")
            .chatMessage(Sound.BLOCK_NOTE_BLOCK_PLING, GRAY.wrap("Re-announced " + ORANGE.wrap(VOTE_NAME) + "."));

    // -- Menus -----------------------------------------------------------------------------------

    public static final TextLocale MENU_LIST_TITLE = LangEntry.builder("Voting.Menu.List.Title")
            .text("[Voting] " + GENERIC_MODE);

    public static final TextLocale MENU_VIEW_TITLE = LangEntry.builder("Voting.Menu.View.Title")
            .text("Vote: " + VOTE_NAME);

    public static final TextLocale MENU_EMPTY = LangEntry.builder("Voting.Menu.Empty")
            .text(DARK_GRAY.wrap("Nothing to show here."));

    public static final TextLocale BUTTON_BACK = LangEntry.builder("Voting.Menu.Button.Back")
            .text(GREEN.wrap("Back"));

    public static final TextLocale BUTTON_FILTER = LangEntry.builder("Voting.Menu.Button.Filter")
            .text(GOLD.wrap("Filter"));

    public static final TextLocale BUTTON_CLOSE = LangEntry.builder("Voting.Menu.Button.Close")
            .text(RED.wrap("End vote"));
}