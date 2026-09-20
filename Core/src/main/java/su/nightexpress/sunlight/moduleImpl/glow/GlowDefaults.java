package su.nightexpress.sunlight.moduleImpl.glow;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GlowDefaults {

    public static final List<NamedTextColor> RAINBOW_COLORS = List.of(
        NamedTextColor.RED,
        NamedTextColor.GOLD,
        NamedTextColor.YELLOW,
        NamedTextColor.GREEN,
        NamedTextColor.AQUA,
        NamedTextColor.BLUE,
        NamedTextColor.LIGHT_PURPLE
    );

    public static Map<String, GlowEffect> getDefaultEffects() {
        Map<String, GlowEffect> map = new LinkedHashMap<>();

        addStatic(map, "white", "<white>White", NamedTextColor.WHITE);
        addStatic(map, "gray", "<gray>Gray", NamedTextColor.GRAY);
        addStatic(map, "dark_gray", "<dark_gray>Dark Gray", NamedTextColor.DARK_GRAY);
        addStatic(map, "black", "<black>Black", NamedTextColor.BLACK);
        addStatic(map, "red", "<red>Red", NamedTextColor.RED);
        addStatic(map, "dark_red", "<dark_red>Dark Red", NamedTextColor.DARK_RED);
        addStatic(map, "gold", "<gold>Gold", NamedTextColor.GOLD);
        addStatic(map, "yellow", "<yellow>Yellow", NamedTextColor.YELLOW);
        addStatic(map, "green", "<green>Green", NamedTextColor.GREEN);
        addStatic(map, "dark_green", "<dark_green>Dark Green", NamedTextColor.DARK_GREEN);
        addStatic(map, "aqua", "<aqua>Aqua", NamedTextColor.AQUA);
        addStatic(map, "dark_aqua", "<dark_aqua>Dark Aqua", NamedTextColor.DARK_AQUA);
        addStatic(map, "blue", "<blue>Blue", NamedTextColor.BLUE);
        addStatic(map, "dark_blue", "<dark_blue>Dark Blue", NamedTextColor.DARK_BLUE);
        addStatic(map, "pink", "<light_purple>Pink", NamedTextColor.LIGHT_PURPLE);
        addStatic(map, "purple", "<dark_purple>Purple", NamedTextColor.DARK_PURPLE);

        map.put("rainbow", new GlowEffect("rainbow", "<red>R<gold>a<yellow>i<green>n<aqua>b<blue>o<light_purple>w", GlowType.RAINBOW, RAINBOW_COLORS, 10L));

        map.put("sunset", new GlowEffect("sunset", "<red>Sunset Gradient", GlowType.GRADIENT,
            List.of(NamedTextColor.RED, NamedTextColor.GOLD, NamedTextColor.YELLOW, NamedTextColor.GOLD), 10L));

        map.put("ocean", new GlowEffect("ocean", "<aqua>Ocean Gradient", GlowType.GRADIENT,
            List.of(NamedTextColor.DARK_BLUE, NamedTextColor.BLUE, NamedTextColor.AQUA, NamedTextColor.GREEN), 10L));

        map.put("candy", new GlowEffect("candy", "<light_purple>Candy", GlowType.CYCLE,
            List.of(NamedTextColor.LIGHT_PURPLE, NamedTextColor.DARK_PURPLE, NamedTextColor.AQUA, NamedTextColor.DARK_AQUA), 10L));

        map.put("alert", new GlowEffect("alert", "<red>Alert Flash", GlowType.FLASH,
            List.of(NamedTextColor.RED, NamedTextColor.WHITE), 10L));

        map.put("strobe", new GlowEffect("strobe", "<white>Strobe Flash", GlowType.FLASH,
            List.of(NamedTextColor.WHITE, NamedTextColor.BLACK), 6L));

        return map;
    }

    private static void addStatic(Map<String, GlowEffect> map, String id, String name, NamedTextColor color) {
        map.put(id, new GlowEffect(id, name, GlowType.STATIC, List.of(color), 10L));
    }
}
