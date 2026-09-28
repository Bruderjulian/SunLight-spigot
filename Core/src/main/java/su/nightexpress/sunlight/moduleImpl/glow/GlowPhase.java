package su.nightexpress.sunlight.moduleImpl.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One step of a phased glow effect: a single vanilla color shown for a fixed
 * amount of ticks before hard-switching to the next phase (looping).
 */
public record GlowPhase(@NotNull NamedTextColor color, long durationTicks) {

    public static final long MIN_DURATION = 1L;
    public static final long MAX_DURATION = 1200L;
    public static final long DEFAULT_DURATION = 20L;

    public GlowPhase {
        if (color == null) throw new IllegalArgumentException("color must not be null");
        durationTicks = clampDuration(durationTicks);
    }

    public static long clampDuration(final long ticks) {
        return Math.clamp(ticks, MIN_DURATION, MAX_DURATION);
    }

    /**
     * Parses a single phase token, e.g. {@code RED}, {@code RED@40} or {@code red:40}.
     *
     * @return null when the color part is unknown or the token is blank.
     */
    public static @Nullable GlowPhase parse(final @Nullable String raw) {
        return parse(raw, DEFAULT_DURATION);
    }

    public static @Nullable GlowPhase parse(final @Nullable String raw, final long defaultDuration) {
        if (raw == null || raw.isBlank()) return null;
        String token = raw.trim();
        String colorPart = token;
        long duration = defaultDuration;

        int sep = Math.max(token.lastIndexOf('@'), token.lastIndexOf(':'));
        if (sep >= 0) {
            colorPart = token.substring(0, sep).trim();
            String durationPart = token.substring(sep + 1).trim();
            if (!durationPart.isEmpty()) {
                try {
                    duration = Long.parseLong(durationPart);
                } catch (final NumberFormatException exception) {
                    return null;
                }
            }
        }

        final NamedTextColor color = GlowEffect.parseColor(colorPart);
        if (color == null) return null;
        return new GlowPhase(color, duration);
    }

    public @NotNull String encode() {
        return this.color.toString() + "@" + this.durationTicks;
    }

    @Override
    public @NotNull String toString() {
        return this.encode();
    }
}
