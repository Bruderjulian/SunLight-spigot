package su.nightexpress.sunlight.moduleImpl.nametagsOld;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import su.nightexpress.sunlight.moduleImpl.nametagsOld.model.NametagRule;
import su.nightexpress.sunlight.moduleImpl.nametagsOld.model.NametagTag;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class NametagsDefaults {

    public static Map<String, NametagRule> getDefaultRules() {
        Map<String, NametagRule> map = new LinkedHashMap<>();

        map.put("default", new NametagRule(1, Set.of("default"), AQUA.wrap("Member "), "", "white"));
        map.put("admin", new NametagRule(100, Set.of("admin"), RED.wrap("Admin "), "", "white"));
        map.put("owner", new NametagRule(10_000, Set.of("owner"), PURPLE.wrap("Owner "), "", "white"));

        return map;
    }

    public static Map<String, NametagTag> getDefaultTags() {
        Map<String, NametagTag> map = new LinkedHashMap<>();

        map.put("dragon", new NametagTag("dragon", GREEN.wrap("Dragon"), GRAY.wrap("[" + GREEN.wrap("✦") + "] "), "", "",
                0D, java.util.List.of(DARK_GRAY.wrap("A rare cosmetic tag.")), ""));
        map.put("hero", new NametagTag("hero", GOLD.wrap("Hero"), GOLD.wrap("[" + YELLOW.wrap("★") + "] "), "", "",
                0D, java.util.List.of(DARK_GRAY.wrap("Earned by the bravest.")), ""));

        return map;
    }
}