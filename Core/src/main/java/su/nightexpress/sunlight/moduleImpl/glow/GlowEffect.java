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
    private final List<NamedTextColor> colors;
    private final long interval;

    public GlowEffect(String id, String name, GlowType type, List<NamedTextColor> colors, long interval) {
        this.id = Utils.lowercase(id);
        this.name = name;
        this.type = type;
        this.colors = List.copyOf(colors);
        this.interval = Math.max(1L, interval);
    }

    public static GlowEffect read(FileConfig config, String path) {
        String id = path.substring(path.lastIndexOf('.') + 1);
        String name = config.getString(path + ".Name", id);
        GlowType type = Utils.enumValueOf(config.getString(path + ".Type", GlowType.STATIC.name()), GlowType.class);
        if (type == null)
            type = GlowType.STATIC;

        List<NamedTextColor> colors = new ArrayList<>();
        for (String raw : config.getStringList(path + ".Colors")) {
            NamedTextColor color = parseColor(raw);
            if (color != null && !colors.contains(color)) {
                colors.add(color);
            }
        }
        if (colors.isEmpty()) {
            colors.add(NamedTextColor.WHITE);
        }

        long interval = Math.max(1L, config.getLong(path + ".Interval", 10L));

        return new GlowEffect(id, name, type, resolveFrames(type, colors), interval);
    }

    @Override
    public void write(FileConfig config, String path) {
        config.set(path + ".Name", this.name);
        config.set(path + ".Type", this.type.name());
        config.set(path + ".Colors", this.colors.stream().map(c -> c.examinableName()).toList());
        config.set(path + ".Interval", this.interval);
    }

    private static List<NamedTextColor> resolveFrames(GlowType type, List<NamedTextColor> colors) {
        return switch (type) {
            case STATIC -> List.of(colors.getFirst());
            case RAINBOW -> GlowDefaults.RAINBOW_COLORS;
            default -> List.copyOf(colors);
        };
    }

    public static NamedTextColor parseColor(String raw) {
        if (raw == null)
            return null;
        try {
            return NamedTextColor.NAMES.value(raw.trim().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException exception) {
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

    public List<NamedTextColor> getColors() {
        return this.colors;
    }

    public long getInterval() {
        return this.interval;
    }

    public boolean isAnimated() {
        return this.type.isAnimated() && this.colors.size() > 1;
    }

    public NamedTextColor getFrame(int frameIndex) {
        if (this.colors.isEmpty())
            return NamedTextColor.WHITE;
        if (!this.isAnimated())
            return this.colors.getFirst();

        int size = this.colors.size();
        if (this.type.isPingPong() && size > 2) {
            int cycle = (size - 1) * 2;
            int mod = Math.floorMod(frameIndex, cycle);
            int index = mod < size ? mod : cycle - mod;
            return this.colors.get(index);
        }
        return this.colors.get(Math.floorMod(frameIndex, size));
    }
}
