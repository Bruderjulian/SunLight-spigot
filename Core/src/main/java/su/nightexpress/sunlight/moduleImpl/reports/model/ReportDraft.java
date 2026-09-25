package su.nightexpress.sunlight.moduleImpl.reports.model;

/**
 * The three answers a guided report collects before anything is written to the database.
 * <p>
 * A draft is only ever handed to the final confirm screen, which is the single place that calls
 * {@code ReportsModule#submit}. The GUI is therefore a different door into the one validation path
 * rather than a second implementation of it.
 */
public record ReportDraft(String targetName, String categoryId, String details) {

    public ReportDraft withCategory(String categoryId) {
        return new ReportDraft(this.targetName, categoryId, this.details);
    }

    public ReportDraft withDetails(String details) {
        return new ReportDraft(this.targetName, this.categoryId, details);
    }
}
