package su.nightexpress.sunlight.moduleImpl.nametags.render;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.sunlight.utils.LegacyText;

/**
 * Combines the team, rank, tag and glow parts into a single {@link Nameplate}.
 * <p>
 * Ordering is deliberate and shared with the glow module:
 * <ul>
 *     <li>prefix: team, then rank, then tag</li>
 *     <li>suffix: rank, then tag, then team</li>
 *     <li>name colour: team first, then rank/tag, and glow always last</li>
 * </ul>
 * Because the client carries the final colour code of a team prefix into the player
 * name, appending the colour to the end of the prefix is what actually colours the
 * name. That is why glow is required to contribute the last colour token.
 * <p>
 * Pure and side-effect free so it can be unit tested directly.
 */
public final class PrefixComposer {

    private PrefixComposer() {
    }

    /**
     * @param team                 UltimateTeams contribution, may be {@code null}
     * @param rank                 resolved rank contribution, may be {@code null}
     * @param tag                  selected tag contribution, may be {@code null}
     * @param glow                 legacy glow colour name, may be {@code null}
     * @param tagColorOverridesRank whether the tag colour beats the rank colour
     */
    public static @NotNull Nameplate compose(@Nullable Part team,
            @Nullable Part rank,
            @Nullable Part tag,
            @Nullable String glow,
            boolean tagColorOverridesRank
    ) {
        Part teamPart = team == null ? Part.EMPTY : team;
        Part rankPart = rank == null ? Part.EMPTY : rank;
        Part tagPart = tag == null ? Part.EMPTY : tag;

        String prefix = teamPart.prefix() + rankPart.prefix() + tagPart.prefix();
        String suffix = rankPart.suffix() + tagPart.suffix() + teamPart.suffix();

        String color = resolveColor(teamPart, rankPart, tagPart, glow, tagColorOverridesRank);

        // The trailing colour code is what colours the player name, so it goes last.
        if (!color.isEmpty()) {
            prefix = prefix + color;
        }

        return new Nameplate(prefix, suffix, color, glow == null ? "" : glow);
    }

    /**
     * Team colour is the weakest source, glow is always the strongest because it
     * tracks the player's currently active glow effect.
     */
    static @NotNull String resolveColor(@NotNull Part team,
            @NotNull Part rank,
            @NotNull Part tag,
            @Nullable String glow,
            boolean tagColorOverridesRank
    ) {
        if (glow != null && !glow.isBlank()) {
            String glowCode = LegacyText.toLegacy(glow);
            if (!glowCode.isEmpty()) return glowCode;
        }

        String primary = tagColorOverridesRank ? tag.color() : rank.color();
        String fallback = tagColorOverridesRank ? rank.color() : tag.color();

        String winner = firstNonBlank(primary, fallback);
        if (winner == null) winner = team.color();
        if (winner == null || winner.isBlank()) return "";

        return LegacyText.toLegacy(winner);
    }

    private static @Nullable String firstNonBlank(@Nullable String primary, @Nullable String fallback) {
        if (primary != null && !primary.isBlank()) return primary;
        if (fallback != null && !fallback.isBlank()) return fallback;
        return null;
    }
}
