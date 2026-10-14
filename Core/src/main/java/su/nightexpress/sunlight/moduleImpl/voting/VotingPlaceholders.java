package su.nightexpress.sunlight.moduleImpl.voting;

import su.nightexpress.nightcore.util.placeholder.TypedPlaceholder;
import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingLang;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteOption;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.GOLD;

/**
 * Placeholders for the voting module.
 * <p>
 * Two layers, deliberately separated:
 * <ul>
 *     <li>{@link #VOTE} and {@link #OPTION} carry only what the object knows about itself. They are
 *     safe to resolve from anywhere, including from the database sync thread.</li>
 *     <li>Scores, shares and counts live in {@link #SCORE}, {@link #PERCENT} and {@link #COUNT},
 *     which are filled from a {@code VoteTally} at the point of display. A tally is derived state
 *     that only exists while somebody is looking, so baking it into the object resolvers would
 *     mean either caching it on the poll or resolving it from a stale copy.</li>
 * </ul>
 */
public class VotingPlaceholders extends SLPlaceholders {

    public static final String VOTE_NAME       = "%vote_name%";
    public static final String VOTE_KEY        = "%vote_key%";
    public static final String VOTE_DESC       = "%vote_description%";
    public static final String VOTE_STATUS     = "%vote_status%";
    public static final String VOTE_CREATOR    = "%vote_creator%";
    public static final String VOTE_DATE       = "%vote_date%";
    public static final String VOTE_TIME_START = "%vote_time_start%";
    public static final String VOTE_TIME_END   = "%vote_time_end%";
    public static final String VOTE_TIME_LEFT  = "%vote_time_left%";
    public static final String VOTE_TIME_TO_START = "%vote_time_to_start%";
    public static final String VOTE_MULTI      = "%vote_multi%";
    public static final String VOTE_MAX_VOTES  = "%vote_max_votes%";
    public static final String VOTE_OUTCOME    = "%vote_outcome%";
    public static final String VOTE_OUTCOME_KEY = "%vote_outcome_key%";
    public static final String VOTE_PASS_PCT   = "%vote_pass_percent%";
    public static final String VOTE_SECRET     = "%vote_secret%";
    public static final String VOTE_TIE_BREAK  = "%vote_tie_break%";
    public static final String VOTE_OPTIONS    = "%vote_options%";
    public static final String VOTE_LEADER     = "%vote_leader%";

    /** Filled from a tally, not from the poll. */
    public static final String VOTE_TOTAL_SCORE   = "%vote_total_score%";
    public static final String VOTE_PARTICIPANTS = "%vote_participants%";

    /** Literal text, not a placeholder: used in the broadcast result line. */
    public static final String WON = GOLD.wrap(" passed");

    public static final String OPTION_KEY     = "%option_key%";
    public static final String OPTION_DISPLAY = "%option_display%";
    public static final String OPTION_GATES   = "%option_gates%";

    /** Filled from a tally, not from the option. */
    public static final String SCORE   = "%score%";
    public static final String PERCENT = "%percent%";
    public static final String COUNT   = "%count%";
    public static final String COST    = "%cost%";

    public static final TypedPlaceholder<Vote> VOTE = TypedPlaceholder
            .builder(Vote.class)
            .with(VOTE_NAME, Vote::getDisplay)
            .with(VOTE_KEY, Vote::getKey)
            .with(VOTE_DESC, Vote::getDescription)
            .with(VOTE_STATUS, vote -> VotingLang.STATUS.getLocalized(vote.getStatus()))
            .with(VOTE_CREATOR, vote -> vote.getCreatorName() == null ? "" : vote.getCreatorName())
            .with(VOTE_DATE, vote -> TimeFormats.formatDateTime(vote.getCreateDate()))
            .with(VOTE_TIME_START, vote -> TimeFormats.formatDateTime(vote.getStartDate()))
            .with(VOTE_TIME_END, vote -> TimeFormats.formatDateTime(vote.getEndDate()))
            .with(VOTE_TIME_LEFT,
                    vote -> TimeFormats.formatDuration(vote.getEndDate(), TimeFormatType.LITERAL))
            .with(VOTE_TIME_TO_START,
                    vote -> vote.isScheduled()
                            ? TimeFormats.formatDuration(vote.getStartDate(), TimeFormatType.LITERAL)
                            : "")
            .with(VOTE_MULTI, vote -> VotingLang.STATE_YES_NO.get(vote.getRules().allowsMultipleVotes()))
            .with(VOTE_MAX_VOTES, vote -> {
                // Not the raw configured cap: a poll that allows one vote per player says "1"
                // whether that came from the cap or from voting being single-choice to begin with.
                if (!vote.getRules().allowsMultipleVotes()) {
                    return "1";
                }
                final int max = vote.getRules().getMaxVotes();
                return max <= 0 ? VotingLang.TEXT_UNLIMITED.text() : String.valueOf(max);
            })
            .with(VOTE_OUTCOME, vote -> {
                final VoteOption outcome = vote.getOutcomeOption();
                return outcome == null ? "" : outcome.getDisplay();
            })
            .with(VOTE_OUTCOME_KEY, vote -> vote.getOutcome() == null ? "" : vote.getOutcome())
            .with(VOTE_PASS_PCT, vote -> String.valueOf(vote.getRules().getPassPercent()))
            .with(VOTE_SECRET, vote -> VotingLang.STATE_YES_NO.get(vote.getRules().isSecretBallot()))
            .with(VOTE_TIE_BREAK, vote -> vote.getRules().getTieBreak().name())
            .with(VOTE_OPTIONS, vote -> String.join(", ",
                    vote.getOptionList().stream().map(VoteOption::getDisplay).toList()))
            .with(VOTE_LEADER, vote -> {
                final VoteOption outcome = vote.getOutcomeOption();
                return outcome == null ? "" : outcome.getDisplay();
            })
            .build();

    public static final TypedPlaceholder<VoteOption> OPTION = TypedPlaceholder
            .builder(VoteOption.class)
            .with(OPTION_KEY, VoteOption::getKey)
            .with(OPTION_DISPLAY, VoteOption::getDisplay)
            .with(OPTION_GATES, option -> option.getGates().stream().map(Enum::name)
                    .reduce((first, second) -> first + ", " + second).orElse(""))
            .with(COST, option -> option.isFree() ? VotingLang.TEXT_FREE.text()
                    : String.valueOf(option.getCost()))
            .build();

    private VotingPlaceholders() {
    }
}