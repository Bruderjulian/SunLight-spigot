package su.nightexpress.sunlight.api.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A snapshot of one poll, handed out by {@link VotingProvider}.
 * <p>
 * Deliberately flat and free of Core types, so that other plugins can read a poll's shape without
 * the Voting module on their classpath being loadable. The option list is a plain list of
 * {@link Option}, ordered as the poll defines it.
 */
public record VoteHandle(@NotNull UUID voteId,
                         @NotNull String voteKey,
                         @NotNull String display,
                         @NotNull String description,
                         @Nullable UUID creatorId,
                         @Nullable String creatorName,
                         long createDate,
                         long startDate,
                         long endDate,
                         @NotNull String status,
                         boolean open,
                         boolean multiVote,
                         int maxVotes,
                         boolean secret,
                         @NotNull String tieBreak,
                         @NotNull List<Option> options,
                         @Nullable String outcome,
                         long concludeDate) {

    public record Option(@NotNull String optionKey,
                         @NotNull String display,
                         boolean builtIn,
                         boolean gated,
                         double cost) {
    }
}
