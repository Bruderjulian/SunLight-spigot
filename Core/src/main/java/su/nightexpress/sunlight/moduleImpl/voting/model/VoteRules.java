package su.nightexpress.sunlight.moduleImpl.voting.model;

import java.util.Locale;
import java.util.Set;

/**
 * Everything about a poll that is a rule rather than an identity: who may vote, what a vote is
 * worth, and how the winner is picked.
 * <p>
 * Mutable and held by {@link Vote}, because every field here can be retuned by staff while the
 * poll is open. Votes already cast keep the weight they were cast with, so tightening a cap
 * afterwards never rewrites the history of who voted for what.
 */
public class VoteRules {

    private static final Set<String> NEUTRAL_KEYS = Set.of(VoteOption.NEUTRAL);

    // -- How many options one player may hold --------------------------------------------------

    private boolean multiVote;
    private int maxVotes = 1;

    // -- Eligibility ----------------------------------------------------------------------------

    private int minPlaytimeHours;
    private WorldMode worldMode = WorldMode.BLACKLIST;
    private Set<String> worlds = Set.of();

    // -- Money ----------------------------------------------------------------------------------

    private MoneyMode moneyMode = MoneyMode.NONE;
    private double minBalance;
    private double maxBalance;
    private OverMaxMode overMaxMode = OverMaxMode.REJECT;
    private double currencyPerWeight;
    private double maxMoneyWeight;

    // -- Weight ---------------------------------------------------------------------------------

    private boolean weightingEnabled;
    private WeightSource weightSource = WeightSource.PERMISSION;
    private WeightCombine weightCombine = WeightCombine.HIGHEST;
    private double baseWeight = 1D;
    private double maxMultiplier = 1D;
    private double maxWeight = 1D;

    // -- Resolution -----------------------------------------------------------------------------

    private int minParticipants;
    private double passPercent = 50D;
    private boolean neutralCountsTowardTotal = true;
    private boolean secretBallot;
    private TieBreak tieBreak = TieBreak.FIRST;

    // -- Revote ---------------------------------------------------------------------------------

    private boolean allowChange = true;
    private int changeLockSeconds;

    // -- Visibility -------------------------------------------------------------------------------

    private boolean silent;

    // -- Recurrence -------------------------------------------------------------------------------

    private boolean recurrenceEnabled;
    private int recurrenceIntervalSeconds;
    private int recurrenceLimit;

    // -- Runoff ---------------------------------------------------------------------------------

    private boolean runoffEnabled;
    private int runoffTopN = 2;
    private int runoffDurationSeconds = 86400;

    public static VoteRules defaults() {
        return new VoteRules();
    }

    /**
     * @param held      How many options this player currently holds a vote on in this poll.
     * @return how many more they may take, or {@link Integer#MAX_VALUE} when the poll is uncapped.
     */
    public int votesLeft(final int held) {
        if (!this.multiVote) {
            return 0;
        }
        if (this.maxVotes <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, this.maxVotes - held);
    }

    /**
     * @return whether one player may hold votes on more than one option at all. Kept separate from
     *         {@link #votesLeft(int)} because a poll with a cap of 1 is single-choice even if the
     *         multi-vote switch is on.
     */
    public boolean allowsMultipleVotes() {
        return this.multiVote && this.maxVotes != 1;
    }

    public boolean isWorldAllowed(final String worldName) {
        if (this.worlds.isEmpty()) {
            // An empty set means the poll places no restriction on worlds at all, regardless of
            // the mode. Otherwise switching a BLACKLIST poll to WHITELIST would silently close it
            // to everybody.
            return true;
        }
        return this.worldMode.allows(worldName, this.containsWorld(this.worlds, worldName));
    }

    static boolean containsWorld(final Set<String> set, final String worldName) {
        if (worldName == null) {
            return false;
        }
        return set.stream().anyMatch(name -> name.equalsIgnoreCase(worldName));
    }

    /**
     * @return whether the neutral option is part of the share denominator.
     */
    public boolean countsNeutral() {
        return this.neutralCountsTowardTotal;
    }

    public boolean isNeutral(final String optionKey) {
        return optionKey != null && NEUTRAL_KEYS.contains(optionKey.toLowerCase(Locale.ROOT));
    }

    /**
     * @return the multiplier ceiling that applies after the sources have been combined. Kept as a
     *         method so the two clamps can never be applied in the wrong order by accident.
     */
    public double clampMultiplier(final double multiplier) {
        return Math.max(0D, Math.min(multiplier, this.maxMultiplier));
    }

    public double clampWeight(final double weight) {
        return Math.max(0D, Math.min(weight, this.maxWeight));
    }

    // -- Accessors ------------------------------------------------------------------------------

    public boolean isMultiVote() {
        return this.multiVote;
    }

    public void setMultiVote(final boolean multiVote) {
        this.multiVote = multiVote;
    }

    public int getMaxVotes() {
        return this.maxVotes;
    }

    public void setMaxVotes(final int maxVotes) {
        // 0 means unlimited, matching the config convention. Negatives are rejected by the
        // command layer rather than silently meaning unlimited here.
        this.maxVotes = Math.max(0, maxVotes);
    }

    public int getMinPlaytimeHours() {
        return this.minPlaytimeHours;
    }

    public void setMinPlaytimeHours(final int minPlaytimeHours) {
        this.minPlaytimeHours = Math.max(0, minPlaytimeHours);
    }

    public WorldMode getWorldMode() {
        return this.worldMode;
    }

    public void setWorldMode(final WorldMode worldMode) {
        this.worldMode = worldMode == null ? WorldMode.BLACKLIST : worldMode;
    }

    public Set<String> getWorlds() {
        return this.worlds;
    }

    public void setWorlds(final Set<String> worlds) {
        this.worlds = worlds == null ? Set.of() : Set.copyOf(worlds);
    }

    public MoneyMode getMoneyMode() {
        return this.moneyMode;
    }

    public void setMoneyMode(final MoneyMode moneyMode) {
        this.moneyMode = moneyMode == null ? MoneyMode.NONE : moneyMode;
    }

    public double getMinBalance() {
        return this.minBalance;
    }

    public void setMinBalance(final double minBalance) {
        this.minBalance = Math.max(0D, minBalance);
    }

    public double getMaxBalance() {
        return this.maxBalance;
    }

    public void setMaxBalance(final double maxBalance) {
        this.maxBalance = Math.max(0D, maxBalance);
    }

    public boolean hasMaxBalance() {
        return this.maxBalance > 0D;
    }

    public OverMaxMode getOverMaxMode() {
        return this.overMaxMode;
    }

    public void setOverMaxMode(final OverMaxMode overMaxMode) {
        this.overMaxMode = overMaxMode == null ? OverMaxMode.REJECT : overMaxMode;
    }

    public double getCurrencyPerWeight() {
        return this.currencyPerWeight;
    }

    public void setCurrencyPerWeight(final double currencyPerWeight) {
        this.currencyPerWeight = Math.max(0D, currencyPerWeight);
    }

    public double getMaxMoneyWeight() {
        return this.maxMoneyWeight;
    }

    public void setMaxMoneyWeight(final double maxMoneyWeight) {
        this.maxMoneyWeight = Math.max(0D, maxMoneyWeight);
    }

    public boolean isWeightingEnabled() {
        return this.weightingEnabled;
    }

    public void setWeightingEnabled(final boolean weightingEnabled) {
        this.weightingEnabled = weightingEnabled;
    }

    public WeightSource getWeightSource() {
        return this.weightSource;
    }

    public void setWeightSource(final WeightSource weightSource) {
        this.weightSource = weightSource == null ? WeightSource.PERMISSION : weightSource;
    }

    public WeightCombine getWeightCombine() {
        return this.weightCombine;
    }

    public void setWeightCombine(final WeightCombine weightCombine) {
        this.weightCombine = weightCombine == null ? WeightCombine.HIGHEST : weightCombine;
    }

    public double getBaseWeight() {
        return this.baseWeight;
    }

    public void setBaseWeight(final double baseWeight) {
        this.baseWeight = Math.max(0D, baseWeight);
    }

    public double getMaxMultiplier() {
        return this.maxMultiplier;
    }

    public void setMaxMultiplier(final double maxMultiplier) {
        this.maxMultiplier = Math.max(0D, maxMultiplier);
    }

    public double getMaxWeight() {
        return this.maxWeight;
    }

    public void setMaxWeight(final double maxWeight) {
        this.maxWeight = Math.max(0D, maxWeight);
    }

    public int getMinParticipants() {
        return this.minParticipants;
    }

    public void setMinParticipants(final int minParticipants) {
        this.minParticipants = Math.max(0, minParticipants);
    }

    public double getPassPercent() {
        return this.passPercent;
    }

    public void setPassPercent(final double passPercent) {
        this.passPercent = Math.min(100D, Math.max(0D, passPercent));
    }

    public boolean isNeutralCountsTowardTotal() {
        return this.neutralCountsTowardTotal;
    }

    public void setNeutralCountsTowardTotal(final boolean neutralCountsTowardTotal) {
        this.neutralCountsTowardTotal = neutralCountsTowardTotal;
    }

    public boolean isSecretBallot() {
        return this.secretBallot;
    }

    public void setSecretBallot(final boolean secretBallot) {
        this.secretBallot = secretBallot;
    }

    public TieBreak getTieBreak() {
        return this.tieBreak;
    }

    public void setTieBreak(final TieBreak tieBreak) {
        this.tieBreak = tieBreak == null ? TieBreak.FIRST : tieBreak;
    }

    public boolean isAllowChange() {
        return this.allowChange;
    }

    public void setAllowChange(final boolean allowChange) {
        this.allowChange = allowChange;
    }

    public int getChangeLockSeconds() {
        return this.changeLockSeconds;
    }

    public void setChangeLockSeconds(final int changeLockSeconds) {
        this.changeLockSeconds = Math.max(0, changeLockSeconds);
    }

    public boolean isSilent() {
        return this.silent;
    }

    public void setSilent(final boolean silent) {
        this.silent = silent;
    }

    public boolean isRecurrenceEnabled() {
        return this.recurrenceEnabled;
    }

    public void setRecurrenceEnabled(final boolean recurrenceEnabled) {
        this.recurrenceEnabled = recurrenceEnabled;
    }

    public int getRecurrenceIntervalSeconds() {
        return this.recurrenceIntervalSeconds;
    }

    public void setRecurrenceIntervalSeconds(final int recurrenceIntervalSeconds) {
        this.recurrenceIntervalSeconds = Math.max(0, recurrenceIntervalSeconds);
    }

    public int getRecurrenceLimit() {
        return this.recurrenceLimit;
    }

    public void setRecurrenceLimit(final int recurrenceLimit) {
        this.recurrenceLimit = Math.max(0, recurrenceLimit);
    }

    public boolean isRunoffEnabled() {
        return this.runoffEnabled;
    }

    public void setRunoffEnabled(final boolean runoffEnabled) {
        this.runoffEnabled = runoffEnabled;
    }

    public int getRunoffTopN() {
        return this.runoffTopN;
    }

    public void setRunoffTopN(final int runoffTopN) {
        this.runoffTopN = Math.max(2, runoffTopN);
    }

    public int getRunoffDurationSeconds() {
        return this.runoffDurationSeconds;
    }

    public void setRunoffDurationSeconds(final int runoffDurationSeconds) {
        this.runoffDurationSeconds = Math.max(1, runoffDurationSeconds);
    }

    /**
     * @return a detached copy, used when a poll is cloned from the server-wide defaults.
     */
    public VoteRules copy() {
        final VoteRules copy = new VoteRules();
        copy.multiVote = this.multiVote;
        copy.maxVotes = this.maxVotes;
        copy.minPlaytimeHours = this.minPlaytimeHours;
        copy.worldMode = this.worldMode;
        copy.worlds = this.worlds;
        copy.moneyMode = this.moneyMode;
        copy.minBalance = this.minBalance;
        copy.maxBalance = this.maxBalance;
        copy.overMaxMode = this.overMaxMode;
        copy.currencyPerWeight = this.currencyPerWeight;
        copy.maxMoneyWeight = this.maxMoneyWeight;
        copy.weightingEnabled = this.weightingEnabled;
        copy.weightSource = this.weightSource;
        copy.weightCombine = this.weightCombine;
        copy.baseWeight = this.baseWeight;
        copy.maxMultiplier = this.maxMultiplier;
        copy.maxWeight = this.maxWeight;
        copy.minParticipants = this.minParticipants;
        copy.passPercent = this.passPercent;
        copy.neutralCountsTowardTotal = this.neutralCountsTowardTotal;
        copy.secretBallot = this.secretBallot;
        copy.tieBreak = this.tieBreak;
        copy.allowChange = this.allowChange;
        copy.changeLockSeconds = this.changeLockSeconds;
        copy.silent = this.silent;
        copy.recurrenceEnabled = this.recurrenceEnabled;
        copy.recurrenceIntervalSeconds = this.recurrenceIntervalSeconds;
        copy.recurrenceLimit = this.recurrenceLimit;
        copy.runoffEnabled = this.runoffEnabled;
        copy.runoffTopN = this.runoffTopN;
        copy.runoffDurationSeconds = this.runoffDurationSeconds;
        return copy;
    }
}