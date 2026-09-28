package su.nightexpress.sunlight.moduleImpl.glow;

import org.bukkit.Material;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * One step of a phased glow effect: a single vanilla color shown for a fixed
 * amount of ticks before hard-switching to the next phase (looping).
 */
public record GlowPhase(NamedTextColor color, long durationTicks) {

    public static final long MIN_DURATION = 1L;
    public static final long MAX_DURATION = 1200L;
    public static final long DEFAULT_DURATION = 20L;

    public GlowPhase {
        if (color == null) {
            throw new IllegalArgumentException("color must not be null");
        }
        durationTicks = Math.clamp(durationTicks, MIN_DURATION, MAX_DURATION);
    }

    /**
     * Parses a single phase token, e.g. {@code RED}, {@code RED@40} or
     * {@code red:40}.
     *
     * @return null when the color part is unknown or the token is blank.
     */
    public static GlowPhase parse(final String raw) {
        return parse(raw, DEFAULT_DURATION);
    }

    public static GlowPhase parse(String token, long duration) {
        if (token == null || token.isBlank()) {
            return null;
        }
        token = token.trim();
        String colorPart = token;

        final int sep = Math.max(token.lastIndexOf('@'), token.lastIndexOf(':'));
        if (sep >= 0) {
            colorPart = token.substring(0, sep).trim();
            final String durationPart = token.substring(sep + 1).trim();
            if (!durationPart.isEmpty()) {
                try {
                    duration = Long.parseLong(durationPart);
                } catch (final NumberFormatException exception) {
                    return null;
                }
            }
        }

        final NamedTextColor color = GlowEffect.parseColor(colorPart);
        if (color == null) {
            return null;
        }
        return new GlowPhase(color, duration);
    }

    public String encode() {
        return this.color.toString() + "@" + this.durationTicks;
    }

    public Material material() {
        if (color.equals(NamedTextColor.WHITE))
            return Material.WHITE_WOOL;
        if (color.equals(NamedTextColor.GRAY))
            return Material.GRAY_WOOL;
        if (color.equals(NamedTextColor.DARK_GRAY))
            return Material.GRAY_CONCRETE;
        if (color.equals(NamedTextColor.BLACK))
            return Material.BLACK_WOOL;
        if (color.equals(NamedTextColor.RED))
            return Material.RED_WOOL;
        if (color.equals(NamedTextColor.DARK_RED))
            return Material.RED_CONCRETE;
        if (color.equals(NamedTextColor.GOLD))
            return Material.ORANGE_WOOL;
        if (color.equals(NamedTextColor.YELLOW))
            return Material.YELLOW_WOOL;
        if (color.equals(NamedTextColor.GREEN))
            return Material.LIME_WOOL;
        if (color.equals(NamedTextColor.DARK_GREEN))
            return Material.GREEN_WOOL;
        if (color.equals(NamedTextColor.AQUA))
            return Material.LIGHT_BLUE_WOOL;
        if (color.equals(NamedTextColor.DARK_AQUA))
            return Material.CYAN_WOOL;
        if (color.equals(NamedTextColor.BLUE))
            return Material.BLUE_WOOL;
        if (color.equals(NamedTextColor.DARK_BLUE))
            return Material.BLUE_CONCRETE;
        if (color.equals(NamedTextColor.LIGHT_PURPLE))
            return Material.PINK_WOOL;
        if (color.equals(NamedTextColor.DARK_PURPLE))
            return Material.PURPLE_WOOL;
        return null;
    }

    @Override
    public String toString() {
        return this.color.toString() + "@" + this.durationTicks;
    }
}
