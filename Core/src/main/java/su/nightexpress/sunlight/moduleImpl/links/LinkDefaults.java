package su.nightexpress.sunlight.moduleImpl.links;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.util.bukkit.NightItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class LinkDefaults {

    /**
     * Link IDs end up as command labels and config keys, so they are kept to a conservative character
     * set. A leading digit is excluded as well, to keep the IDs readable in tab completion.
     */
    public static final Pattern ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,31}$");

    /**
     * IDs that would shadow the module's own commands. A link with one of these IDs would hijack
     * {@code /links}, {@code /links all}, {@code /links get}, a standalone helper (e.g.
     * {@code /alllinks}) or an admin command (e.g. {@code /createlink}), so creation is rejected and
     * existing config entries are skipped with a warning.
     */
    public static final java.util.Set<String> RESERVED_IDS = java.util.Set.of(
        "all", "get",
        "links", "serverlinks", "alllinks", "linklist", "getlink",
        "linksadmin", "links-admin", "editlinks",
        "createlink", "deletelink", "setlinkurl", "setlinkname", "setlinkcommand",
        "setlinkexecutor", "setlinkpermission", "setlinkicon", "setlinkpriority",
        "togglelink", "resetlinkclicks"
    );

    public static final List<Material> ICON_PALETTE = List.of(
            Material.PLAYER_HEAD,
            Material.DIAMOND,
            Material.EMERALD,
            Material.GOLD_INGOT,
            Material.IRON_INGOT,
            Material.MAP,
            Material.WRITABLE_BOOK,
            Material.KNOWLEDGE_BOOK,
            Material.GRASS_BLOCK,
            Material.ENDER_CHEST,
            Material.CHEST,
            Material.NAME_TAG,
            Material.COMPASS,
            Material.OAK_SIGN,
            Material.SHIELD,
            Material.NETHER_STAR
    );

    private LinkDefaults() {
    }

    public static @NotNull Map<String, Link> createDefaults() {
        Map<String, Link> map = new LinkedHashMap<>();

        map.put("discord", create("discord", "<#5865F2>Discord", "https://discord.gg/example", Material.PLAYER_HEAD, 100));
        map.put("site", create("site", "<#7BD88F>Website", "https://example.com", Material.GRASS_BLOCK, 90));
        map.put("store", create("store", "<#FFD75F>Store", "https://example.com/store", Material.GOLD_INGOT, 80));
        map.put("vote", create("vote", "<#FF6B6B>Vote", "https://example.com/vote", Material.DIAMOND, 70));
        map.put("map", create("map", "<#6BCBFF>Map", "https://example.com/map", Material.MAP, 60));

        return map;
    }

    private static Link create(@NotNull String id, @NotNull String display, @NotNull String url,
                               @NotNull Material material, int priority) {
        Link link = new Link(id, display, url);
        link.setIcon(NightItem.fromType(material));
        link.setPriority(priority);
        return link;
    }

    public static boolean isValidId(@NotNull String id) {
        return ID_PATTERN.matcher(id).matches() && !RESERVED_IDS.contains(id);
    }

    public static boolean isReservedId(@NotNull String id) {
        return RESERVED_IDS.contains(id);
    }

    /**
     * Normalizes user input into a link ID. Returns {@code null} when the input cannot be salvaged.
     */
    public static @org.jetbrains.annotations.Nullable String normalizeId(@NotNull String input) {
        String id = input.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
        if (id.isEmpty())
            return null;

        if (!Character.isLetter(id.charAt(0)))
            id = "link_" + id;

        return id.length() > 32 ? id.substring(0, 32) : id;
    }
}
