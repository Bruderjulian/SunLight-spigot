package su.nightexpress.sunlight.moduleImpl.nametags.render;

import org.jetbrains.annotations.NotNull;

/**
 * The composed, render-ready result for one player.
 * <p>
 * {@code color} is the colour that actually landed on the name, which is not necessarily the
 * glow colour: glow has to win, but a rank or tag colour can win when the player has no glow.
 * {@code glow} therefore carries the active glow colour separately, so a placeholder can
 * report it without re-deriving it.
 *
 * @param prefix team + rank + tag text, with the name colour appended last
 * @param suffix rank + tag + team text
 * @param color  legacy colour codes of the winning name colour, may be empty
 * @param glow   the player's active glow colour name, may be empty
 */
public record Nameplate(@NotNull String prefix, @NotNull String suffix, @NotNull String color,
        @NotNull String glow
) {

    public Nameplate {
        prefix = prefix == null ? "" : prefix;
        suffix = suffix == null ? "" : suffix;
        color = color == null ? "" : color;
        glow = glow == null ? "" : glow;
    }

    public Nameplate(@NotNull String prefix, @NotNull String suffix, @NotNull String color) {
        this(prefix, suffix, color, "");
    }

    public static final Nameplate EMPTY = new Nameplate("", "", "");

    public boolean hasPrefix() {
        return !this.prefix.isEmpty();
    }

    public boolean hasSuffix() {
        return !this.suffix.isEmpty();
    }

    public boolean hasColor() {
        return !this.color.isEmpty();
    }

    public boolean hasGlow() {
        return !this.glow.isEmpty();
    }

    public boolean isEmpty() {
        return !this.hasPrefix() && !this.hasSuffix() && !this.hasColor();
    }
}
