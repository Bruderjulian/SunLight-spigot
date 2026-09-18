package su.nightexpress.sunlight.module.warps.core;

import org.bukkit.Material;
import org.jspecify.annotations.NonNull;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.util.bukkit.NightItem;

public class WarpsSettings extends AbstractConfig {

    protected final ConfigProperty<Integer> warpSaveInterval = this.addProperty(ConfigTypes.INT, "Warps.Save-Interval", 60);

    protected final ConfigProperty<NightItem> warpDefaultIcon = this.addProperty(ConfigTypes.NIGHT_ITEM, "Warps.Default-Icon",
        NightItem.fromType(Material.ENDER_PEARL)
    );

    protected final ConfigProperty<Double> warpCreationCost = this.addProperty(ConfigTypes.DOUBLE, "Warps.Creation.Cost",
        0D,
        "Sets how much it costs (in Vault currency) to create a warp.",
        "Set to '0' to disable."
    );

    protected final ConfigProperty<Double> warpTeleportCost = this.addProperty(ConfigTypes.DOUBLE, "Warps.Teleport.Cost",
        0D,
        "Sets how much it costs (in Vault currency) to teleport to a warp.",
        "Set to '0' to disable."
    );

    public int getSaveInterval() {
        return this.warpSaveInterval.get();
    }

    
    public NightItem getDefaultIcon() {
        return this.warpDefaultIcon.get().copy();
    }

    public double getCreationCost() {
        return this.warpCreationCost.get();
    }

    public double getTeleportCost() {
        return this.warpTeleportCost.get();
    }
}
