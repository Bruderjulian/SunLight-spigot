package su.nightexpress.sunlight.api.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of the Reports module's queue. Obtained via
 * {@code SunLightPlugin#reportsProvider()}, which returns an empty
 * {@link java.util.Optional} when the module is disabled, so callers must treat this as a soft
 * dependency.
 * <p>
 * Every type here is a primitive or a JDK type on purpose: this interface lives in the API module,
 * which must not depend on Core, so the module's own {@code Report} model cannot be exposed.
 */
public interface ReportsProvider {

    /** Number of reports that are not yet concluded (OPEN or CLAIMED) across all players. */
    int getOpenReportCount();

    /** Number of not-yet-concluded reports filed by the given player. */
    int getOpenReportCount(@NotNull UUID playerId);

    /**
     * Number of not-yet-concluded reports against the given player, matched by UUID.
     * <p>
     * Reports filed against a player who had not yet joined carry no UUID, so this undercounts
     * them. Use {@link #getOpenReportCountAgainst(UUID, String)} when a name is at hand.
     */
    int getOpenReportCountAgainst(@NotNull UUID playerId);

    /**
     * Number of not-yet-concluded reports against a target identified by both UUID and name.
     * Either may be {@code null}; a name is matched case-insensitively.
     */
    int getOpenReportCountAgainst(@Nullable UUID playerId, @Nullable String targetName);

    /** Number of concluded (RESOLVED or DENIED, or EXPIRED) reports filed by the given player. */
    int getConcludedReportCount(@NotNull UUID playerId);

    /** Whether any not-yet-concluded report exists against the given player, matched by UUID. */
    boolean hasOpenReportAgainst(@NotNull UUID playerId);

    /**
     * Whether any not-yet-concluded report exists against a target identified by both UUID and
     * name. Either may be {@code null}.
     */
    boolean hasOpenReportAgainst(@Nullable UUID playerId, @Nullable String targetName);

    /**
     * Names of the players the given player currently has open reports against.
     * Never contains report details — this is a moderation signal, not evidence.
     */
    @NotNull List<String> getOpenReportTargets(@NotNull UUID playerId);

    /**
     * The player's total playtime in milliseconds, delegated to the Playtime module.
     * Returns {@code 0} when that module is disabled — reports must not require playtime to exist.
     */
    long getTotalPlaytimeMs(@NotNull UUID playerId);

    /** Whether the player has opted out of being able to file reports. */
    boolean isOptedOut(@NotNull UUID playerId);

    /**
     * A single report by id, or {@code null} when there is no such report or it has been purged.
     */
    @Nullable ReportHandle getReport(@Nullable UUID reportId);
}
