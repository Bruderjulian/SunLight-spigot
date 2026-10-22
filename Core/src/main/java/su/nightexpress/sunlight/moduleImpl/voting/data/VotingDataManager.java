package su.nightexpress.sunlight.moduleImpl.voting.data;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import su.nightexpress.nightcore.db.column.Column;
import su.nightexpress.nightcore.db.statement.condition.Operator;
import su.nightexpress.nightcore.db.statement.condition.Wheres;
import su.nightexpress.nightcore.db.table.Table;
import su.nightexpress.sunlight.data.DataHandler;
import su.nightexpress.sunlight.moduleImpl.voting.VotingModule;
import su.nightexpress.sunlight.moduleImpl.voting.model.GateType;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteBallot;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteOption;
import su.nightexpress.sunlight.utils.TimeUtil;

/**
 * The voting module's three tables, and every read and write against them.
 * <p>
 * The column API has no composite keys, so the two child tables carry a surrogate key. Those keys
 * are derived from the row's real identity rather than generated randomly, so re-inserting the
 * same logical row upserts instead of duplicating — which matters because two servers in a network
 * can both write the same ballot, and with a random key neither could tell that its write had
 * already landed.
 */
public class VotingDataManager {

    // -- votes ------------------------------------------------------------------------------------

    public static final Column<UUID> COLUMN_VOTE_ID       = Column.uuidType("voteId").primaryKey().build();
    public static final Column<String> COLUMN_VOTE_KEY     = Column.stringType("voteKey", 64).build();
    public static final Column<String> COLUMN_DISPLAY      = Column.stringType("display", 128).build();
    public static final Column<String> COLUMN_DESCRIPTION = Column.stringType("description", 512).nullable().build();
    public static final Column<UUID>   COLUMN_CREATOR_ID   = Column.uuidType("creatorId").nullable().build();
    public static final Column<String> COLUMN_CREATOR_NAME = Column.stringType("creatorName", 64).nullable().build();
    public static final Column<Long>   COLUMN_CREATE_DATE  = Column.longType("createDate").build();
    public static final Column<Long>   COLUMN_START_DATE   = Column.longType("startDate").build();
    public static final Column<Long>   COLUMN_END_DATE     = Column.longType("endDate").build();
    public static final Column<String> COLUMN_STATUS       = Column.stringType("status", 16).build();
    public static final Column<String> COLUMN_OUTCOME      = Column.stringType("outcome", 64).nullable().build();
    public static final Column<Long>   COLUMN_CONCLUDE_DATE = Column.longType("concludeDate").build();

    public static final Column<Boolean> COLUMN_RULE_MULTI_VOTE  = Column.booleanType("multiVote").build();
    public static final Column<Integer> COLUMN_RULE_MAX_VOTES   = Column.intType("maxVotes").build();
    public static final Column<Integer> COLUMN_RULE_MIN_PLAYTIME = Column.intType("minPlaytime").build();
    public static final Column<String>  COLUMN_RULE_WORLD_MODE  = Column.stringType("worldMode", 16).build();
    public static final Column<String>  COLUMN_RULE_WORLDS      = Column.stringType("worlds", 512).build();
    public static final Column<String>  COLUMN_RULE_MONEY_MODE  = Column.stringType("moneyMode", 16).build();
    public static final Column<Double>  COLUMN_RULE_MIN_BALANCE = Column.doubleType("minBalance").build();
    public static final Column<Double>  COLUMN_RULE_MAX_BALANCE = Column.doubleType("maxBalance").build();
    public static final Column<String>  COLUMN_RULE_OVER_MAX    = Column.stringType("overMax", 16).build();
    public static final Column<Double>  COLUMN_RULE_CURRENCY_W  = Column.doubleType("currencyWeight").build();
    public static final Column<Double>  COLUMN_RULE_MAX_MONEY_W = Column.doubleType("maxMoneyWeight").build();
    public static final Column<Boolean> COLUMN_RULE_WEIGHTED    = Column.booleanType("weighted").build();
    public static final Column<String>  COLUMN_RULE_W_SOURCE    = Column.stringType("weightSource", 16).build();
    public static final Column<String>  COLUMN_RULE_W_COMBINE   = Column.stringType("weightCombine", 16).build();
    public static final Column<Double>  COLUMN_RULE_BASE_W      = Column.doubleType("baseWeight").build();
    public static final Column<Double>  COLUMN_RULE_MAX_MULT    = Column.doubleType("maxMultiplier").build();
    public static final Column<Double>  COLUMN_RULE_MAX_WEIGHT  = Column.doubleType("maxWeight").build();
    public static final Column<Integer> COLUMN_RULE_MIN_PART    = Column.intType("minParticipants").build();
    public static final Column<Double>  COLUMN_RULE_PASS_PCT    = Column.doubleType("passPercent").build();
    public static final Column<Boolean> COLUMN_RULE_NEUTRAL     = Column.booleanType("neutralCounts").build();
    public static final Column<Boolean> COLUMN_RULE_SECRET      = Column.booleanType("secretBallot").build();
    public static final Column<String>  COLUMN_RULE_TIE_BREAK   = Column.stringType("tieBreak", 16).build();
    public static final Column<Boolean> COLUMN_RULE_ALLOW_CHANGE = Column.booleanType("allowChange").build();
    public static final Column<Integer> COLUMN_RULE_CHANGE_LOCK = Column.intType("changeLockSeconds").build();
    public static final Column<Boolean> COLUMN_RULE_RUNOFF      = Column.booleanType("runoffEnabled").build();
    public static final Column<Integer> COLUMN_RULE_RUNOFF_TOP  = Column.intType("runoffTopN").build();
    public static final Column<Integer> COLUMN_RULE_RUNOFF_DUR  = Column.intType("runoffDuration").build();
    public static final Column<Boolean> COLUMN_RULE_SILENT      = Column.booleanType("silent").build();
    public static final Column<Boolean> COLUMN_RULE_RECURRENCE  = Column.booleanType("recurrenceEnabled").build();
    public static final Column<Integer> COLUMN_RULE_REC_INTERVAL = Column.intType("recurrenceInterval").build();
    public static final Column<Integer> COLUMN_RULE_REC_LIMIT   = Column.intType("recurrenceLimit").build();
    public static final Column<Integer> COLUMN_REC_COUNT         = Column.intType("recurrenceCount").build();
    public static final Column<String>  COLUMN_REC_SERIES        = Column.stringType("recurrenceSeries", 64).nullable().build();

    // -- vote options -----------------------------------------------------------------------------

    public static final Column<UUID> COLUMN_OPTION_ID      = Column.uuidType("optionId").primaryKey().build();
    public static final Column<UUID> COLUMN_OPTION_VOTE    = Column.uuidType("voteId").build();
    public static final Column<String> COLUMN_OPTION_KEY   = Column.stringType("optionKey", 64).build();
    public static final Column<String> COLUMN_OPTION_NAME  = Column.stringType("display", 128).build();
    public static final Column<String> COLUMN_OPTION_ICON  = Column.stringType("icon", 64).build();
    public static final Column<String> COLUMN_OPTION_GATES = Column.stringType("gates", 64).build();
    public static final Column<Double> COLUMN_OPTION_COST  = Column.doubleType("cost").build();
    public static final Column<String> COLUMN_OPTION_PERM  = Column.stringType("permission", 128).build();
    public static final Column<String> COLUMN_OPTION_RANK  = Column.stringType("rank", 64).build();
    public static final Column<String> COLUMN_OPTION_WORLDS = Column.stringType("worlds", 512).build();
    public static final Column<Integer> COLUMN_OPTION_PLAYTIME = Column.intType("minPlaytime").build();

    // -- ballots ----------------------------------------------------------------------------------

    public static final Column<UUID> COLUMN_BALLOT_ID      = Column.uuidType("ballotId").primaryKey().build();
    public static final Column<UUID> COLUMN_BALLOT_VOTE    = Column.uuidType("voteId").build();
    public static final Column<String> COLUMN_BALLOT_OPTION = Column.stringType("optionKey", 64).build();
    public static final Column<UUID> COLUMN_BALLOT_PLAYER  = Column.uuidType("playerId").build();
    public static final Column<String> COLUMN_BALLOT_NAME  = Column.stringType("playerName", 64).build();
    public static final Column<Double> COLUMN_BALLOT_WEIGHT = Column.doubleType("weight").build();
    public static final Column<Double> COLUMN_BALLOT_COST  = Column.doubleType("costPaid").build();
    public static final Column<Long> COLUMN_BALLOT_DATE    = Column.longType("date").build();

    // -- reminder opt-outs ------------------------------------------------------------------------

    public static final Column<UUID> COLUMN_PREF_PLAYER = Column.uuidType("playerId").primaryKey().build();
    public static final Column<Boolean> COLUMN_PREF_ENABLED = Column.booleanType("enabled").build();

    /**
     * The option table's row shape. Options live inside a vote and are keyed by their vote's id,
     * neither of which belongs on the option model itself, so the statements are built over this
     * instead of over {@link VoteOption}.
     */
    public record OptionRow(UUID optionId, UUID voteId, VoteOption option) {
    }

    /**
     * One player's reminder preference. Absent row means opted in; only opt-outs are stored
     * in memory long-term, but every explicit choice is persisted so a relog keeps it.
     */
    public record NotifyPref(UUID playerId, boolean enabled) {
    }

    private final DataHandler dataHandler;
    private final VotingModule module;

    private Table tableVotes;
    private Table tableOptions;
    private Table tableBallots;
    private Table tablePrefs;

    public VotingDataManager(final DataHandler dataHandler, final VotingModule module) {
        this.dataHandler = dataHandler;
        this.module = module;
    }

    public void init(final String tablePrefix) {
        this.tableVotes = Table.builder(tablePrefix + "_votes")
                .withColumn(
                        COLUMN_VOTE_ID, COLUMN_VOTE_KEY, COLUMN_DISPLAY, COLUMN_DESCRIPTION, COLUMN_CREATOR_ID, COLUMN_CREATOR_NAME,
                        COLUMN_CREATE_DATE, COLUMN_START_DATE, COLUMN_END_DATE, COLUMN_STATUS, COLUMN_OUTCOME, COLUMN_CONCLUDE_DATE,
                        COLUMN_RULE_MULTI_VOTE, COLUMN_RULE_MAX_VOTES, COLUMN_RULE_MIN_PLAYTIME, COLUMN_RULE_WORLD_MODE,
                        COLUMN_RULE_WORLDS, COLUMN_RULE_MONEY_MODE, COLUMN_RULE_MIN_BALANCE, COLUMN_RULE_MAX_BALANCE,
                        COLUMN_RULE_OVER_MAX, COLUMN_RULE_CURRENCY_W, COLUMN_RULE_MAX_MONEY_W, COLUMN_RULE_WEIGHTED,
                        COLUMN_RULE_W_SOURCE, COLUMN_RULE_W_COMBINE, COLUMN_RULE_BASE_W, COLUMN_RULE_MAX_MULT,
                        COLUMN_RULE_MAX_WEIGHT, COLUMN_RULE_MIN_PART, COLUMN_RULE_PASS_PCT, COLUMN_RULE_NEUTRAL,
                        COLUMN_RULE_SECRET, COLUMN_RULE_TIE_BREAK, COLUMN_RULE_ALLOW_CHANGE, COLUMN_RULE_CHANGE_LOCK,
                        COLUMN_RULE_RUNOFF, COLUMN_RULE_RUNOFF_TOP, COLUMN_RULE_RUNOFF_DUR,
                        COLUMN_RULE_SILENT, COLUMN_RULE_RECURRENCE, COLUMN_RULE_REC_INTERVAL, COLUMN_RULE_REC_LIMIT,
                        COLUMN_REC_COUNT, COLUMN_REC_SERIES)
                .build();

        this.tableOptions = Table.builder(tablePrefix + "_vote_options")
                .withColumn(
                        COLUMN_OPTION_ID, COLUMN_OPTION_VOTE, COLUMN_OPTION_KEY, COLUMN_OPTION_NAME, COLUMN_OPTION_ICON,
                        COLUMN_OPTION_GATES, COLUMN_OPTION_COST, COLUMN_OPTION_PERM, COLUMN_OPTION_RANK,
                        COLUMN_OPTION_WORLDS, COLUMN_OPTION_PLAYTIME)
                .build();

        this.tableBallots = Table.builder(tablePrefix + "_vote_ballots")
                .withColumn(
                        COLUMN_BALLOT_ID, COLUMN_BALLOT_VOTE, COLUMN_BALLOT_OPTION, COLUMN_BALLOT_PLAYER,
                        COLUMN_BALLOT_NAME, COLUMN_BALLOT_WEIGHT, COLUMN_BALLOT_COST, COLUMN_BALLOT_DATE)
                .build();

        this.tablePrefs = Table.builder(tablePrefix + "_notify_prefs")
                .withColumn(COLUMN_PREF_PLAYER, COLUMN_PREF_ENABLED)
                .build();

        this.dataHandler.createTable(this.tableVotes);
        this.dataHandler.createTable(this.tableOptions);
        this.dataHandler.createTable(this.tableBallots);
        this.dataHandler.createTable(this.tablePrefs);

        this.dataHandler.addTableSync(this.tableVotes, this::syncVote);
        this.dataHandler.addTableSync(this.tableOptions, this::syncOption);
        this.dataHandler.addTableSync(this.tableBallots, this::syncBallot);
    }

    private void syncVote(final ResultSet resultSet) {
        try {
            final Vote vote = VotingQueries.VOTE_LOADER.map(resultSet);
            if (vote != null) {
                this.module.getRepository().upsertVote(vote);
            }
        } catch (final Exception exception) {
            // Widened beyond SQLException on purpose: a single malformed row must not kill the
            // whole table sync. The row is skipped and the stack trace says which one.
            exception.printStackTrace();
        }
    }

    private void syncOption(final ResultSet resultSet) {
        try {
            final OptionRow row = VotingQueries.OPTION_LOADER.map(resultSet);
            if (row != null) {
                this.module.getRepository().upsertOption(row.option(), row.voteId());
            }
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }

    private void syncBallot(final ResultSet resultSet) {
        try {
            final VoteBallot ballot = VotingQueries.BALLOT_LOADER.map(resultSet);
            if (ballot != null) {
                this.module.getBallotRepository().upsert(ballot);
            }
        } catch (final Exception exception) {
            exception.printStackTrace();
        }
    }

    // -- Reads ------------------------------------------------------------------------------------

    public List<Vote> getVotes() {
        return this.dataHandler.selectAny(this.tableVotes, VotingQueries.SELECT_VOTE);
    }

    public List<VotingDataManager.OptionRow> getOptions() {
        return this.dataHandler.selectAny(this.tableOptions, VotingQueries.SELECT_OPTION);
    }

    public List<VoteBallot> getBallots() {
        return this.dataHandler.selectAny(this.tableBallots, VotingQueries.SELECT_BALLOT);
    }

    public boolean existsKey(final String voteKey) {
        return this.dataHandler.selectFirst(this.tableVotes, VotingQueries.SELECT_VOTE,
                Wheres.where(COLUMN_VOTE_KEY, Operator.EQUALS_IGNORE_CASE, o -> voteKey)).isPresent();
    }

    // -- Writes -----------------------------------------------------------------------------------

    public void insertVote(final Vote vote) {
        this.dataHandler.insert(this.tableVotes, VotingQueries.INSERT_VOTE, vote);
    }

    public void updateVote(final Vote vote) {
        this.dataHandler.update(this.tableVotes, VotingQueries.UPDATE_VOTE, vote,
                Wheres.whereUUID(COLUMN_VOTE_ID, Vote::getId));
    }

    public void insertOption(final UUID voteId, final VoteOption option) {
        this.dataHandler.insert(this.tableOptions, VotingQueries.INSERT_OPTION, row(voteId, option));
    }

    public void updateOption(final UUID voteId, final VoteOption option) {
        // Matched by (vote, key) like the delete below, so a row with a non-derived stored id is
        // still updated instead of silently missed.
        final String key = option.getKey().toLowerCase(java.util.Locale.ROOT);
        final Wheres<OptionRow> where = Wheres.whereUUID(COLUMN_OPTION_VOTE, (OptionRow o) -> voteId);
        where.and(COLUMN_OPTION_KEY, Operator.EQUALS_IGNORE_CASE, o -> (Object) key);
        this.dataHandler.update(this.tableOptions, VotingQueries.UPDATE_OPTION, row(voteId, option), where);
    }

    /**
     * Deletes by (vote, key) rather than by the derived row id, so a row written by hand or
     * imported with a non-derived id is still removed instead of orphaned.
     */
    public void deleteOption(final UUID voteId, final String optionKey) {
        if (voteId == null || optionKey == null) {
            return;
        }
        final String key = optionKey.toLowerCase(java.util.Locale.ROOT);
        final Wheres<Object> where = Wheres.whereUUID(COLUMN_OPTION_VOTE, o -> voteId);
        where.and(COLUMN_OPTION_KEY, Operator.EQUALS_IGNORE_CASE, o -> (Object) key);
        this.dataHandler.delete(this.tableOptions, where);
    }

    public void deleteVote(final Vote vote) {
        this.deleteBallots(vote.getId());
        this.dataHandler.delete(this.tableOptions, Wheres.whereUUID(COLUMN_OPTION_VOTE, o -> vote.getId()));
        this.dataHandler.delete(this.tableVotes, vote, Wheres.whereUUID(COLUMN_VOTE_ID, Vote::getId));
    }

    public void insertBallot(final VoteBallot ballot) {
        this.dataHandler.insert(this.tableBallots, VotingQueries.INSERT_BALLOT, ballot);
    }

    /**
     * Deletes by (vote, player, option) rather than by the derived row id, for the same reason as
     * {@link #deleteOption}: the stored row is what has to go, whatever its id column holds.
     */
    public void deleteBallot(final UUID voteId, final UUID playerId, final String optionKey) {
        if (voteId == null || playerId == null || optionKey == null) {
            return;
        }
        final String key = optionKey.toLowerCase(java.util.Locale.ROOT);
        final Wheres<Object> where = Wheres.whereUUID(COLUMN_BALLOT_VOTE, o -> voteId);
        where.and(COLUMN_BALLOT_PLAYER, Operator.EQUALS, o -> (Object) playerId.toString());
        where.and(COLUMN_BALLOT_OPTION, Operator.EQUALS_IGNORE_CASE, o -> (Object) key);
        this.dataHandler.delete(this.tableBallots, where);
    }

    public void deleteBallots(final UUID voteId) {
        this.dataHandler.delete(this.tableBallots, Wheres.whereUUID(COLUMN_BALLOT_VOTE, o -> voteId));
    }

    public Map<UUID, Boolean> getNotifyPrefs() {
        final List<NotifyPref> prefs = this.dataHandler.selectAny(this.tablePrefs, VotingQueries.SELECT_PREF);
        final Map<UUID, Boolean> map = new LinkedHashMap<>();
        for (final NotifyPref pref : prefs) {
            if (pref != null && pref.playerId() != null) {
                map.put(pref.playerId(), pref.enabled());
            }
        }
        return map;
    }

    public void setNotifyPref(final UUID playerId, final boolean enabled) {
        if (playerId == null) {
            return;
        }
        final NotifyPref pref = new NotifyPref(playerId, enabled);
        this.dataHandler.insert(this.tablePrefs, VotingQueries.INSERT_PREF, pref);
        // Insert upserts on the derived PK; update as well so a non-derived stored row still flips.
        final Wheres<NotifyPref> where = Wheres.whereUUID(COLUMN_PREF_PLAYER, o -> playerId);
        this.dataHandler.update(this.tablePrefs, VotingQueries.UPDATE_PREF, pref, where);
    }

    /**
     * Clears finished votes and everything inside them.
     * <p>
     * Only terminal rows are considered: an open vote is never swept away however long it has been
     * running, because losing it would silently invalidate every ballot cast in it.
     */
    public void purgeFinished(final int keepDays) {
        final LocalDateTime deadline = TimeUtil.getCurrentDateTime().minusDays(Math.max(0, keepDays));
        final long deadlineMs = TimeUtil.toEpochMillis(deadline);

        final List<Vote> stale = this.module.getRepository().getVotes().stream()
                .filter(vote -> vote.getStatus().isTerminal())
                // A terminal row with no conclude date (legacy or corrupt) falls back to its end
                // date so it is eventually purged instead of accumulating forever.
                .filter(vote -> {
                    final long stamped = vote.getConcludeDate() > 0L ? vote.getConcludeDate() : vote.getEndDate();
                    return stamped > 0L && stamped <= deadlineMs;
                })
                .toList();

        for (final Vote vote : stale) {
            this.deleteVote(vote);
            this.module.getRepository().removeVote(vote);
            this.module.getBallotRepository().removeVote(vote.getId());
        }
    }

    // -- Helpers ----------------------------------------------------------------------------------

    private static OptionRow row(final UUID voteId, final VoteOption option) {
        return new OptionRow(deriveOptionId(voteId, option.getKey()), voteId, option);
    }

    /**
     * A stable identity for an option row, so an insert that repeats what is already stored
     * upserts instead of producing a second copy.
     */
    public static UUID deriveOptionId(final UUID voteId, final String optionKey) {
        return derive("option", voteId + "|" + optionKey.toLowerCase(Locale.ROOT));
    }

    public static UUID deriveBallotId(final UUID voteId, final UUID playerId, final String optionKey) {
        return derive("ballot", voteId + "|" + playerId + "|" + optionKey.toLowerCase(Locale.ROOT));
    }

    private static UUID derive(final String kind, final String identity) {
        return UUID.nameUUIDFromBytes((kind + "|" + identity).getBytes(StandardCharsets.UTF_8));
    }

    static UUID readUuid(final ResultSet resultSet, final Column<UUID> column) throws SQLException {
        return readUuid(resultSet, column.getName());
    }

    static UUID readUuid(final ResultSet resultSet, final String column) throws SQLException {
        final String raw = resultSet.getString(column);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    /**
     * World names are stored in one column. They are joined rather than normalised because they
     * are only ever compared case-insensitively, and a world may legitimately contain a semicolon
     * nowhere but a name from a careless config could — which is why an empty decode is treated
     * as "no restriction" everywhere rather than as a restriction matching nothing.
     */
    public static String encodeSet(final Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(";", values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList());
    }

    public static Set<String> decodeSet(final String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        final Set<String> values = new LinkedHashSet<>();
        for (final String entry : raw.split(";")) {
            if (!entry.isBlank()) {
                values.add(entry.trim());
            }
        }
        return values;
    }

    public static String encodeGates(final Collection<GateType> gates) {
        if (gates == null || gates.isEmpty()) {
            return "";
        }
        return String.join(";", gates.stream().map(Enum::name).toList());
    }

    public static Set<GateType> decodeGates(final String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        final Set<GateType> gates = EnumSet.noneOf(GateType.class);
        Arrays.stream(raw.split(";")).filter(entry -> !entry.isBlank()).forEach(entry -> {
            try {
                gates.add(GateType.valueOf(entry.trim().toUpperCase(Locale.ROOT)));
            } catch (final IllegalArgumentException ignored) {
                // A gate type dropped from the module in an upgrade must not break the whole row:
                // whatever gates remain are still perfectly usable.
            }
        });
        return gates;
    }
}