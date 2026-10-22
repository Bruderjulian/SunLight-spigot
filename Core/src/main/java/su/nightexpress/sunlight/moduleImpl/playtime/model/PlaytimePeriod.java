package su.nightexpress.sunlight.moduleImpl.playtime.model;

import org.bukkit.Material;

import su.nightexpress.nightcore.util.bukkit.NightItem;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;
import su.nightexpress.nightcore.ui.inventory.item.MenuItem;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.*;

import java.util.List;

/**
 * Time buckets used by the playtime module.
 */
public enum PlaytimePeriod {

    ALLTIME("Alltime"), YEAR("Year"), MONTH("Month"), WEEK("Week"), DAY("Day"), SESSION("Session");

    private String displayname;

    private PlaytimePeriod(String displayname) {
        this.displayname = displayname;
    }

    public static PlaytimePeriod fromString(String raw) {
        return Utils.enumOptionalValueOf(raw, PlaytimePeriod.class).orElse(ALLTIME);
    }

    public String getConfigName() {
        return this.name().toLowerCase();
    }

    public String displayname() {
        return displayname;
    }

    public MenuItem item(Material material, int slot, long ms) {
        return MenuItem.button()
                .defaultState(NightItem.fromType(material)
                        .setDisplayName(GOLD.wrap(displayname))
                        .setLore(List.of(WHITE.wrap(TimeUtil.formatMillis(ms))))
                        .hideAllComponents())
                .slots(slot)
                .build();
    }
}