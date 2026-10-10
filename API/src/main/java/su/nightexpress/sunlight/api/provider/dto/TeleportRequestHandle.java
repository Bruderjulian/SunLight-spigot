package su.nightexpress.sunlight.api.provider.dto;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A pending PTP request or invite, as handed out by {@link su.nightexpress.sunlight.api.provider.PtpProvider}.
 * <p>
 * A record of JDK and API-declared types rather than the module's own request class, because the
 * API module must not depend on Core.
 *
 * @param senderId the player who sent the request
 * @param targetId the player who is expected to act on it
 * @param mode     whether accepting teleports the target to the sender or the other way round
 * @param expireDate epoch milliseconds after which the request stops being actionable
 * @param expired whether that moment has already passed
 */
public record TeleportRequestHandle(@NotNull UUID senderId,
                                    @NotNull UUID targetId,
                                    @NotNull PtpMode mode,
                                    long expireDate,
                                    boolean expired
) {
}