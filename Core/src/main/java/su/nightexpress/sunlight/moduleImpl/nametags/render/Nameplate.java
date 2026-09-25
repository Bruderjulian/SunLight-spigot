package su.nightexpress.sunlight.moduleImpl.nametags.render;

import org.jetbrains.annotations.NotNull;

/**
 * The composed, render-ready result for one player.
 *
 * @param prefix team + rank + tag text, with the name colour appended last
 * @param suffix rank + tag + team text
 * @param color  legacy colour codes of the winning name colour, may be empty
 */
public record Nameplate(@NotNull String prefix, @NotNull String suffix, @NotNull String color) {

    public Nameplate {
        prefix = prefix == null ? "" : prefix;
        suffix = suffix == null ? "" : suffix;
        color = color == null ? "" : color;
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

    public boolean isEmpty() {
        return !this.hasPrefix() && !this.hasSuffix() && !this.hasColor();
    }
}
