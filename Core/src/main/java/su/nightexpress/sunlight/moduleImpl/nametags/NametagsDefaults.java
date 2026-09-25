package su.nightexpress.sunlight.moduleImpl.nametags;

import su.nightexpress.sunlight.moduleImpl.nametags.model.PriceMode;
import su.nightexpress.sunlight.moduleImpl.nametags.model.Profile;
import su.nightexpress.sunlight.moduleImpl.nametags.model.RankDefinition;
import su.nightexpress.sunlight.moduleImpl.nametags.model.SubscriptionPeriod;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagAccessMode;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

public class NametagsDefaults {

    public static Map<String, RankDefinition> getDefaultRanks() {
        Map<String, RankDefinition> map = new LinkedHashMap<>();

        map.put("default", new RankDefinition("default", 1, Set.of("default"), AQUA.wrap("Member "), "", "white", true));
        map.put("admin", new RankDefinition("admin", 100, Set.of("admin"), RED.wrap("Admin "), "", "white", false));
        map.put("owner", new RankDefinition("owner", 10_000, Set.of("owner"), PURPLE.wrap("Owner "), "", "white", false));

        return map;
    }

    public static Map<String, TagDefinition> getDefaultTags() {
        Map<String, TagDefinition> map = new LinkedHashMap<>();

        map.put("dragon", new TagDefinition("dragon",
            GREEN.wrap("Dragon"),
            List.of(DARK_GRAY.wrap("A rare cosmetic tag.")),
            GRAY.wrap("[" + GREEN.wrap("✦") + "] "),
            "",
            "",
            "",
            TagAccessMode.PERMISSION,
            PriceMode.INTERNAL,
            0D,
            SubscriptionPeriod.MONTHLY,
            "DRAGON_HEAD",
            true,
            10
        ));

        map.put("hero", new TagDefinition("hero",
            GOLD.wrap("Hero"),
            List.of(DARK_GRAY.wrap("Earned by the bravest.")),
            GOLD.wrap("[" + YELLOW.wrap("★") + "] "),
            "",
            "",
            "",
            TagAccessMode.PURCHASE,
            PriceMode.INTERNAL,
            1_000D,
            SubscriptionPeriod.MONTHLY,
            "IRON_SWORD",
            true,
            20
        ));

        return map;
    }

    public static Map<String, Profile> getDefaultProfiles() {
        Map<String, Profile> map = new LinkedHashMap<>();

        map.put("default", new Profile("default", "Default", List.of(), "NAME_TAG", List.of(), 0, null, null));

        return map;
    }
}
