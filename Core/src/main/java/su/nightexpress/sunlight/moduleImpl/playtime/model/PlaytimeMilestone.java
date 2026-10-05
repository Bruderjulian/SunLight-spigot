package su.nightexpress.sunlight.moduleImpl.playtime.model;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A total-playtime milestone from {@code Playtime.Milestones.<id>}.
 * <p>
 * One-shot milestones pay once when total playtime reaches the threshold.
 * Repeatable milestones pay once per multiple (every {@code minutes} of total playtime).
 */
public record PlaytimeMilestone(@NotNull String id, @NotNull String name, long thresholdMs, boolean repeatable,
        @NotNull List<String> rewards) {
}
