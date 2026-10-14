package su.nightexpress.sunlight.moduleImpl.voting.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

import su.nightexpress.nightcore.db.column.Column;
import su.nightexpress.nightcore.db.statement.RowMapper;
import su.nightexpress.nightcore.db.statement.template.InsertStatement;
import su.nightexpress.nightcore.db.statement.template.SelectStatement;
import su.nightexpress.nightcore.db.statement.template.UpdateStatement;
import su.nightexpress.sunlight.moduleImpl.voting.model.MoneyMode;
import su.nightexpress.sunlight.moduleImpl.voting.model.OverMaxMode;
import su.nightexpress.sunlight.moduleImpl.voting.model.TieBreak;
import su.nightexpress.sunlight.moduleImpl.voting.model.Vote;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteBallot;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteOption;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteRules;
import su.nightexpress.sunlight.moduleImpl.voting.model.VoteStatus;
import su.nightexpress.sunlight.moduleImpl.voting.model.WeightCombine;
import su.nightexpress.sunlight.moduleImpl.voting.model.WeightSource;
import su.nightexpress.sunlight.moduleImpl.voting.model.WorldMode;
import su.nightexpress.sunlight.utils.Utils;

import static su.nightexpress.sunlight.moduleImpl.voting.data.VotingDataManager.*;

public class VotingQueries {

    public static final RowMapper<Vote> VOTE_LOADER = resultSet -> {
        try {
            final UUID id = readUuid(resultSet, COLUMN_VOTE_ID);
            if (id == null) {
                return null;
            }

            final VoteStatus status = readEnum(resultSet, COLUMN_STATUS, VoteStatus.class);
            if (status == null) {
                // Not OPEN: an unreadable status must never be mistaken for a poll that is still
                // running, or a deliberately closed poll would start accepting votes again.
                return new Vote(id,
                        resultSet.getString(COLUMN_VOTE_KEY.getName()),
                        resultSet.getString(COLUMN_DISPLAY.getName()),
                        readUuid(resultSet, COLUMN_CREATOR_ID),
                        resultSet.getString(COLUMN_CREATOR_NAME.getName()),
                        resultSet.getLong(COLUMN_CREATE_DATE.getName()),
                        resultSet.getLong(COLUMN_END_DATE.getName()),
                        VoteStatus.CANCELLED,
                        Set.of(),
                        new VoteRules(),
                        resultSet.getString(COLUMN_OUTCOME.getName()),
                        resultSet.getLong(COLUMN_CONCLUDE_DATE.getName()));
            }

            final VoteRules rules = new VoteRules();
            rules.setMultiVote(resultSet.getBoolean(COLUMN_RULE_MULTI_VOTE.getName()));
            rules.setMaxVotes(resultSet.getInt(COLUMN_RULE_MAX_VOTES.getName()));
            rules.setMinPlaytimeHours(resultSet.getInt(COLUMN_RULE_MIN_PLAYTIME.getName()));
            rules.setWorldMode(readEnum(resultSet, COLUMN_RULE_WORLD_MODE, WorldMode.class));
            rules.setWorlds(decodeSet(resultSet.getString(COLUMN_RULE_WORLDS.getName())));
            rules.setMoneyMode(readEnum(resultSet, COLUMN_RULE_MONEY_MODE, MoneyMode.class));
            rules.setMinBalance(resultSet.getDouble(COLUMN_RULE_MIN_BALANCE.getName()));
            rules.setMaxBalance(resultSet.getDouble(COLUMN_RULE_MAX_BALANCE.getName()));
            rules.setOverMaxMode(readEnum(resultSet, COLUMN_RULE_OVER_MAX, OverMaxMode.class));
            rules.setCurrencyPerWeight(resultSet.getDouble(COLUMN_RULE_CURRENCY_W.getName()));
            rules.setMaxMoneyWeight(resultSet.getDouble(COLUMN_RULE_MAX_MONEY_W.getName()));
            rules.setWeightingEnabled(resultSet.getBoolean(COLUMN_RULE_WEIGHTED.getName()));
            rules.setWeightSource(readEnum(resultSet, COLUMN_RULE_W_SOURCE, WeightSource.class));
            rules.setWeightCombine(readEnum(resultSet, COLUMN_RULE_W_COMBINE, WeightCombine.class));
            rules.setBaseWeight(resultSet.getDouble(COLUMN_RULE_BASE_W.getName()));
            rules.setMaxMultiplier(resultSet.getDouble(COLUMN_RULE_MAX_MULT.getName()));
            rules.setMaxWeight(resultSet.getDouble(COLUMN_RULE_MAX_WEIGHT.getName()));
            rules.setMinParticipants(resultSet.getInt(COLUMN_RULE_MIN_PART.getName()));
            rules.setPassPercent(resultSet.getDouble(COLUMN_RULE_PASS_PCT.getName()));
            rules.setNeutralCountsTowardTotal(resultSet.getBoolean(COLUMN_RULE_NEUTRAL.getName()));
            rules.setSecretBallot(readBooleanOr(resultSet, COLUMN_RULE_SECRET, false));
            final TieBreak tieBreak = readEnum(resultSet, COLUMN_RULE_TIE_BREAK, TieBreak.class);
            rules.setTieBreak(tieBreak == null ? TieBreak.FIRST : tieBreak);
            rules.setAllowChange(readBooleanOr(resultSet, COLUMN_RULE_ALLOW_CHANGE, true));
            rules.setChangeLockSeconds(readIntOr(resultSet, COLUMN_RULE_CHANGE_LOCK, 0));
            rules.setRunoffEnabled(readBooleanOr(resultSet, COLUMN_RULE_RUNOFF, false));
            rules.setRunoffTopN(Math.max(2, readIntOr(resultSet, COLUMN_RULE_RUNOFF_TOP, 2)));
            rules.setRunoffDurationSeconds(Math.max(1, readIntOr(resultSet, COLUMN_RULE_RUNOFF_DUR, 86400)));

            final long createDate = resultSet.getLong(COLUMN_CREATE_DATE.getName());
            final String description = readStringOr(resultSet, COLUMN_DESCRIPTION, "");
            long startDate = readLongOr(resultSet, COLUMN_START_DATE, 0L);
            if (startDate <= 0L) {
                startDate = createDate;
            }

            return new Vote(
                    id,
                    resultSet.getString(COLUMN_VOTE_KEY.getName()),
                    resultSet.getString(COLUMN_DISPLAY.getName()),
                    description,
                    readUuid(resultSet, COLUMN_CREATOR_ID),
                    resultSet.getString(COLUMN_CREATOR_NAME.getName()),
                    createDate,
                    startDate,
                    resultSet.getLong(COLUMN_END_DATE.getName()),
                    status,
                    Set.of(),
                    rules,
                    resultSet.getString(COLUMN_OUTCOME.getName()),
                    resultSet.getLong(COLUMN_CONCLUDE_DATE.getName())
            );
        } catch (final SQLException | IllegalArgumentException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    /**
 * Reads an option together with the vote it belongs to.
 * <p>
 * The option table's own key is derived rather than stored, so the parent vote id has to come out
 * of the row along with it — otherwise a choice that syncs before its poll has nowhere to go.
 */
    public static final RowMapper<VotingDataManager.OptionRow> OPTION_LOADER = resultSet -> {
        try {
            final UUID voteId = readUuid(resultSet, COLUMN_OPTION_VOTE);
            final String key = resultSet.getString(COLUMN_OPTION_KEY.getName());
            if (voteId == null || key == null || key.isBlank()) {
                return null;
            }

            final VoteOption option = new VoteOption(key,
                    resultSet.getString(COLUMN_OPTION_NAME.getName()),
                    resultSet.getString(COLUMN_OPTION_ICON.getName()));
            option.setGates(decodeGates(resultSet.getString(COLUMN_OPTION_GATES.getName())));
            option.setCost(resultSet.getDouble(COLUMN_OPTION_COST.getName()));
            option.setPermission(resultSet.getString(COLUMN_OPTION_PERM.getName()));
            option.setRank(resultSet.getString(COLUMN_OPTION_RANK.getName()));
            option.setWorlds(decodeSet(resultSet.getString(COLUMN_OPTION_WORLDS.getName())));
            option.setMinPlaytimeHours(resultSet.getInt(COLUMN_OPTION_PLAYTIME.getName()));

            return new VotingDataManager.OptionRow(VotingDataManager.deriveOptionId(voteId, key), voteId, option);
        } catch (final SQLException | IllegalArgumentException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    public static final RowMapper<VoteBallot> BALLOT_LOADER = resultSet -> {
        try {
            final UUID voteId = readUuid(resultSet, COLUMN_BALLOT_VOTE);
            final UUID playerId = readUuid(resultSet, COLUMN_BALLOT_PLAYER);
            final String optionKey = resultSet.getString(COLUMN_BALLOT_OPTION.getName());
            if (voteId == null || playerId == null || optionKey == null) {
                return null;
            }

            return new VoteBallot(
                    voteId,
                    optionKey,
                    playerId,
                    resultSet.getString(COLUMN_BALLOT_NAME.getName()),
                    resultSet.getDouble(COLUMN_BALLOT_WEIGHT.getName()),
                    resultSet.getDouble(COLUMN_BALLOT_COST.getName()),
                    resultSet.getLong(COLUMN_BALLOT_DATE.getName())
            );
        } catch (final SQLException | IllegalArgumentException exception) {
            exception.printStackTrace();
            return null;
        }
    };

    public static final SelectStatement<Vote> SELECT_VOTE = SelectStatement.builder(VOTE_LOADER).build();
    public static final SelectStatement<VotingDataManager.OptionRow> SELECT_OPTION = SelectStatement
            .builder(OPTION_LOADER).build();
    public static final SelectStatement<VoteBallot> SELECT_BALLOT = SelectStatement.builder(BALLOT_LOADER).build();

    public static final InsertStatement<Vote> INSERT_VOTE = InsertStatement.builder(Vote.class)
            .setUUID(COLUMN_VOTE_ID, Vote::getId)
            .setString(COLUMN_VOTE_KEY, Vote::getKey)
            .setString(COLUMN_DISPLAY, Vote::getDisplay)
            .setString(COLUMN_DESCRIPTION, Vote::getDescription)
            .setUUID(COLUMN_CREATOR_ID, Vote::getCreatorId)
            .setString(COLUMN_CREATOR_NAME, Vote::getCreatorName)
            .setLong(COLUMN_CREATE_DATE, Vote::getCreateDate)
            .setLong(COLUMN_START_DATE, Vote::getStartDate)
            .setLong(COLUMN_END_DATE, Vote::getEndDate)
            .setString(COLUMN_STATUS, vote -> vote.getStatus().name())
            .setString(COLUMN_OUTCOME, Vote::getOutcome)
            .setLong(COLUMN_CONCLUDE_DATE, Vote::getConcludeDate)
            .setBoolean(COLUMN_RULE_MULTI_VOTE, vote -> vote.getRules().isMultiVote())
            .setInt(COLUMN_RULE_MAX_VOTES, vote -> vote.getRules().getMaxVotes())
            .setInt(COLUMN_RULE_MIN_PLAYTIME, vote -> vote.getRules().getMinPlaytimeHours())
            .setString(COLUMN_RULE_WORLD_MODE, vote -> vote.getRules().getWorldMode().name())
            .setString(COLUMN_RULE_WORLDS, vote -> encodeSet(vote.getRules().getWorlds()))
            .setString(COLUMN_RULE_MONEY_MODE, vote -> vote.getRules().getMoneyMode().name())
            .setDouble(COLUMN_RULE_MIN_BALANCE, vote -> vote.getRules().getMinBalance())
            .setDouble(COLUMN_RULE_MAX_BALANCE, vote -> vote.getRules().getMaxBalance())
            .setString(COLUMN_RULE_OVER_MAX, vote -> vote.getRules().getOverMaxMode().name())
            .setDouble(COLUMN_RULE_CURRENCY_W, vote -> vote.getRules().getCurrencyPerWeight())
            .setDouble(COLUMN_RULE_MAX_MONEY_W, vote -> vote.getRules().getMaxMoneyWeight())
            .setBoolean(COLUMN_RULE_WEIGHTED, vote -> vote.getRules().isWeightingEnabled())
            .setString(COLUMN_RULE_W_SOURCE, vote -> vote.getRules().getWeightSource().name())
            .setString(COLUMN_RULE_W_COMBINE, vote -> vote.getRules().getWeightCombine().name())
            .setDouble(COLUMN_RULE_BASE_W, vote -> vote.getRules().getBaseWeight())
            .setDouble(COLUMN_RULE_MAX_MULT, vote -> vote.getRules().getMaxMultiplier())
            .setDouble(COLUMN_RULE_MAX_WEIGHT, vote -> vote.getRules().getMaxWeight())
            .setInt(COLUMN_RULE_MIN_PART, vote -> vote.getRules().getMinParticipants())
            .setDouble(COLUMN_RULE_PASS_PCT, vote -> vote.getRules().getPassPercent())
            .setBoolean(COLUMN_RULE_NEUTRAL, vote -> vote.getRules().isNeutralCountsTowardTotal())
            .setBoolean(COLUMN_RULE_SECRET, vote -> vote.getRules().isSecretBallot())
            .setString(COLUMN_RULE_TIE_BREAK, vote -> vote.getRules().getTieBreak().name())
            .setBoolean(COLUMN_RULE_ALLOW_CHANGE, vote -> vote.getRules().isAllowChange())
            .setInt(COLUMN_RULE_CHANGE_LOCK, vote -> vote.getRules().getChangeLockSeconds())
            .setBoolean(COLUMN_RULE_RUNOFF, vote -> vote.getRules().isRunoffEnabled())
            .setInt(COLUMN_RULE_RUNOFF_TOP, vote -> vote.getRules().getRunoffTopN())
            .setInt(COLUMN_RULE_RUNOFF_DUR, vote -> vote.getRules().getRunoffDurationSeconds())
            .build();

    public static final UpdateStatement<Vote> UPDATE_VOTE = UpdateStatement.builder(Vote.class)
            .setString(COLUMN_DISPLAY, Vote::getDisplay)
            .setString(COLUMN_DESCRIPTION, Vote::getDescription)
            .setLong(COLUMN_START_DATE, Vote::getStartDate)
            .setLong(COLUMN_END_DATE, Vote::getEndDate)
            .setString(COLUMN_STATUS, vote -> vote.getStatus().name())
            .setString(COLUMN_OUTCOME, Vote::getOutcome)
            .setLong(COLUMN_CONCLUDE_DATE, Vote::getConcludeDate)
            .setBoolean(COLUMN_RULE_MULTI_VOTE, vote -> vote.getRules().isMultiVote())
            .setInt(COLUMN_RULE_MAX_VOTES, vote -> vote.getRules().getMaxVotes())
            .setInt(COLUMN_RULE_MIN_PLAYTIME, vote -> vote.getRules().getMinPlaytimeHours())
            .setString(COLUMN_RULE_WORLD_MODE, vote -> vote.getRules().getWorldMode().name())
            .setString(COLUMN_RULE_WORLDS, vote -> encodeSet(vote.getRules().getWorlds()))
            .setString(COLUMN_RULE_MONEY_MODE, vote -> vote.getRules().getMoneyMode().name())
            .setDouble(COLUMN_RULE_MIN_BALANCE, vote -> vote.getRules().getMinBalance())
            .setDouble(COLUMN_RULE_MAX_BALANCE, vote -> vote.getRules().getMaxBalance())
            .setString(COLUMN_RULE_OVER_MAX, vote -> vote.getRules().getOverMaxMode().name())
            .setDouble(COLUMN_RULE_CURRENCY_W, vote -> vote.getRules().getCurrencyPerWeight())
            .setDouble(COLUMN_RULE_MAX_MONEY_W, vote -> vote.getRules().getMaxMoneyWeight())
            .setBoolean(COLUMN_RULE_WEIGHTED, vote -> vote.getRules().isWeightingEnabled())
            .setString(COLUMN_RULE_W_SOURCE, vote -> vote.getRules().getWeightSource().name())
            .setString(COLUMN_RULE_W_COMBINE, vote -> vote.getRules().getWeightCombine().name())
            .setDouble(COLUMN_RULE_BASE_W, vote -> vote.getRules().getBaseWeight())
            .setDouble(COLUMN_RULE_MAX_MULT, vote -> vote.getRules().getMaxMultiplier())
            .setDouble(COLUMN_RULE_MAX_WEIGHT, vote -> vote.getRules().getMaxWeight())
            .setInt(COLUMN_RULE_MIN_PART, vote -> vote.getRules().getMinParticipants())
            .setDouble(COLUMN_RULE_PASS_PCT, vote -> vote.getRules().getPassPercent())
            .setBoolean(COLUMN_RULE_NEUTRAL, vote -> vote.getRules().isNeutralCountsTowardTotal())
            .setBoolean(COLUMN_RULE_SECRET, vote -> vote.getRules().isSecretBallot())
            .setString(COLUMN_RULE_TIE_BREAK, vote -> vote.getRules().getTieBreak().name())
            .setBoolean(COLUMN_RULE_ALLOW_CHANGE, vote -> vote.getRules().isAllowChange())
            .setInt(COLUMN_RULE_CHANGE_LOCK, vote -> vote.getRules().getChangeLockSeconds())
            .setBoolean(COLUMN_RULE_RUNOFF, vote -> vote.getRules().isRunoffEnabled())
            .setInt(COLUMN_RULE_RUNOFF_TOP, vote -> vote.getRules().getRunoffTopN())
            .setInt(COLUMN_RULE_RUNOFF_DUR, vote -> vote.getRules().getRunoffDurationSeconds())
            .build();

    public static final InsertStatement<VotingDataManager.OptionRow> INSERT_OPTION =
            InsertStatement.builder(VotingDataManager.OptionRow.class)
                    .setUUID(COLUMN_OPTION_ID, VotingDataManager.OptionRow::optionId)
                    .setUUID(COLUMN_OPTION_VOTE, VotingDataManager.OptionRow::voteId)
                    .setString(COLUMN_OPTION_KEY, row -> row.option().getKey())
                    .setString(COLUMN_OPTION_NAME, row -> row.option().getDisplay())
                    .setString(COLUMN_OPTION_ICON, row -> row.option().getIcon())
                    .setString(COLUMN_OPTION_GATES, row -> encodeGates(row.option().getGates()))
                    .setDouble(COLUMN_OPTION_COST, row -> row.option().getCost())
                    .setString(COLUMN_OPTION_PERM, row -> row.option().getPermission())
                    .setString(COLUMN_OPTION_RANK, row -> row.option().getRank())
                    .setString(COLUMN_OPTION_WORLDS, row -> encodeSet(row.option().getWorlds()))
                    .setInt(COLUMN_OPTION_PLAYTIME, row -> row.option().getMinPlaytimeHours())
                    .build();

    public static final UpdateStatement<VotingDataManager.OptionRow> UPDATE_OPTION =
            UpdateStatement.builder(VotingDataManager.OptionRow.class)
                    .setString(COLUMN_OPTION_NAME, row -> row.option().getDisplay())
                    .setString(COLUMN_OPTION_ICON, row -> row.option().getIcon())
                    .setString(COLUMN_OPTION_GATES, row -> encodeGates(row.option().getGates()))
                    .setDouble(COLUMN_OPTION_COST, row -> row.option().getCost())
                    .setString(COLUMN_OPTION_PERM, row -> row.option().getPermission())
                    .setString(COLUMN_OPTION_RANK, row -> row.option().getRank())
                    .setString(COLUMN_OPTION_WORLDS, row -> encodeSet(row.option().getWorlds()))
                    .setInt(COLUMN_OPTION_PLAYTIME, row -> row.option().getMinPlaytimeHours())
                    .build();

    public static final InsertStatement<VoteBallot> INSERT_BALLOT = InsertStatement.builder(VoteBallot.class)
            .setUUID(COLUMN_BALLOT_ID,
                    ballot -> deriveBallotId(ballot.voteId(), ballot.playerId(), ballot.optionKey()))
            .setUUID(COLUMN_BALLOT_VOTE, VoteBallot::voteId)
            .setString(COLUMN_BALLOT_OPTION, VoteBallot::optionKey)
            .setUUID(COLUMN_BALLOT_PLAYER, VoteBallot::playerId)
            .setString(COLUMN_BALLOT_NAME, VoteBallot::playerName)
            .setDouble(COLUMN_BALLOT_WEIGHT, VoteBallot::weight)
            .setDouble(COLUMN_BALLOT_COST, VoteBallot::costPaid)
            .setLong(COLUMN_BALLOT_DATE, VoteBallot::date)
            .build();

    /**
     * Reads an enum column, or {@code null} when the stored name is one this build does not know.
     * <p>
     * The rule setters treat {@code null} as "use the default", so an unrecognised rule value
     * degrades one setting instead of losing the poll. The vote status is the exception and is
     * handled explicitly by the caller: guessing there could re-open a poll somebody closed.
     * <p>
     * A missing column (pre-migration database) also yields {@code null} rather than failing
     * the whole row, so old polls load with defaults until they are next saved.
     */
    private static <E extends Enum<E>> E readEnum(final ResultSet resultSet,
            final Column<String> column,
            final Class<E> type) {
        try {
            return Utils.enumValueOf(resultSet.getString(column.getName()), type);
        } catch (final SQLException | IllegalArgumentException exception) {
            return null;
        }
    }

    private static String readStringOr(final ResultSet resultSet, final Column<String> column,
            final String fallback) {
        try {
            final String value = resultSet.getString(column.getName());
            return value == null ? fallback : value;
        } catch (final SQLException exception) {
            return fallback;
        }
    }

    private static long readLongOr(final ResultSet resultSet, final Column<Long> column,
            final long fallback) {
        try {
            return resultSet.getLong(column.getName());
        } catch (final SQLException exception) {
            return fallback;
        }
    }

    private static int readIntOr(final ResultSet resultSet, final Column<Integer> column,
            final int fallback) {
        try {
            return resultSet.getInt(column.getName());
        } catch (final SQLException exception) {
            return fallback;
        }
    }

    private static boolean readBooleanOr(final ResultSet resultSet, final Column<Boolean> column,
            final boolean fallback) {
        try {
            final Object raw = resultSet.getObject(column.getName());
            if (raw instanceof final Boolean bool) {
                return bool;
            }
            if (raw instanceof final Number number) {
                return number.intValue() != 0;
            }
            if (raw instanceof final String text) {
                return text.equalsIgnoreCase("true") || text.equals("1");
            }
            return resultSet.getBoolean(column.getName());
        } catch (final SQLException | IllegalArgumentException exception) {
            return fallback;
        }
    }

    private VotingQueries() {
    }
}