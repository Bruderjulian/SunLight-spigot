package su.nightexpress.sunlight.moduleImpl.voting.command;

/**
 * Command names that are also written into player-facing messages.
 * <p>
 * Kept in one place so that a message advertising a command and the command actually registered
 * cannot drift apart — the PTP module does the same thing for its accept/decline shortcuts.
 */
public class VotingCommands {

    public static final String ROOT_NAME     = "voting";
    public static final String VOTE_NAME     = "vote";
    public static final String YES_NAME      = "yes";
    public static final String NO_NAME       = "no";
    public static final String NEUTRAL_NAME  = "neutral";
    public static final String MY_VOTES_NAME = "myvotes";

    /** Full form, for a staff-only message that must be spelled out in full. */
    public static final String REMOVE_NAME = "voting option remove";

    private VotingCommands() {
    }
}