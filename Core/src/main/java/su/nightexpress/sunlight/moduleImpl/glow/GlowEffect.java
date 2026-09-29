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
    private GlowPhase[] phases;
    private int len;

    public static enum GlowType {
        STATIC,
        PHASED;
    }

    public GlowEffect(final String id, final String name, final List<GlowPhase> phases) {
        this.id = Utils.lowercase(id);
        this.name = name;
        if (phases == null || phases.isEmpty()) {
            this.phases = new GlowPhase[] { new GlowPhase(NamedTextColor.WHITE, GlowPhase.INFINITE_DURATION) };
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
            this.phases = new GlowPhase[] { new GlowPhase(NamedTextColor.WHITE, GlowPhase.INFINITE_DURATION) };
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

        phase = phase == null ? new GlowPhase(NamedTextColor.WHITE, GlowPhase.INFINITE_DURATION) : phase;
        this.phases = new GlowPhase[] { phase };
        this.len = 1;
    }

    public GlowEffect(final String id, final String name, final NamedTextColor color) {
        this.id = Utils.lowercase(id);
        this.name = name;
        this.type = GlowType.STATIC;
        this.phases = new GlowPhase[] { new GlowPhase(color, GlowPhase.INFINITE_DURATION) };
        this.len = 1;
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
        for (final String token : raw.split(",", GlowHandler.MAX_PHASES)) {
            final GlowPhase phase = GlowPhase.parse(token);
            if (phase != null) {
                phases.add(phase);
            }
        }
        return new GlowEffect(id, name, phases);
    }

    public static GlowEffect parse(final String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        final String[] parts = raw.trim().split(":", 3);
        if (parts.length != 3) {
            return null;
        }
        final String id = parts[0] == null || parts[0].isEmpty() ? null : parts[0].trim();
        if (id == null) {
            return null;
        }
        final String name = parts[1] == null || parts[1].isEmpty() ? id : parts[1].trim();
        final List<GlowPhase> phases = new ArrayList<>();
        for (final String phasePart : parts[2].split(",", GlowHandler.MAX_PHASES)) {
            final GlowPhase phase = GlowPhase.parse(phasePart);
            if (phase != null) {
                phases.add(phase);
            }
        }
        if (phases.isEmpty()) {
            return null;
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

    public String encode() {
        final StringBuilder builder = new StringBuilder(id);
        builder.append(":");
        builder.append(name);
        builder.append(":");
        for (int index = 0; index < len; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(phases[index].encode());
        }
        return builder.toString();
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

    public boolean addPhase(final GlowPhase phase) {
        if (phase == null) {
            return false;
        }
        if (len >= GlowHandler.MAX_PHASES) {
            return false;
        }
        this.len = len + 2;
        final GlowPhase[] newArray = new GlowPhase[len];
        System.arraycopy(phases, 0, newArray, 0, len);
        newArray[phases.length] = phase;
        phases = newArray;
        return true;
    }

    public boolean removePhase(final int index) {
        if (index < 0 || index >= phases.length) {
            return false;
        }
        final int newSize = len - 1;
        if (newSize > index) {
            System.arraycopy(phases, index + 1, phases, index, newSize - index);
        }
        phases[len = newSize] = null;
        return true;
    }

    public boolean setPhaseDuration(final int index, final long duration) {
        if (index < 0 || index >= len) {
            return false;
        }
        phases[index] = new GlowPhase(phases[index].color(), duration);
        return true;
    }
}
