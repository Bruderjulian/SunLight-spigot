package su.nightexpress.sunlight.moduleImpl.reports.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A case is the staff-facing unit of work; a report is one piece of evidence inside it.
 * <p>
 * They carry independent status on purpose, because they answer different questions. A report
 * records one submission; a case records one offender's wider situation. Five reports about the
 * same griefer are one thing to work, but any single one of them can still be bogus on its own, so
 * a report must stay individually deniable while its case stays open.
 */
public class ReportCase {

    private final UUID id;

    private final UUID targetId;
    private final String targetName;

    private CaseStatus status;
    private UUID staffId;
    private String staffName;
    private String outcomeReason;

    private final long createDate;
    private long updateDate;

    private int reportCount;

    public ReportCase(UUID id,
            @Nullable UUID targetId,
            @NotNull String targetName,
            CaseStatus status,
            @Nullable UUID staffId,
            @Nullable String staffName,
            @Nullable String outcomeReason,
            long createDate,
            long updateDate,
            int reportCount
    ) {
        this.id = id;
        this.targetId = targetId;
        this.targetName = targetName;
        this.status = status;
        this.staffId = staffId;
        this.staffName = staffName;
        this.outcomeReason = outcomeReason;
        this.createDate = createDate;
        this.updateDate = updateDate;
        this.reportCount = reportCount;
    }

    public static ReportCase create(@Nullable UUID targetId, @NotNull String targetName) {
        long now = System.currentTimeMillis();
        return new ReportCase(UUID.randomUUID(), targetId, targetName, CaseStatus.OPEN, null, null, null, now, now, 0);
    }

    public ReportCase copy() {
        return new ReportCase(this.id, this.targetId, this.targetName, this.status, this.staffId, this.staffName,
                this.outcomeReason, this.createDate, this.updateDate, this.reportCount);
    }

    public UUID getId() {
        return this.id;
    }

    public @Nullable UUID getTargetId() {
        return this.targetId;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public CaseStatus getStatus() {
        return this.status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }

    public boolean isPending() {
        return this.status.isPending();
    }

    public boolean isTerminal() {
        return this.status.isTerminal();
    }

    public @Nullable UUID getStaffId() {
        return this.staffId;
    }

    public @Nullable String getStaffName() {
        return this.staffName;
    }

    public void setStaff(@Nullable UUID staffId, @Nullable String staffName) {
        this.staffId = staffId;
        this.staffName = staffName;
    }

    public @Nullable String getOutcomeReason() {
        return this.outcomeReason;
    }

    public void setOutcomeReason(@Nullable String outcomeReason) {
        this.outcomeReason = outcomeReason;
    }

    public long getCreateDate() {
        return this.createDate;
    }

    public long getUpdateDate() {
        return this.updateDate;
    }

    public void setUpdateDate(long updateDate) {
        this.updateDate = updateDate;
    }

    public int getReportCount() {
        return this.reportCount;
    }

    public void setReportCount(int reportCount) {
        this.reportCount = reportCount;
    }
}
