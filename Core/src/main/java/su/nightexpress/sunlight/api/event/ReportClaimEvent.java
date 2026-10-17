package su.nightexpress.sunlight.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import su.nightexpress.sunlight.moduleImpl.reports.model.Report;

/**
 * Fired before a staff member takes ownership of a report, after the report has been validated as
 * claimable but before it is marked claimed.
 * <p>
 * Cancelling leaves the report open and unassigned. This is the hook for staff-tracking plugins that
 * want to reject a claim — for example when the case is already handled elsewhere, or when a shift
 * is over.
 * <p>
 * Re-claiming a report you already own is a no-op and does not fire this event; it only fires on an
 * actual transfer of ownership.
 */
public class ReportClaimEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Report report;
    private final CommandSender staff;
    private boolean cancelled;

    public ReportClaimEvent(@NotNull Report report, @NotNull CommandSender staff) {
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

    /** The staff member taking ownership. Not necessarily a player — the console may claim. */
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