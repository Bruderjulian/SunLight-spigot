package su.nightexpress.sunlight.moduleImpl.nametags.hook;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Team information contributed to a nameplate.
 *
 * @param id      the team's own identifier
 * @param display the team name, already resolved through the player's nickname context
 * @param prefix  team prefix text
 * @param suffix  team suffix text
 * @param color   team colour, a named colour or hex value
 */
public record TeamInfo(@NotNull String id,
        @NotNull String display,
        @NotNull String prefix,
        @NotNull String suffix,
        @NotNull String color
) {

    public TeamInfo {
        id = id == null ? "" : id;
        display = display == null ? "" : display;
        prefix = prefix == null ? "" : prefix;
        suffix = suffix == null ? "" : suffix;
        color = color == null ? "" : color;
    }

    public static final TeamInfo EMPTY = new TeamInfo("", "", "", "", "");

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
        return this.id.isEmpty() && !this.hasPrefix() && !this.hasSuffix() && !this.hasColor();
    }

    public static @Nullable TeamInfo of(@Nullable String id,
            @Nullable String display,
            @Nullable String prefix,
            @Nullable String suffix,
            @Nullable String color
    ) {
        TeamInfo info = new TeamInfo(nullToEmpty(id), nullToEmpty(display), nullToEmpty(prefix), nullToEmpty(suffix),
                nullToEmpty(color));
        return info.isEmpty() ? null : info;
    }

    private static @NotNull String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }
}
