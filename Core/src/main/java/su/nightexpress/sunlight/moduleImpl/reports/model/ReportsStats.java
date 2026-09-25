package su.nightexpress.sunlight.moduleImpl.reports.model;

import java.util.List;
import java.util.UUID;

/**
 * A snapshot of the report queue, derived from the in-memory repository on demand.
 * <p>
 * Deliberately not persisted: it is all recomputable from the reports table, so a table of
 * statistics would only ever be able to disagree with the truth.
 */
public record ReportsStats(int open,
                           int claimed,
                           int resolved,
                           int denied,
                           int expired,
                           int concluded,
                           double resolutionRate,
                           double falsePositiveRate,
                           long medianTimeToClaim,
                           long medianTimeToResolve,
                           int reportersOptedOut,
                           List<TopEntry> topTargets,
                           List<TopEntry> topReporters,
                           List<StaffEntry> staff
) {

    public record TopEntry(UUID playerId, int count) {
    }

    public record StaffEntry(UUID staffId, String staffName, int resolved, int denied, double falsePositiveRate) {
    }
}
