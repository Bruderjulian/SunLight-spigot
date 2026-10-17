package su.nightexpress.sunlight.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.reports.model.Report;

/**
 * Fired before a staff member gives up ownership of a report, before it is returned to the open
 * queue.
 * <p>
 * Cancelling keeps the report claimed by its current owner, which is how a plugin can pin a case
 * to the staff member handling it. Only fires when the report is actually claimed; releasing an
 * open report is rejected earlier and does not reach this point.
 */
public class ReportReleaseEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Report report;
    private final CommandSender staff;
    private boolean cancelled;

    public ReportReleaseEvent(@NotNull Report report, @NotNull CommandSender staff) {
        this.report = report;
        this.staff = staff;
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

    /** The staff member releasing ownership. */
    public @NotNull CommandSender getStaff() {
        return this.staff;
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