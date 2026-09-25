package su.nightexpress.sunlight.utils;

import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts configured colour names into legacy section-sign codes.
 * <p>
 * Needed because TAB's {@code NameTagManager#setPrefix}/{@code setSuffix} API takes plain
 * strings, and the client carries the last colour code of a team prefix over into the
 * player name. That trailing code is what the glow effect appends.
 */
public final class LegacyText {

    private static final char SECTION = '§';

    private static final Pattern SIMPLE_PATTERN = Pattern.compile("(?i)" + SECTION + "[0-9a-fk-or]");
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)" + SECTION + "x(?:(?:" + SECTION + "[0-9a-f]){6})");
    private static final Pattern HEX_INPUT = Pattern.compile("(?i)^#?([0-9a-f]{6})$");
    private static final Pattern HEX_INPUT_0X = Pattern.compile("(?i)^0x([0-9a-f]{6})$");

    /** The sixteen vanilla colours, by their Minecraft hex value. */
    private static final Map<String, String> BY_HEX = new HashMap<>();
    private static final Map<String, String> BY_NAME = new HashMap<>();

    static {
        String codes = "0123456789abcdef";
        String[] hexes = {
                "000000", "0000aa", "00aa00", "00aaaa", "aa0000", "aa00aa", "ffaa00", "aaaaaa",
                "555555", "5555ff", "55ff55", "55ffff", "ff5555", "ff55ff", "ffff55", "ffffff"
        };
        for (int i = 0; i < hexes.length; i++) {
            BY_HEX.put(hexes[i], SECTION + String.valueOf(codes.charAt(i)));
        }

        // NAMES is keyed by the colour name, so iterating keys gives us "gold", "red", ...
        for (String name : NamedTextColor.NAMES.keys()) {
            NamedTextColor color = NamedTextColor.NAMES.value(name);
            if (color == null) continue;

            String legacy = BY_HEX.get(stripHash(color.asHexString()));
            if (legacy != null) {
                BY_NAME.put(name.toLowerCase(Locale.ROOT), legacy);
            }
        }
    }

    private static @NotNull String stripHash(@NotNull String hex) {
        String lower = hex.toLowerCase(Locale.ROOT);
        return lower.charAt(0) == '#' ? lower.substring(1) : lower;
    }

    private LegacyText() {
    }

    /**
     * Resolves a colour name or hex value to a legacy code sequence.
     * <p>
     * Values that already contain section codes (raw {@code §a} from a config or another
     * plugin) are passed through untouched, so callers can hand us either representation.
     *
     * @param color colour name ({@code gold}), hex ({@code #ffaa00}), raw section codes or {@code null}
     * @return the legacy codes, or an empty string when the input is blank or unknown
     */
    public static @NotNull String toLegacy(@Nullable String color) {
        if (color == null || color.isBlank()) return "";
        if (color.indexOf(SECTION) >= 0) return color;

        String raw = color.trim();

        Matcher hex = HEX_INPUT.matcher(raw);
        if (hex.matches()) return toHexLegacy(hex.group(1));

        Matcher prefixedHex = HEX_INPUT_0X.matcher(raw);
        if (prefixedHex.matches()) return toHexLegacy(prefixedHex.group(1));

        String name = raw.toLowerCase(Locale.ROOT).replace(' ', '_');
        String legacy = BY_NAME.get(name);
        if (legacy != null) return legacy;

        // Fall back to a direct hex match, so unusual spellings still resolve.
        String direct = BY_HEX.get(name);
        return direct == null ? "" : direct;
    }

    /**
     * Resolves a {@link NamedTextColor} to its legacy code, e.g. {@code §a} for green.
     */
    public static @NotNull String toLegacy(@Nullable NamedTextColor color) {
        if (color == null) return "";
        return BY_HEX.getOrDefault(stripHash(color.asHexString()), "");
    }

    private static @NotNull String toHexLegacy(@NotNull String hex) {
        String lower = hex.toLowerCase(Locale.ROOT);

        // A vanilla colour can be expressed with its single legacy code.
        String simple = BY_HEX.get(lower);
        if (simple != null) return simple;

        StringBuilder builder = new StringBuilder(14).append(SECTION).append('x');
        for (int i = 0; i < lower.length(); i++) {
            builder.append(SECTION).append(lower.charAt(i));
        }
        return builder.toString();
    }

    /** Strips every legacy section-sign sequence from the given text. */
    public static @NotNull String stripLegacy(@Nullable String text) {
        if (text == null || text.isEmpty()) return "";
        return text.replaceAll("(?i)" + SECTION + "[0-9a-fk-orx]", "");
    }

    /**
     * Finds the last colour code in a formatted string, e.g. the tint of a coloured
     * team name, and returns it as raw legacy codes.
     *
     * @param text text that may embed section codes
     * @return the trailing colour codes, or an empty string when the text carries none
     */
    public static @NotNull String extractLastColor(@Nullable String text) {
        if (text == null || text.isEmpty()) return "";

        Matcher hex = HEX_PATTERN.matcher(text);
        String result = "";
        while (hex.find()) {
            result = text.substring(hex.start(), hex.end());
        }
        if (!result.isEmpty()) return result;

        Matcher simple = SIMPLE_PATTERN.matcher(text);
        while (simple.find()) {
            result = simple.group();
        }
        return result;
    }
}
