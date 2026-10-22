package su.nightexpress.sunlight.api.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.moduleImpl.reports.model.Report;
import su.nightexpress.sunlight.moduleImpl.reports.model.ReportStatus;

/**
 * Fired when a report reaches a terminal state, whether a staff member concluded it or the
 * reported player was punished.
 * <p>
 * Extends {@link Event} rather than {@code PlayerEvent} because the reporter is frequently
 * offline by the time a report is worked.
 */
public class PlayerReportResolvedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Report report;
    private final ReportStatus status;
    private final boolean rewarded;
    private final boolean punishmentTriggered;

    public PlayerReportResolvedEvent(@NotNull Report report,
            @NotNull ReportStatus status,
            boolean rewarded,
            boolean punishmentTriggered
    ) {
        this.report = report;
        this.status = status;
        this.rewarded = rewarded;
        this.punishmentTriggered = punishmentTriggered;
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

    public @NotNull ReportStatus getStatus() {
        return this.status;
    }

    /** The reporter, or {@code null} when they are offline. */
    public @Nullable Player getReporter() {
        return Bukkit.getPlayer(this.report.getReporterId());
    }

    /**
     * Whether a reward payout was started for this report. The console commands themselves are
     * dispatched off-thread just after the event fires, so this is a promise to pay rather than
     * confirmation that the commands ran.
     */
    public boolean isRewarded() {
        return this.rewarded;
    }

    public boolean isPunishmentTriggered() {
        return this.punishmentTriggered;
    }
}
