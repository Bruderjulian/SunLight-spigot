package su.nightexpress.sunlight.moduleImpl.voting.model;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

import su.nightexpress.sunlight.moduleImpl.voting.VotingPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolvable;
import su.nightexpress.nightcore.util.placeholder.PlaceholderResolver;
import org.jetbrains.annotations.NotNull;

/**
 * One choice within a poll, plus the gates that decide who may pick it.
 * <p>
 * Mutable because staff can add gates and retune them while the poll is open. The key is
 * immutable for the life of the option: it is half of the ballot table's primary key, so renaming
 * an option means removing it and adding a new one.
 */
public class VoteOption implements PlaceholderResolvable {

    public static final String YES     = "yes";
    public static final String NO      = "no";
    public static final String NEUTRAL = "neutral";

    private static final Set<String> BUILT_IN = Set.of(YES, NO, NEUTRAL);

    private final String key;

    private String display;
    private String icon;

    private final Set<GateType> gates;
    private double cost;
    private String permission;
    private String rank;
    private Set<String> worlds = Set.of();
    private int minPlaytimeHours;

    public VoteOption(final String key, final String display, final String icon) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("option key");
        }
        this.key = key.toLowerCase(Locale.ROOT);
        this.display = display;
        this.icon = icon;
        this.gates = EnumSet.noneOf(GateType.class);
    }

    public static boolean isBuiltInKey(final String key) {
        return key != null && BUILT_IN.contains(key.toLowerCase(Locale.ROOT));
    }

    public static boolean isNeutralKey(final String key) {
        return key != null && NEUTRAL.equalsIgnoreCase(key);
    }

    public boolean isBuiltIn() {
        return isBuiltInKey(this.key);
    }

    public boolean isNeutral() {
        return isNeutralKey(this.key);
    }

    // -- Gates ----------------------------------------------------------------------------------

    /**
     * All gate access is synchronized on the option: table sync mutates gates from the database
     * thread while menus iterate them on the main thread, and an unsynchronized {@code EnumSet}
     * throws {@code ConcurrentModificationException} mid-iteration. Snapshots are copied under
     * the same lock so iteration never races mutation.
     */
    public synchronized boolean isGated() {
        return !this.gates.isEmpty();
    }

    public synchronized boolean hasGate(final GateType type) {
        return this.gates.contains(type);
    }

    /**
     * @return a snapshot rather than a live view, copied under lock.
     */
    public synchronized @NotNull Set<GateType> getGates() {
        return Set.copyOf(this.gates);
    }

    public synchronized void setGates(final Set<GateType> gates) {
        this.gates.clear();
        if (gates != null) {
            this.gates.addAll(gates);
        }
    }

    public synchronized void addGate(final GateType type) {
        this.gates.add(type);
    }

    public synchronized void removeGate(final GateType type) {
        this.gates.remove(type);
    }

    public synchronized void toggleGate(final GateType type) {
        if (this.gates.contains(type)) {
            this.gates.remove(type);
        } else {
            this.gates.add(type);
        }
    }

    /**
     * @return whether the payment gate is configured to cost nothing. Kept as a method so the
     *         "gate is off" and "gate is on but free" cases cannot be confused by a caller
     *         reading {@code cost == 0}.
     */
    public boolean isFree() {
        return this.cost <= 0D;
    }

    // -- Accessors ------------------------------------------------------------------------------

    @Override
    public @NotNull PlaceholderResolver placeholders() {
        return VotingPlaceholders.OPTION.resolver(this);
    }

    public String getKey() {
        return this.key;
    }

    public String getDisplay() {
        return this.display == null || this.display.isBlank() ? this.key : this.display;
    }

    public void setDisplay(final String display) {
        this.display = display;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setIcon(final String icon) {
        this.icon = icon;
    }

    public double getCost() {
        return this.cost;
    }

    public void setCost(final double cost) {
        this.cost = Math.max(0D, cost);
    }

    public String getPermission() {
        return this.permission;
    }

    public void setPermission(final String permission) {
        this.permission = permission;
    }

    public boolean requiresPermission() {
        return this.permission != null && !this.permission.isBlank();
    }

    public String getRank() {
        return this.rank;
    }

    public void setRank(final String rank) {
        this.rank = rank;
    }

    public boolean requiresRank() {
        return this.rank != null && !this.rank.isBlank();
    }

    public Set<String> getWorlds() {
        return this.worlds;
    }

    public void setWorlds(final Set<String> worlds) {
        this.worlds = worlds == null ? Set.of() : Set.copyOf(worlds);
    }

    public int getMinPlaytimeHours() {
        return this.minPlaytimeHours;
    }

    public void setMinPlaytimeHours(final int minPlaytimeHours) {
        this.minPlaytimeHours = Math.max(0, minPlaytimeHours);
    }
}