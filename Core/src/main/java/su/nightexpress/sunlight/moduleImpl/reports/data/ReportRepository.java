package su.nightexpress.sunlight.moduleImpl.reports.data;

import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportNote;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory index of reports and notes.
 * <p>
 * Every mutator is synchronized because {@code addTableSync} delivers rows from the database sync
 * thread while the main thread reads and writes. This mirrors {@code PunishmentRepository} in the
 * Bans module, which is the closest existing analogue.
 * <p>
 * There is no ORDER BY anywhere in the query API, so every accessor hands back an unordered
 * collection and callers sort. That is the reason the whole table is loaded eagerly at boot
 * rather than queried per menu page.
 */
public class ReportRepository {

    private final Map<UUID, Report> byId = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> byReporter = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> byTargetId = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> byTargetName = new ConcurrentHashMap<>();
    private final Map<ReportStatus, Set<UUID>> byStatus = new ConcurrentHashMap<>();
    private final Map<UUID, List<ReportNote>> notesByReport = new ConcurrentHashMap<>();

    public synchronized void clear() {
        this.byId.clear();
        this.byReporter.clear();
        this.byTargetId.clear();
        this.byTargetName.clear();
        this.byStatus.clear();
        this.notesByReport.clear();
    }

    /**
     * Inserts a report, or re-indexes it if it is already tracked. Used by the boot load and by
     * table sync, where the incoming object is an exact stand-in for the tracked one.
     */
    public synchronized void upsertReport(Report report) {
        Report existing = this.byId.get(report.getId());
        if (existing != null) {
            this.unindex(existing);
        }
        this.byId.put(report.getId(), report);
        this.index(report);
    }

    /**
     * Applies an in-place state change. Status and staff are part of the indexes, so the old keys
     * have to be dropped using the pre-mutation snapshot — the live object already carries the new
     * values and unindexing it would leave the old bucket behind. Notes are deliberately kept.
     */
    public synchronized void applyTransition(Report before, Report after) {
        this.unindex(before);
        this.byId.put(after.getId(), after);
        this.index(after);
    }

    /** Hard removal, used when a report is actually deleted. Drops its notes as well. */
    public synchronized void removeReport(Report report) {
        if (!this.byId.containsKey(report.getId()))
            return;

        this.unindex(report);
        this.byId.remove(report.getId());
        this.notesByReport.remove(report.getId());
    }

    private void index(Report report) {
        this.byReporter.computeIfAbsent(report.getReporterId(), key -> new HashSet<>()).add(report.getId());
        this.byStatus.computeIfAbsent(report.getStatus(), key -> new HashSet<>()).add(report.getId());

        if (report.getTargetId() != null) {
            this.byTargetId.computeIfAbsent(report.getTargetId(), key -> new HashSet<>()).add(report.getId());
        }
        this.byTargetName.computeIfAbsent(key(report.getTargetName()), k -> new HashSet<>()).add(report.getId());
    }

    private void unindex(Report report) {
        discard(this.byReporter, report.getReporterId(), report.getId());
        discard(this.byStatus, report.getStatus(), report.getId());
        discard(this.byTargetName, key(report.getTargetName()), report.getId());

        if (report.getTargetId() != null) {
            discard(this.byTargetId, report.getTargetId(), report.getId());
        }
    }

    private static <K> void discard(Map<K, Set<UUID>> map, K key, UUID reportId) {
        Set<UUID> ids = map.get(key);
        if (ids == null)
            return;

        ids.remove(reportId);
        if (ids.isEmpty()) {
            map.remove(key, ids);
        }
    }

    public synchronized void addNote(ReportNote note) {
        List<ReportNote> notes = this.notesByReport.computeIfAbsent(note.getReportId(),
                key -> Collections.synchronizedList(new ArrayList<>()));
        notes.removeIf(existing -> existing.getId().equals(note.getId()));
        notes.add(note);

        Report report = this.byId.get(note.getReportId());
        if (report != null) {
            report.setNoteCount(notes.size());
        }
    }

    public Report getReport(UUID reportId) {
        return this.byId.get(reportId);
    }

    public boolean contains(UUID reportId) {
        return this.byId.containsKey(reportId);
    }

    public List<Report> getReports() {
        return new ArrayList<>(this.byId.values());
    }

    public List<Report> getReports(ReportStatus status) {
        Set<UUID> ids = this.byStatus.get(status);
        if (ids == null)
            return List.of();

        return this.resolve(ids);
    }

    public List<Report> getPendingReports() {
        List<Report> reports = new ArrayList<>();
        reports.addAll(this.getReports(ReportStatus.OPEN));
        reports.addAll(this.getReports(ReportStatus.CLAIMED));
        return reports;
    }

    public List<Report> getReportsByReporter(UUID reporterId) {
        return this.resolve(this.byReporter.get(reporterId));
    }

    public List<Report> getPendingReportsByReporter(UUID reporterId) {
        List<Report> reports = new ArrayList<>();
        this.getReportsByReporter(reporterId).stream().filter(Report::isPending).forEach(reports::add);
        return reports;
    }

    /**
     * By target UUID. Only matches reports whose target was a known player at filing time.
     */
    public List<Report> getReportsByTargetId(UUID targetId) {
        return this.resolve(this.byTargetId.get(targetId));
    }

    /**
     * By target name. This is the only lookup that finds reports against a player who had never
     * joined, since those have no target UUID to match on.
     */
    public List<Report> getReportsByTargetName(String targetName) {
        if (targetName == null || targetName.isBlank())
            return List.of();

        return this.resolve(this.byTargetName.get(key(targetName)));
    }

    public List<Report> getPendingReportsByTargetId(UUID targetId) {
        List<Report> reports = new ArrayList<>();
        this.getReportsByTargetId(targetId).stream().filter(Report::isPending).forEach(reports::add);
        return reports;
    }

    public List<Report> getPendingReportsByTargetName(String targetName) {
        List<Report> reports = new ArrayList<>();
        this.getReportsByTargetName(targetName).stream().filter(Report::isPending).forEach(reports::add);
        return reports;
    }

    public int getCount(ReportStatus status) {
        Set<UUID> ids = this.byStatus.get(status);
        return ids == null ? 0 : ids.size();
    }

    public int getPendingCount() {
        return this.getCount(ReportStatus.OPEN) + this.getCount(ReportStatus.CLAIMED);
    }

    public List<ReportNote> getNotes(UUID reportId) {
        List<ReportNote> notes = this.notesByReport.get(reportId);
        if (notes == null)
            return List.of();

        List<ReportNote> copy = new ArrayList<>(notes);
        copy.sort(Comparator.comparingLong(ReportNote::getDate));
        return copy;
    }

    private List<Report> resolve(Set<UUID> ids) {
        if (ids == null)
            return List.of();

        List<Report> reports = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            Report report = this.byId.get(id);
            if (report != null)
                reports.add(report);
        }
        return reports;
    }

    private static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
