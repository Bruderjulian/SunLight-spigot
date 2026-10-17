package su.nightexpress.sunlight.api.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.moduleImpl.playtime.model.PlaytimePeriod;
import su.nightexpress.sunlight.user.SunUser;

/**
 * Fired before a playtime counter is changed by an administrative command.
 * <p>
 * This covers only the explicit {@code /playtime add}, {@code set} and {@code reset} paths — the
 * passive per-tick accumulation that happens while a player is online is deliberately not reported,
 * so this event is safe to hook without being flooded.
 * <p>
 * Cancelling leaves the affected counters untouched. Note that a single call may touch several
 * counters: granting against a period always moves that period and the all-time total together.
 */
public class PlaytimeAdjustedEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    /** Whether the counter was granted extra time or set to an absolute value. */
    public enum Operation {
        /** Time was added on top of the current value. */
        ADD,
        /** The counter was overwritten with an absolute value. */
        SET,
        /** The counter was zeroed. */
        RESET
    }

    private final SunUser user;
    private final Operation operation;
    private final PlaytimePeriod period;
    private final String scope;
    private final long previousValue;
    private final long newValue;
    private boolean cancelled;

    /**
     * @param user          whose counters are about to change
     * @param operation     what kind of change this is
     * @param period        the period being changed, or {@code null} for {@link Operation#RESET}
     * @param scope         the raw reset scope, or {@code null} when not a reset
     * @param previousValue the counter's value before the change, in milliseconds
     * @param newValue      the value it will hold afterwards, in milliseconds
     */
    public PlaytimeAdjustedEvent(@NotNull SunUser user,
            @NotNull Operation operation,
            @Nullable PlaytimePeriod period,
            @Nullable String scope,
            long previousValue,
            long newValue
    ) {
        this.user = user;
        this.operation = operation;
        this.period = period;
        this.scope = scope;
        this.previousValue = previousValue;
        this.newValue = newValue;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public @NotNull SunUser getUser() {
        return this.user;
    }

    public @NotNull Operation getOperation() {
        return this.operation;
    }

    /** The affected period, or {@code null} when {@link Operation#RESET} touched a named scope. */
    public @Nullable PlaytimePeriod getPeriod() {
        return this.period;
    }

    /** The raw reset scope, or {@code null} when this is not a {@link Operation#RESET}. */
    public @Nullable String getScope() {
        return this.scope;
    }

    public long getPreviousValue() {
        return this.previousValue;
    }

    public long getNewValue() {
        return this.newValue;
    }

    /** Signed difference between {@link #getNewValue()} and {@link #getPreviousValue()}. */
    public long getDelta() {
        return this.newValue - this.previousValue;
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