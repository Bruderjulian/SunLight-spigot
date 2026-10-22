package su.nightexpress.sunlight.moduleImpl.reports.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ReportNote {

    private final UUID id;
    private final UUID reportId;
    private final @Nullable UUID authorId;
    private final String authorName;
    private final String text;
    private final long date;

    public ReportNote(UUID reportId, @Nullable UUID authorId, @NotNull String authorName, @NotNull String text) {
        this(UUID.randomUUID(), reportId, authorId, authorName, text, System.currentTimeMillis());
    }

    public ReportNote(UUID id, UUID reportId, @Nullable UUID authorId, @NotNull String authorName,
            @NotNull String text, long date) {
        this.id = id;
        this.reportId = reportId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.text = text;
        this.date = date;
    }

    /** A note the server itself wrote, e.g. "closed because the target was banned". */
    public static ReportNote system(@NotNull UUID reportId, @NotNull String text) {
        return new ReportNote(reportId, null, "CONSOLE", text);
    }

    public UUID getId() {
        return this.id;
    }

    public UUID getReportId() {
        return this.reportId;
    }

    public @Nullable UUID getAuthorId() {
        return this.authorId;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public boolean isSystem() {
        return this.authorId == null;
    }

    public String getText() {
        return this.text;
    }

    public long getDate() {
        return this.date;
    }
}
