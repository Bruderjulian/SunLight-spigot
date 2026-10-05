package su.nightexpress.sunlight.api.provider;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of the Playtime module's tracking data. Obtained via
 * {@code SunLightPlugin#playtimeProvider()}, which returns an empty
 * {@link java.util.Optional} when the module is disabled, so callers must treat this as a soft
 * dependency.
 * <p>
 * Every type here is a primitive or a JDK type on purpose: this interface lives in the API module,
 * which must not depend on Core, so the module's own {@code PlaytimePeriod} model cannot be exposed.
 * Periods are passed as plain names: {@code alltime}, {@code year}, {@code month}, {@code week},
 * {@code day}. Unknown names resolve to {@code alltime}.
 */
public interface PlaytimeProvider {

    /** Playtime in milliseconds for the given period. Never negative. */
    long getPlaytimeMs(@NotNull UUID playerId, @NotNull String period);

    /** Total (alltime) playtime in milliseconds. Never negative. */
    long getTotalPlaytimeMs(@NotNull UUID playerId);

    /** Playtime in milliseconds accumulated in the current session. Never negative. */
    long getSessionPlaytimeMs(@NotNull UUID playerId);

    /** Consecutive-day streak. Never negative. */
    int getDailyStreak(@NotNull UUID playerId);

    /** Consecutive-week streak. Never negative. */
    int getWeeklyStreak(@NotNull UUID playerId);

    /** Effective goal in milliseconds for {@code day}, {@code week} or {@code month}. */
    long getGoalMs(@NotNull UUID playerId, @NotNull String scope);

    /** Top entries for the given period, up to {@code limit}, sorted best-first. */
    @NotNull List<TopEntry> getTop(@NotNull String period, int limit);

    /** A single leaderboard row. JDK types only, see {@link PlaytimeProvider}. */
    record TopEntry(@NotNull UUID playerId, @NotNull String playerName, long playtimeMs) {
    }
}
