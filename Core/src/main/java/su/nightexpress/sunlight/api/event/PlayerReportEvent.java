package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;

import java.util.UUID;

/**
 * Fired after a report has been written and added to the repository.
 * <p>
 * Cancellable so that a moderation plugin can veto a report outright. Because the row is already
 * inserted at this point, cancelling rolls it straight back — see
 * {@code ReportsModule#submit}. Nothing can observe the report in between, so the rollback cannot
 * lose a state anyone else depends on.
 */
public class PlayerReportEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Report report;

    private boolean cancelled;

    public PlayerReportEvent(@NotNull Report report) {
        this.report = report;
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

    public @NotNull UUID getReporterId() {
        return this.report.getReporterId();
    }

    public @NotNull String getReporterName() {
        return this.report.getReporterName();
    }

    public @Nullable UUID getTargetId() {
        return this.report.getTargetId();
    }

    public @NotNull String getTargetName() {
        return this.report.getTargetName();
    }

    public @NotNull String getCategoryId() {
        return this.report.getCategoryId();
    }

    public @NotNull String getDetails() {
        return this.report.getDetails();
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
