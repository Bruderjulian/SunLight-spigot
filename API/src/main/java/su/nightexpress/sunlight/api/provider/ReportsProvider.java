package su.nightexpress.sunlight.api.provider;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of the Reports module's queue. Obtained via
 * {@code SunLightPlugin#getReportsProvider()}, which returns an empty
 * {@link java.util.Optional} when the module is disabled, so callers must treat
 * this as a soft dependency.
 */
public interface ReportsProvider {

    /** Number of reports that are not yet concluded (OPEN or CLAIMED) across all players. */
    int getOpenReportCount();

    /** Number of not-yet-concluded reports filed by the given player. */
    int getOpenReportCount(@NotNull UUID playerId);

    /** Number of concluded (RESOLVED or DENIED) reports filed by the given player. */
    int getConcludedReportCount(@NotNull UUID playerId);

    /** Whether any not-yet-concluded report exists against the given player. */
    boolean hasOpenReportAgainst(@NotNull UUID playerId);

    /**
     * Names of the players the given player currently has open reports against.
     * Never contains report details — this is a moderation signal, not evidence.
     */
    @NotNull List<String> getOpenReportTargets(@NotNull UUID playerId);

    /**
     * The player's total playtime in milliseconds, delegated to the Playtime module.
     * Returns {@code 0} when that module is disabled — reports must not require
     * playtime to exist.
     */
    long getTotalPlaytimeMs(@NotNull UUID playerId);
}
