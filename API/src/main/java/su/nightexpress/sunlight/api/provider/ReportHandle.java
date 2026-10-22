package su.nightexpress.sunlight.api.provider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The subset of a report that is safe to hand to another plugin.
 * <p>
 * A record of primitives rather than the module's own model, because the API module must not
 * depend on Core. It deliberately omits the report text: a moderation signal about whether
 * somebody is being reported is fine to expose, the reason they gave is not something to leak
 * into an unrelated plugin's scoreboard.
 */
public record ReportHandle(@NotNull UUID reportId,
                            @NotNull UUID reporterId,
                            @NotNull String reporterName,
                            @Nullable UUID targetId,
                            @NotNull String targetName,
                            @NotNull String category,
                            @NotNull String status,
                            long createDate,
                            long updateDate,
                            boolean rewarded,
                            boolean pending,
                            @Nullable UUID caseId
) {
}
