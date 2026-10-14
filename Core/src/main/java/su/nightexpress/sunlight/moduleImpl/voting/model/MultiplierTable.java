package su.nightexpress.sunlight.moduleImpl.voting.model;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Players;

/**
 * Declared {@code name -> multiplier} entries, used to turn a permission or a rank into a vote
 * multiplier.
 * <p>
 * Two things forced this instead of reusing {@code RankTable}:
 * <ul>
 *     <li><b>Dots.</b> Permission nodes contain dots, so they cannot be stored as YAML mapping
 *     keys — {@code sunlight.voting.multiplier.2} would be read back as a nested section. Entries
 *     are therefore a list of {@code "node = value"} strings, which survives any name.</li>
 *     <li><b>Combination.</b> A poll can combine a player's multipliers either by taking the best
 *     one or by stacking them, and {@code RankTable} only exposes "greatest".</li>
 * </ul>
 * A numeric node such as {@code sunlight.voting.multiplier.2.0} still cannot be discovered here:
 * Bukkit exposes no way to enumerate a player's effective permission set, so the tiers have to be
 * listed in the config. That is why this is a declared table and not a scan.
 */
public class MultiplierTable {

    private static final String SEPARATOR = "=";

    private final Map<String, Double> permissionValues;
    private final Map<String, Double> rankValues;

    public MultiplierTable(final Map<String, Double> permissionValues, final Map<String, Double> rankValues) {
        this.permissionValues = permissionValues == null ? Map.of() : Map.copyOf(permissionValues);
        this.rankValues = rankValues == null ? Map.of() : Map.copyOf(rankValues);
    }

    public static MultiplierTable read(final FileConfig config, final String path) {
        return new MultiplierTable(
                readSection(config, path + ".Permissions"),
                readSection(config, path + ".Ranks")
        );
    }

    private static Map<String, Double> readSection(final FileConfig config, final String path) {
        final Map<String, Double> values = new LinkedHashMap<>();
        for (final String raw : config.getStringList(path)) {
            if (raw == null || raw.isBlank()) {
                continue;
            }

            // Split on the last '=' so a node or rank containing one still parses.
            final int index = raw.lastIndexOf(SEPARATOR);
            if (index <= 0) {
                continue;
            }

            final String name = raw.substring(0, index).trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty()) {
                continue;
            }

            try {
                final double value = Double.parseDouble(raw.substring(index + 1).trim());
                if (!Double.isFinite(value) || value < 0D) {
                    // NaN and infinities propagate through every Math.min/max clamp and poison
                    // the whole tally; negatives make HIGHEST and ADDITIVE disagree. Drop the
                    // entry so the tier falls back to the default instead of corrupting results.
                    continue;
                }
                values.put(name, value);
            } catch (final NumberFormatException exception) {
                // A typo'd multiplier must not take the whole table down: the entry is dropped
                // and everyone on that tier simply gets the default.
            }
        }
        return values;
    }

    /**
     * @param combine    How a player matching several nodes is treated.
     * @param fallback   Returned when the player matches nothing.
     * @return the player's permission-derived multiplier, or {@code fallback}.
     */
    public double permissionMultiplier(final Player player, final WeightCombine combine, final double fallback) {
        double sum = 0D;
        double best = 0D;
        int matches = 0;

        for (final Map.Entry<String, Double> entry : this.permissionValues.entrySet()) {
            if (!player.hasPermission(entry.getKey())) {
                continue;
            }
            matches++;
            final double value = entry.getValue();
            sum += value;
            best = Math.max(best, value);
        }

        if (matches == 0) {
            return fallback;
        }
        return combine == WeightCombine.ADDITIVE ? sum : best;
    }

    /**
     * Rank multipliers read the player's primary group only, so
     * {@link WeightCombine#ADDITIVE} has nothing extra to add here and is equivalent to
     * {@code HIGHEST}. A player has one primary group; the other groups they hold are the
     * permission tier's business.
     *
     * @return the multiplier of the player's primary group, or {@code fallback}.
     */
    public double rankMultiplier(final Player player, final double fallback) {
        final String group = Players.getPrimaryGroupOrDefault(player);
        if (group == null) {
            return fallback;
        }
        return this.rankValues.getOrDefault(group.toLowerCase(Locale.ROOT), fallback);
    }

    public boolean hasPermissions() {
        return !this.permissionValues.isEmpty();
    }

    public boolean hasRanks() {
        return !this.rankValues.isEmpty();
    }
}