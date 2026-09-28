package su.nightexpress.sunlight.moduleImpl.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.sunlight.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class GlowEffect implements Writeable {

    private final String id;
    private final String name;
    private final GlowType type;
    private final List<GlowPhase> phases;

    public GlowEffect(final String id, final String name, final GlowType type, final List<GlowPhase> phases) {
        this.id = Utils.lowercase(id);
        this.name = name;
        GlowType resolved = type == null ? GlowType.STATIC : type;
        List<GlowPhase> copy = phases == null || phases.isEmpty()
            ? List.of(new GlowPhase(NamedTextColor.WHITE, GlowPhase.DEFAULT_DURATION))
            : List.copyOf(phases);
        // Normalize: single-phase PHASED behaves like STATIC.
        if (resolved == GlowType.PHASED && copy.size() < 2) {
            resolved = GlowType.STATIC;
        }
        if (resolved == GlowType.STATIC) {
            copy = List.of(copy.getFirst());
        }
        this.type = resolved;
        this.phases = copy;
    }

    /** Backwards-compatible factory for single-color effects. */
    public static GlowEffect ofColor(final String id, final String name, final NamedTextColor color) {
        return new GlowEffect(id, name, GlowType.STATIC, List.of(new GlowPhase(color, GlowPhase.DEFAULT_DURATION)));
    }

    public static GlowEffect read(final FileConfig config, final String path) {
        final String id = path.substring(path.lastIndexOf('.') + 1);
        final String name = config.getString(path + ".Name", id);
        GlowType type = Utils.enumValueOf(config.getString(path + ".Type", GlowType.STATIC.name()), GlowType.class);

        List<GlowPhase> phases = new ArrayList<>();
        if (config.contains(path + ".Phases")) {
            for (final String raw : config.getStringList(path + ".Phases")) {
                final GlowPhase phase = GlowPhase.parse(raw);
                if (phase != null) {
                    phases.add(phase);
                }
            }
        }

        // Legacy fallback: entries stored as Colors + Interval become phases,
        // each color one phase with the shared interval. This preserves old
        // single-color and multi-color configs as STATIC / PHASED respectively.
        if (phases.isEmpty() && config.contains(path + ".Colors")) {
            final List<NamedTextColor> legacyColors = new ArrayList<>();
            for (final String raw : config.getStringList(path + ".Colors")) {
                final NamedTextColor color = parseColor(raw);
                if (color != null && !legacyColors.contains(color)) {
                    legacyColors.add(color);
                }
            }
            final long legacyInterval = Math.max(1L, config.getLong(path + ".Interval", GlowPhase.DEFAULT_DURATION));
            for (final NamedTextColor color : legacyColors) {
                phases.add(new GlowPhase(color, legacyInterval));
            }
        }

        if (phases.isEmpty()) {
            phases.add(new GlowPhase(NamedTextColor.WHITE, GlowPhase.DEFAULT_DURATION));
        }
        if (type == null) {
            type = phases.size() > 1 ? GlowType.PHASED : GlowType.STATIC;
        }
        // Unknown legacy type strings resolve to null via enumValueOf; default sensibly.
        if (type == GlowType.PHASED && phases.size() < 2 && phases.size() == 1) {
            type = GlowType.STATIC;
        }

        return new GlowEffect(id, name, type, phases);
    }

    @Override
    public void write(final FileConfig config, final String path) {
        config.set(path + ".Name", this.name);
        config.set(path + ".Type", this.type.name());
        config.set(path + ".Phases", this.phases.stream().map(GlowPhase::encode).toList());
        config.remove(path + ".Colors");
        config.remove(path + ".Interval");
    }

    public static NamedTextColor parseColor(final String raw) {
        if (raw == null)
            return null;
        try {
            return NamedTextColor.NAMES.value(raw.trim().toUpperCase().replace(" ", "_"));
        } catch (final IllegalArgumentException exception) {
            return null;
        }
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public GlowType getType() {
        return this.type;
    }

    public List<GlowPhase> getPhases() {
        return this.phases;
    }

    /** Derived color list, kept for menu / permission / placeholder convenience. */
    public List<NamedTextColor> getColors() {
        return this.phases.stream().map(GlowPhase::color).distinct().toList();
    }

    /** Legacy accessor: global frame duration. Now returns the first phase duration. */
    public long getInterval() {
        return this.phases.isEmpty() ? GlowPhase.DEFAULT_DURATION : this.phases.getFirst().durationTicks();
    }

    public boolean isAnimated() {
        return this.type.isAnimated() && this.phases.size() > 1;
    }

    public NamedTextColor getFrame(final int phaseIndex) {
        return this.getPhase(phaseIndex).color();
    }

    public GlowPhase getPhase(final int phaseIndex) {
        if (this.phases.isEmpty())
            return new GlowPhase(NamedTextColor.WHITE, GlowPhase.DEFAULT_DURATION);
        if (!this.isAnimated())
            return this.phases.getFirst();
        return this.phases.get(Math.floorMod(phaseIndex, this.phases.size()));
    }

    public int phaseCount() {
        return this.phases.size();
    }
}
