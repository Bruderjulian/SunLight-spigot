package su.nightexpress.sunlight.moduleImpl.voting.config;

public class VotingPerms {

    public static final String MODULE  = "sunlight.voting";
    public static final String COMMAND = MODULE + ".command";
    public static final String BYPASS  = MODULE + ".bypass";

    // Player-facing.

    public static final String COMMAND_LIST   = COMMAND + ".list";
    public static final String COMMAND_VIEW   = COMMAND + ".view";
    public static final String COMMAND_VOTE   = COMMAND + ".vote";
    public static final String COMMAND_MY     = COMMAND + ".myvotes";
    public static final String COMMAND_HISTORY = COMMAND + ".history";
    public static final String COMMAND_NOTIFY = COMMAND + ".notify";
    public static final String COMMAND_TEMPLATE = COMMAND + ".template";
    public static final String COMMAND_WIZARD = COMMAND + ".wizard";

    // Staff-facing.

    public static final String COMMAND_CREATE  = COMMAND + ".create";
    public static final String COMMAND_CLOSE   = COMMAND + ".close";
    public static final String COMMAND_CANCEL  = COMMAND + ".cancel";
    public static final String COMMAND_DELETE  = COMMAND + ".delete";
    public static final String COMMAND_EXTEND  = COMMAND + ".extend";
    public static final String COMMAND_INFO    = COMMAND + ".info";
    public static final String COMMAND_ALL     = COMMAND + ".all";
    public static final String COMMAND_BROADCAST = COMMAND + ".broadcast";
    public static final String COMMAND_CLONE   = COMMAND + ".clone";
    public static final String COMMAND_RENAME  = COMMAND + ".rename";
    public static final String COMMAND_DESCRIBE = COMMAND + ".describe";
    public static final String COMMAND_START   = COMMAND + ".start";

    public static final String COMMAND_OPTION_ADD    = COMMAND + ".option.add";
    public static final String COMMAND_OPTION_REMOVE = COMMAND + ".option.remove";
    public static final String COMMAND_OPTION_LIST   = COMMAND + ".option.list";
    public static final String COMMAND_OPTION_GATE   = COMMAND + ".option.gate";
    public static final String COMMAND_RULES         = COMMAND + ".rules";

    // Bypasses. Each is honored: requirements skips the poll bar and the choice gates,
    // limits skips the participation and per-poll caps, weighting counts the vote at base weight,
    // worlds skips every world check, cost skips every payment.

    public static final String BYPASS_REQUIREMENTS = BYPASS + ".requirements";
    public static final String BYPASS_LIMITS       = BYPASS + ".limits";
    public static final String BYPASS_WEIGHTING     = BYPASS + ".weighting";
    public static final String BYPASS_WORLDS       = BYPASS + ".worlds";
    public static final String BYPASS_COST         = BYPASS + ".cost";

    /** Skips the whole module, requirements and limits included. */
    public static final String EXEMPT = MODULE + ".exempt";

    /**
     * Per-option gate permissions are read from each option's own configured node at check time
     * rather than declared here, because the set of options is user-defined and a static table
     * cannot describe it. This is only the wildcard that covers all of them.
     */
    public static final String OPTION = MODULE + ".option";

    private VotingPerms() {
    }
}