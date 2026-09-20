package su.nightexpress.sunlight.moduleImpl.glow;

import org.bukkit.ChatColor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GlowDefaults {

    public static final List<ChatColor> RAINBOW_COLORS = List.of(
        ChatColor.RED,
        ChatColor.GOLD,
        ChatColor.YELLOW,
        ChatColor.GREEN,
        ChatColor.AQUA,
        ChatColor.BLUE,
        ChatColor.LIGHT_PURPLE
    );

    public static Map<String, GlowEffect> getDefaultEffects() {
        Map<String, GlowEffect> map = new LinkedHashMap<>();

        addStatic(map, "white", "<white>White", ChatColor.WHITE);
        addStatic(map, "gray", "<gray>Gray", ChatColor.GRAY);
        addStatic(map, "dark_gray", "<dark_gray>Dark Gray", ChatColor.DARK_GRAY);
        addStatic(map, "black", "<black>Black", ChatColor.BLACK);
        addStatic(map, "red", "<red>Red", ChatColor.RED);
        addStatic(map, "dark_red", "<dark_red>Dark Red", ChatColor.DARK_RED);
        addStatic(map, "gold", "<gold>Gold", ChatColor.GOLD);
        addStatic(map, "yellow", "<yellow>Yellow", ChatColor.YELLOW);
        addStatic(map, "green", "<green>Green", ChatColor.GREEN);
        addStatic(map, "dark_green", "<dark_green>Dark Green", ChatColor.DARK_GREEN);
        addStatic(map, "aqua", "<aqua>Aqua", ChatColor.AQUA);
        addStatic(map, "dark_aqua", "<dark_aqua>Dark Aqua", ChatColor.DARK_AQUA);
        addStatic(map, "blue", "<blue>Blue", ChatColor.BLUE);
        addStatic(map, "dark_blue", "<dark_blue>Dark Blue", ChatColor.DARK_BLUE);
        addStatic(map, "pink", "<light_purple>Pink", ChatColor.LIGHT_PURPLE);
        addStatic(map, "purple", "<dark_purple>Purple", ChatColor.DARK_PURPLE);

        map.put("rainbow", new GlowEffect("rainbow", "<red>R<gold>a<yellow>i<green>n<aqua>b<blue>o<light_purple>w", GlowType.RAINBOW, RAINBOW_COLORS, 10L));

        map.put("sunset", new GlowEffect("sunset", "<red>Sunset Gradient", GlowType.GRADIENT,
            List.of(ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GOLD), 10L));

        map.put("ocean", new GlowEffect("ocean", "<aqua>Ocean Gradient", GlowType.GRADIENT,
            List.of(ChatColor.DARK_BLUE, ChatColor.BLUE, ChatColor.AQUA, ChatColor.GREEN), 10L));

        map.put("candy", new GlowEffect("candy", "<light_purple>Candy", GlowType.CYCLE,
            List.of(ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE, ChatColor.AQUA, ChatColor.DARK_AQUA), 10L));

        map.put("alert", new GlowEffect("alert", "<red>Alert Flash", GlowType.FLASH,
            List.of(ChatColor.RED, ChatColor.WHITE), 10L));

        map.put("strobe", new GlowEffect("strobe", "<white>Strobe Flash", GlowType.FLASH,
            List.of(ChatColor.WHITE, ChatColor.BLACK), 6L));

        return map;
    }

    private static void addStatic(Map<String, GlowEffect> map, String id, String name, ChatColor color) {
        map.put(id, new GlowEffect(id, name, GlowType.STATIC, List.of(color), 10L));
    }
}
