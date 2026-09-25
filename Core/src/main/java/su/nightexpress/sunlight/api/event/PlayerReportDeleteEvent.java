package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportNote;

import java.util.List;

/**
 * Fired after a report and its whole note trail have been deleted, for audit-log plugins.
 * <p>
 * Deletion is a purge action, not a moderation action: a wrongly concluded report should be
 * denied or resolved differently, not removed. This event is the only trace that it happened.
 */
public class PlayerReportDeleteEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Report report;
    private final List<ReportNote> notes;

    public PlayerReportDeleteEvent(@NotNull Report report, @NotNull List<ReportNote> notes) {
        this.report = report;
        this.notes = List.copyOf(notes);
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull Report getReport() {
        return this.report;
    }

    public @NotNull List<ReportNote> getNotes() {
        return this.notes;
    }
}
