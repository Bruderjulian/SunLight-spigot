package su.nightexpress.sunlight.moduleImpl.nametags.render;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One contributor to a nameplate (team, rank, tag or glow). Holds raw configured
 * text and a raw colour name; conversion to legacy codes happens in
 * {@link PrefixComposer} so composition stays a pure, testable function.
 */
public record Part(@NotNull String prefix, @NotNull String suffix, @NotNull String color) {

    public static final Part EMPTY = new Part("", "", "");

    public Part {
        prefix = prefix == null ? "" : prefix;
        suffix = suffix == null ? "" : suffix;
        color = color == null ? "" : color;
    }

    public static @NotNull Part of(@Nullable String prefix, @Nullable String suffix, @Nullable String color) {
        return new Part(nullToEmpty(prefix), nullToEmpty(suffix), nullToEmpty(color));
    }

    public boolean hasPrefix() {
        return !this.prefix.isEmpty();
    }

    public boolean hasSuffix() {
        return !this.suffix.isEmpty();
    }

    public boolean hasColor() {
        return !this.color.isBlank();
    }

    public boolean isEmpty() {
        return !this.hasPrefix() && !this.hasSuffix() && !this.hasColor();
    }

    private static @NotNull String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}
