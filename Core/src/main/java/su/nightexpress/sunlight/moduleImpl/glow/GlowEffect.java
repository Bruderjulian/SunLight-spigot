package su.nightexpress.sunlight.moduleImpl.glow;

import net.kyori.adventure.text.format.NamedTextColor;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.sunlight.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

public class GlowEffect implements Writeable {

    private final String id;
    private final String name;
    private final GlowType type;
    private final GlowPhase[] phases;
    private final int len;

    public static enum GlowType {

        STATIC,
        PHASED;
    }

    public GlowEffect(final String id, final String name, final List<GlowPhase> phases) {
        this.id = Utils.lowercase(id);
        this.name = name;
        if (phases == null || phases.isEmpty()) {
            this.phases = new GlowPhase[] { new GlowPhase(NamedTextColor.WHITE, GlowPhase.DEFAULT_DURATION) };
            this.type = GlowType.STATIC;
            this.len = 1;
        } else if (phases.size() == 1) {
            this.phases = new GlowPhase[] { phases.get(0) };
            this.type = GlowType.STATIC;
            this.len = 1;
        } else {
            this.phases = phases.toArray(new GlowPhase[phases.size()]);
            this.type = GlowType.PHASED;
            this.len = this.phases.length;
        }
    }

    public GlowEffect(final String id, final String name, final GlowPhase... phases) {
        this.id = Utils.lowercase(id);
        this.name = name;
        if (phases == null || phases.length == 0) {
            this.phases = new GlowPhase[] { new GlowPhase(NamedTextColor.WHITE, GlowPhase.DEFAULT_DURATION) };
            this.type = GlowType.STATIC;
            this.len = 1;
        } else if (phases.length == 1) {
            this.phases = new GlowPhase[] { phases[0] };
            this.type = GlowType.STATIC;
            this.len = 1;
        } else {
            this.phases = phases;
            this.type = GlowType.PHASED;
            this.len = this.phases.length;
        }
    }

    public GlowEffect(final String id, final String name, GlowPhase phase) {
        this.id = Utils.lowercase(id);
        this.name = name;
        this.type = GlowType.STATIC;

        phase = phase == null ? new GlowPhase(NamedTextColor.WHITE, GlowPhase.DEFAULT_DURATION) : phase;
        this.phases = new GlowPhase[] { phase };
        this.len = 1;
    }

    /** Backwards-compatible factory for single-color effects. */
    public static GlowEffect ofColor(final String id, final String name, final NamedTextColor color) {
        return new GlowEffect(id, name, new GlowPhase(color, GlowPhase.DEFAULT_DURATION));
    }

    public static GlowEffect read(final FileConfig config, final String path) {
        final String id = path.substring(path.lastIndexOf('.') + 1);

        final List<GlowPhase> phases = new ArrayList<>();
        if (config.contains(path + ".Phases")) {
            for (final String raw : config.getStringList(path + ".Phases")) {
                if (phases.size() > GlowHandler.MAX_PHASES) {
                    continue;
                }
                final GlowPhase phase = GlowPhase.parse(raw);
                if (phase != null) {
                    phases.add(phase);
                }
            }
        }
        return new GlowEffect(id, config.getString(path + ".Name", id), phases);
    }

    public static GlowEffect parse(final String id, final String name, final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        final List<GlowPhase> phases = new ArrayList<>();
        for (final String token : raw.split(",")) {
            if (phases.size() > GlowHandler.MAX_PHASES) {
                continue;
            }
            final GlowPhase phase = GlowPhase.parse(token);
            if (phase != null) {
                phases.add(phase);
            }
        }
        return new GlowEffect(id, name, phases);
    }

    @Override
    public void write(final FileConfig config, final String path) {
        final List<String> list = new ArrayList<>();
        for (int i = 0; i < len; i++) {
            list.add(phases[i].encode());
        }
        config.set(path + ".Name", this.name);
        config.set(path + ".Phases", list);
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

    public GlowPhase[] getPhases() {
        return this.phases;
    }

    public boolean isAnimated() {
        return this.type == GlowType.PHASED;
    }

    public GlowPhase getPhase(final int phaseIndex) {
        if (this.type != GlowType.PHASED) {
            return this.phases[0];
        }
        return this.phases[Math.floorMod(phaseIndex, len)];
    }

    public NamedTextColor getBaseColor() {
        return phases[0].color();
    }

    public Material material() {
        if (type == GlowType.STATIC) {
            final Material material = phases[0].material();
            return material == null ? Material.WHITE_WOOL : material;
        }
        return Material.PRISMARINE_SHARD;
    }

    public int phaseCount() {
        return len;
    }
}
