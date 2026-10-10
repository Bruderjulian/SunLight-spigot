package su.nightexpress.sunlight.api.provider.dto;

import org.jetbrains.annotations.NotNull;

/**
 * The outcome of a player's most recent random teleport, as handed out by
 * {@link su.nightexpress.sunlight.api.provider.RtpProvider}.
 * <p>
 * Coordinates only, never prebuilt {@code Location}s: the engine records the origin before the
 * destination chunk is necessarily loaded, and either world may be unloaded by the time this is read.
 *
 * @param originWorld      the world the player was thrown out of
 * @param originX          block x coordinate of the origin
 * @param originZ          block z coordinate of the origin
 * @param destinationWorld the world the player landed in
 * @param destinationX     block x coordinate of the destination
 * @param destinationZ     block z coordinate of the destination
 * @param distance         horizontal block distance covered, XZ only
 */
public record LastRtpHandle(@NotNull String originWorld,
                            double originX,
                            double originZ,
                            @NotNull String destinationWorld,
                            double destinationX,
                            double destinationZ,
                            double distance
) {
}