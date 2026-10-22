package su.nightexpress.sunlight.moduleImpl.reports.data;

import su.nightexpress.sunlight.moduleImpl.reports.model.CaseStatus;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory index of cases. A case is looked up by its target, because that is the only question
 * the submission path ever asks: "is this player already being dealt with?"
 */
public class CaseRepository {

    private final Map<UUID, ReportCase> byId = new ConcurrentHashMap<>();
    private final Map<String, UUID> byTargetName = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> byTargetId = new ConcurrentHashMap<>();
    private final Map<UUID, List<Report>> reportsByCase = new ConcurrentHashMap<>();

    public synchronized void clear() {
        this.byId.clear();
        this.byTargetName.clear();
        this.byTargetId.clear();
        this.reportsByCase.clear();
    }

    public synchronized void upsertCase(ReportCase reportCase) {
        ReportCase existing = this.byId.get(reportCase.getId());
        if (existing != null) {
            this.unindex(existing);
        }
        this.byId.put(reportCase.getId(), reportCase);
        this.index(reportCase);
    }

    /**
     * A case whose status just changed has to move between the open-case lookup buckets, otherwise
     * a concluded case would keep absorbing new reports.
     */
    public synchronized void applyTransition(ReportCase before, ReportCase after) {
        this.unindex(before);
        this.byId.put(after.getId(), after);
        this.index(after);
    }

    public synchronized void removeCase(ReportCase reportCase) {
        if (!this.byId.containsKey(reportCase.getId()))
            return;

        this.unindex(reportCase);
        this.byId.remove(reportCase.getId());
        this.reportsByCase.remove(reportCase.getId());
    }

    private void index(ReportCase reportCase) {
        if (reportCase.isPending()) {
            this.byTargetName.put(key(reportCase.getTargetName()), reportCase.getId());
            if (reportCase.getTargetId() != null) {
                this.byTargetId.put(reportCase.getTargetId(), reportCase.getId());
            }
        }
    }

    private void unindex(ReportCase reportCase) {
        this.byTargetName.remove(key(reportCase.getTargetName()), reportCase.getId());
        if (reportCase.getTargetId() != null) {
            this.byTargetId.remove(reportCase.getTargetId(), reportCase.getId());
        }
    }

    public void setCaseReports(UUID caseId, List<Report> reports) {
        this.reportsByCase.put(caseId, new ArrayList<>(reports));
    }

    public List<Report> getCaseReports(UUID caseId) {
        List<Report> reports = this.reportsByCase.get(caseId);
        return reports == null ? List.of() : new ArrayList<>(reports);
    }

    public ReportCase getCase(UUID caseId) {
        return this.byId.get(caseId);
    }

    public boolean contains(UUID caseId) {
        return this.byId.containsKey(caseId);
    }

    public List<ReportCase> getCases() {
        return new ArrayList<>(this.byId.values());
    }

    public List<ReportCase> getCases(CaseStatus status) {
        List<ReportCase> cases = new ArrayList<>();
        for (ReportCase reportCase : this.byId.values()) {
            if (reportCase.getStatus() == status)
                cases.add(reportCase);
        }
        return cases;
    }

    public List<ReportCase> getPendingCases() {
        List<ReportCase> cases = new ArrayList<>();
        for (ReportCase reportCase : this.byId.values()) {
            if (reportCase.isPending())
                cases.add(reportCase);
        }
        return cases;
    }

    /**
     * The open case for a target, matched by UUID when known and by name otherwise. A report can
     * legitimately be filed against a player who never joined, so the name is not a fallback
     * convenience but a real identity.
     */
    public ReportCase getOpenCaseByTarget(UUID targetId, String targetName) {
        if (targetId != null) {
            ReportCase byUuid = this.byId.get(this.byTargetId.get(targetId));
            if (byUuid != null && byUuid.isPending())
                return byUuid;
        }
        if (targetName != null && !targetName.isBlank()) {
            return this.byId.get(this.byTargetName.get(key(targetName)));
        }
        return null;
    }

    private static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
