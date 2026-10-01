package su.nightexpress.sunlight.moduleImpl.glow;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlowDefaults {
        public static final List<String> VANILLA_COLORS = List.of(
                        "white", "gray", "dark_gray", "black", "red", "dark_red", "gold", "yellow",
                        "green", "dark_green", "aqua", "dark_aqua", "blue", "dark_blue",
                        "light_purple", "dark_purple");

        public static Map<String, GlowEffect> getDefaultEffects() {
                final Map<String, GlowEffect> map = new HashMap<>();

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

                map.put("rainbow",
                                new GlowEffect("rainbow", "<red>R<gold>a<yellow>i<green>n<aqua>b<blue>o<light_purple>w",
                                                new GlowPhase(NamedTextColor.RED, 10L),
                                                new GlowPhase(NamedTextColor.GOLD, 10L),
                                                new GlowPhase(NamedTextColor.YELLOW, 10L),
                                                new GlowPhase(NamedTextColor.GREEN, 10L),
                                                new GlowPhase(NamedTextColor.AQUA, 10L),
                                                new GlowPhase(NamedTextColor.BLUE, 10L),
                                                new GlowPhase(NamedTextColor.LIGHT_PURPLE, 10L)));

                map.put("sunset", new GlowEffect("sunset", "<red>Sunset",
                                new GlowPhase(NamedTextColor.RED, 20L),
                                new GlowPhase(NamedTextColor.GOLD, 20L),
                                new GlowPhase(NamedTextColor.YELLOW, 20L)));

                map.put("ocean", new GlowEffect("ocean", "<aqua>Ocean",
                                new GlowPhase(NamedTextColor.DARK_BLUE, 20L),
                                new GlowPhase(NamedTextColor.BLUE, 20L),
                                new GlowPhase(NamedTextColor.AQUA, 20L),
                                new GlowPhase(NamedTextColor.GREEN, 20L)));

                map.put("candy", new GlowEffect("candy", "<light_purple>Candy",
                                new GlowPhase(NamedTextColor.LIGHT_PURPLE, 15L),
                                new GlowPhase(NamedTextColor.DARK_PURPLE, 15L),
                                new GlowPhase(NamedTextColor.AQUA, 15L),
                                new GlowPhase(NamedTextColor.DARK_AQUA, 15L)));

                map.put("alert", new GlowEffect("alert", "<red>Alert",
                                new GlowPhase(NamedTextColor.RED, 10L),
                                new GlowPhase(NamedTextColor.WHITE, 10L)));

                return map;
        }

        private static void addStatic(final Map<String, GlowEffect> map, final String id, final String name,
                        final NamedTextColor color) {
                map.put(id, new GlowEffect(id, name, color));
        }
}
