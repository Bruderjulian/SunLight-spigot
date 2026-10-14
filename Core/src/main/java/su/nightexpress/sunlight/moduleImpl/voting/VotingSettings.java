package su.nightexpress.sunlight.moduleImpl.voting;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.sunlight.moduleImpl.voting.config.VotingPerms;
import su.nightexpress.sunlight.moduleImpl.voting.model.GateType;
import su.nightexpress.sunlight.moduleImpl.voting.model.MoneyMode;
import su.nightexpress.sunlight.moduleImpl.voting.model.MultiplierTable;
import su.nightexpress.sunlight.moduleImpl.voting.model.OverMaxMode;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteOption;
import su.nightexpress.sunlight.moduleImpl.voting.model.WeightCombine;
import su.nightexpress.sunlight.moduleImpl.voting.model.WeightSource;
import su.nightexpress.sunlight.moduleImpl.voting.model.WorldMode;

/**
 * Server-wide defaults and switches for the voting module.
 * <p>
 * Everything a poll inherits when it is created lives here. A poll then owns its own copy and can
 * be retuned independently, which is why none of the rule values live on a poll in this file — the
 * poll's own columns are its source of truth once it exists.
 */
public class VotingSettings extends AbstractConfig {

    // -- Data -------------------------------------------------------------------------------------

    public final ConfigProperty<String> dataTablePrefix = this.addProperty(ConfigTypes.STRING,
            "Voting.Data.Table-Prefix",
            "sunlight_voting",
            "Prefix for the votes, choices and ballots tables."
    );

    public final ConfigProperty<Integer> sweepSeconds = this.addProperty(ConfigTypes.INT,
            "Voting.Data.Sweep-Seconds",
            60,
            "How often (in seconds) to check for votes whose time is up.",
            "Use 0 to never close votes automatically."
    );

    public final ConfigProperty<Integer> keepConcludedDays = this.addProperty(ConfigTypes.INT,
            "Voting.Data.Keep-Concluded-Days",
            30,
            "How many days a finished vote is kept before it is purged.",
            "Its ballots go with it. An open vote is never purged.",
            "Use 0 to purge finished votes on the next sweep."
    );

    // -- Choices ----------------------------------------------------------------------------------

    public final ConfigProperty<Set<String>> builtInOptions = this.addProperty(ConfigTypes.STRING_SET,
            "Voting.Options.Built-In",
            Set.of(VoteOption.YES, VoteOption.NO, VoteOption.NEUTRAL),
            "Which of 'yes', 'no' and 'neutral' every new vote starts with.",
            "Removing one also disables its shortcut command ('/yes', '/no', '/neutral')."
    );

    public final ConfigProperty<String> iconYes = this.addProperty(ConfigTypes.STRING,
            "Voting.Options.Icon.Yes",
            "LIME_DYE",
            "Icon for the 'yes' choice."
    );

    public final ConfigProperty<String> iconNo = this.addProperty(ConfigTypes.STRING,
            "Voting.Options.Icon.No",
            "RED_DYE",
            "Icon for the 'no' choice."
    );

    public final ConfigProperty<String> iconNeutral = this.addProperty(ConfigTypes.STRING,
            "Voting.Options.Icon.Neutral",
            "GRAY_DYE",
            "Icon for the 'neutral' choice."
    );

    public final ConfigProperty<String> iconDefault = this.addProperty(ConfigTypes.STRING,
            "Voting.Options.Icon.Default",
            "PAPER",
            "Icon for choices added without one."
    );

    // -- Limits -----------------------------------------------------------------------------------

    public final ConfigProperty<Integer> maxActiveVotes = this.addProperty(ConfigTypes.INT,
            "Voting.Limits.Max-Active-Votes",
            10,
            "How many votes may be open across the whole server at once.",
            "Use 0 for no limit."
    );

    public final ConfigProperty<Integer> maxVotesPerCreator = this.addProperty(ConfigTypes.INT,
            "Voting.Limits.Max-Votes-Per-Creator",
            3,
            "How many open votes one staff member may have at a time.",
            "Only counts votes the player created, not ones somebody else opened.",
            "Use 0 for no limit."
    );

    public final ConfigProperty<Integer> maxParticipatingVotes = this.addProperty(ConfigTypes.INT,
            "Voting.Limits.Max-Participating-Votes",
            3,
            "How many open votes one player may hold a vote in at once.",
            "Checked when a player takes part in a new vote they are not already in.",
            "Use 0 for no limit."
    );

    public final ConfigProperty<Integer> minOptions = this.addProperty(ConfigTypes.INT,
            "Voting.Limits.Min-Options",
            2,
            "Fewest choices a vote may have. A vote with one choice is not a vote."
    );

    public final ConfigProperty<Integer> maxOptions = this.addProperty(ConfigTypes.INT,
            "Voting.Limits.Max-Options",
            15,
            "Most choices a vote may have.",
            "This also bounds the ballot table, which has one row per (vote, player, choice)."
    );

    // -- Requirements -----------------------------------------------------------------------------

    public final ConfigProperty<Integer> minPlaytimeHours = this.addProperty(ConfigTypes.INT,
            "Voting.Requirements.Min-Playtime-Hours",
            0,
            "Hours a player needs before their vote counts in any vote.",
            "Skipped when the Playtime module is disabled.",
            "Use 0 for no requirement."
    );

    public final ConfigProperty<WorldMode> worldMode = this.addProperty(ConfigTypes.forEnum(WorldMode.class),
            "Voting.Requirements.World-Mode",
            WorldMode.BLACKLIST,
            "How 'Worlds' below is read.",
            "BLACKLIST  - the listed worlds cannot vote.",
            "WHITELIST - only the listed worlds can vote.",
            "An empty list means no world restriction at all, whichever mode is set."
    );

    public final ConfigProperty<Set<String>> worlds = this.addProperty(ConfigTypes.STRING_SET,
            "Voting.Requirements.Worlds",
            Set.of(),
            "World names, read according to 'World-Mode' above."
    );

    public final ConfigProperty<Set<String>> exemptPermissions = this.addProperty(ConfigTypes.STRING_SET,
            "Voting.Requirements.Exempt-Permissions",
            Set.of(VotingPerms.EXEMPT),
            "Players holding any of these are skipped by the whole module."
    );

    // -- Multiple votes ---------------------------------------------------------------------------

    public final ConfigProperty<Boolean> multiVote = this.addProperty(ConfigTypes.BOOLEAN,
            "Voting.Multi.Enabled",
            false,
            "When true, new votes let one player hold a vote on several choices at once."
    );

    public final ConfigProperty<Integer> maxVotes = this.addProperty(ConfigTypes.INT,
            "Voting.Multi.Max-Votes",
            3,
            "How many choices one player may vote for when 'Multi.Enabled' is on.",
            "Use 1 for a normal one-choice vote, or 0 for no limit.",
            "Each extra choice is another ballot row and another chance at an option's score, so",
            "raising this changes how a result is weighted overall, not just how many clicks it takes."
    );

    // -- Money ------------------------------------------------------------------------------------

    public final ConfigProperty<MoneyMode> moneyMode = this.addProperty(ConfigTypes.forEnum(MoneyMode.class),
            "Voting.Money.Mode",
            MoneyMode.NONE,
            "How new votes use the economy.",
            "NONE   - the economy is not consulted.",
            "GATE   - a player's balance has to fall inside a range to vote.",
            "WEIGHT - holding currency makes a vote count for more.",
            "BOTH   - the range gates, and the balance adds weight inside it.",
            "Every money setting below is ignored when no economy plugin is installed."
    );

    public final ConfigProperty<Double> minBalance = this.addProperty(ConfigTypes.DOUBLE,
            "Voting.Money.Min-Balance",
            0D,
            "Balance a player needs to vote when the mode gates.",
            "Use 0 for no minimum."
    );

    public final ConfigProperty<Double> maxBalance = this.addProperty(ConfigTypes.DOUBLE,
            "Voting.Money.Max-Balance",
            0D,
            "Balance above which a vote is capped or refused when the mode gates.",
            "Use 0 for no maximum."
    );

    public final ConfigProperty<OverMaxMode> overMaxMode = this.addProperty(ConfigTypes.forEnum(OverMaxMode.class),
            "Voting.Money.Over-Maximum",
            OverMaxMode.REJECT,
            "What a vote from an unusually wealthy player is worth.",
            "REJECT - their vote is refused outright.",
            "CLAMP  - their vote is kept, but capped at the weight the maximum is worth."
    );

    public final ConfigProperty<Double> currencyPerWeight = this.addProperty(ConfigTypes.DOUBLE,
            "Voting.Money.Currency-Per-Weight",
            0D,
            "How much currency buys one extra weight when the mode grants weight.",
            "A weight costs this much divided by the highest weight any vote is allowed.",
            "Use 0 to grant no weight for money."
    );

    public final ConfigProperty<Double> maxMoneyWeight = this.addProperty(ConfigTypes.DOUBLE,
            "Voting.Money.Max-Money-Weight",
            0D,
            "Most extra weight money alone can add to a vote.",
            "Use 0 for no limit. Keep this set: without it a single wealthy player can carry a vote."
    );

    // -- Weighting --------------------------------------------------------------------------------

    public final ConfigProperty<Boolean> weightingEnabled = this.addProperty(ConfigTypes.BOOLEAN,
            "Voting.Weighting.Enabled",
            false,
            "When true, new votes weight votes by permission, rank or balance instead of counting each one equally."
    );

    public final ConfigProperty<WeightSource> weightSource = this.addProperty(
            ConfigTypes.forEnum(WeightSource.class),
            "Voting.Weighting.Source",
            WeightSource.PERMISSION,
            "Which multipliers new votes read.",
            "PERMISSION - the 'Permissions' list below.",
            "RANK        - the 'Ranks' list below.",
            "BOTH        - both of them, combined as described below."
    );

    public final ConfigProperty<WeightCombine> weightCombine = this.addProperty(
            ConfigTypes.forEnum(WeightCombine.class),
            "Voting.Weighting.Combine",
            WeightCombine.HIGHEST,
            "What a player who matches several permissions is worth.",
            "HIGHEST - only the biggest multiplier applies.",
            "ADDITIVE - all of them stack.",
            "Ranks are read one at a time, since a player has a single primary group, so this",
            "setting only changes how permissions combine."
    );

    public final ConfigProperty<Double> baseWeight = this.addProperty(ConfigTypes.DOUBLE,
            "Voting.Weighting.Base-Weight",
            1D,
            "What one vote is worth before any multiplier."
    );

    public final ConfigProperty<Double> maxMultiplier = this.addProperty(ConfigTypes.DOUBLE,
            "Voting.Weighting.Max-Multiplier",
            3D,
            "Highest multiplier a vote can carry, after the sources have been combined.",
            "Applied before 'Max-Weight' so an additive stack cannot skip past it."
    );

    public final ConfigProperty<Double> maxWeight = this.addProperty(ConfigTypes.DOUBLE,
            "Voting.Weighting.Max-Weight",
            3D,
            "Highest value one vote can reach, whatever it was worth before.",
            "This is the last word: it caps a vote however rich or high-ranked the voter is."
    );

    public final ConfigProperty<List<String>> permissionMultipliers = this.addProperty(
            ConfigTypes.STRING_LIST,
            "Voting.Weighting.Permissions",
            List.of("sunlight.voting.multiplier.2 = 2.0",
                    "sunlight.voting.multiplier.5 = 5.0"),
            "Permissions and the multiplier each one grants.",
            "A permission has to be listed here to do anything. Bukkit offers no way to ask a",
            "player which nodes they hold, so a numeric node such as '...multiplier.2.5' cannot",
            "be found on its own — it has to be named in this list."
    );

    public final ConfigProperty<List<String>> rankMultipliers = this.addProperty(
            ConfigTypes.STRING_LIST,
            "Voting.Weighting.Ranks",
            List.of("vip = 2.0"),
            "Permission groups and the multiplier each one grants.",
            "Read from the player's primary group, which comes from LuckPerms or Vault when one",
            "is installed and is empty otherwise."
    );

    // -- Resolution -------------------------------------------------------------------------------

    public final ConfigProperty<Integer> minParticipants = this.addProperty(ConfigTypes.INT,
            "Voting.Resolution.Min-Participants",
            0,
            "Voters needed before a vote can produce a result at all.",
            "Below this the vote is cancelled and the outcome commands do not run.",
            "Counts players, not choices, so someone who picked three things still counts once.",
            "Use 0 for no minimum."
    );

    public final ConfigProperty<Double> passPercent = this.addProperty(ConfigTypes.DOUBLE,
            "Voting.Resolution.Pass-Percent",
            50D,
            "Share of the total a choice has to reach to count as passing.",
            "The highest-scoring passing choice wins. If none passes, the vote fails.",
            "A share is always a proper split of 100%, including when players may pick several",
            "choices, because it is measured against the sum of every choice's score."
    );

    public final ConfigProperty<Boolean> neutralCounts = this.addProperty(ConfigTypes.BOOLEAN,
            "Voting.Resolution.Neutral-Counts-Toward-Total",
            true,
            "When true, 'neutral' votes dilute the share of the choices that were actually made.",
            "Turn this off so a poll cannot be carried by people picking neutral.",
            "Neutral voters count as participants either way."
    );

    // -- Outcomes ---------------------------------------------------------------------------------

    public final ConfigProperty<List<String>> commandsOnPass = this.addProperty(ConfigTypes.STRING_LIST,
            "Voting.Outcomes.Commands.On-Pass",
            List.of(),
            "Console commands run when a vote is decided.",
            "Placeholders: %vote_name%, %vote_key%, %option%, %option_key%, %percent%, %score%,",
            "%count%, %vote_total_score%, %vote_participants%, %pass_percent%."
    );

    public final ConfigProperty<List<String>> commandsOnFail = this.addProperty(ConfigTypes.STRING_LIST,
            "Voting.Outcomes.Commands.On-Fail",
            List.of(),
            "Console commands run when no choice reaches the pass share."
    );

    public final ConfigProperty<List<String>> commandsOnCancel = this.addProperty(ConfigTypes.STRING_LIST,
            "Voting.Outcomes.Commands.On-Cancel",
            List.of(),
            "Console commands run when a vote is cancelled.",
            "They are deliberately separate from the pass and fail lists: a vote that nobody",
            "answered and a vote a staff member stopped are not the same thing, and a refund",
            "paid on cancel has to be able to tell them apart."
    );

    // -- Notifications ----------------------------------------------------------------------------

    public final ConfigProperty<Boolean> announceOnCreate = this.addProperty(ConfigTypes.BOOLEAN,
            "Voting.Notify.On-Create",
            true,
            "When true, opening a vote announces it to the whole server."
    );

    public final ConfigProperty<String> notifyPermission = this.addProperty(ConfigTypes.STRING,
            "Voting.Notify.Permission",
            "sunlight.voting.notify",
            "Permission that receives vote announcements instead of the whole server.",
            "Leave empty to announce to everybody."
    );

    public final ConfigProperty<Boolean> notifyStaffResult = this.addProperty(ConfigTypes.BOOLEAN,
            "Voting.Notify.On-Result",
            true,
            "When true, the result of a finished vote is announced to the whole server."
    );

    // -- Command helpers --------------------------------------------------------------------------

    public final ConfigProperty<Boolean> commandGui = this.addProperty(ConfigTypes.BOOLEAN,
            "Voting.Command.Gui",
            true,
            "When true, a bare '/voting' opens the vote list instead of printing usage."
    );

    public final ConfigProperty<EnumSet<GateType>> availableGates = this.addProperty(
            ConfigTypes.forEnumSet(GateType.class),
            "Voting.Gates.Available",
            java.util.EnumSet.allOf(GateType.class),
            "Which requirements a choice is allowed to carry.",
            "Taking one out here stops it being configured on any choice, so a poll owner cannot",
            "open a paid vote on a server that is not ready for it."
    );

    /**
     * Read outside the property system because it is not a single scalar: the entries are
     * permission nodes, which contain dots and would be misread as nested config sections.
     */
    private MultiplierTable multiplierTable;

    @Override
    public void load(final FileConfig config) {
        super.load(config);
        // Deliberately never written back: re-serialising would lower-case every permission and
        // rank in the file, so a reload would silently rewrite an admin's config.
        this.multiplierTable = MultiplierTable.read(config, "Voting.Weighting");
    }

    public MultiplierTable getMultiplierTable() {
        if (this.multiplierTable == null) {
            this.multiplierTable = new MultiplierTable(Map.of(), Map.of());
        }
        return this.multiplierTable;
    }
}