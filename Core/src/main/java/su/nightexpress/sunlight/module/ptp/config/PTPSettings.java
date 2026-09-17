package su.nightexpress.sunlight.module.ptp.config;

import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;

public class PTPSettings extends AbstractConfig {

    private final ConfigProperty<Integer> requestTimeout = this.addProperty(ConfigTypes.INT, "Request_Timeout",
        60,
        "Sets how long (in seconds) teleport request will be valid to accept/decline it."
    );

    private final ConfigProperty<Double> teleportCost = this.addProperty(ConfigTypes.DOUBLE, "Teleport.Cost",
        0D,
        "Sets how much it costs (in Vault currency) to accept a teleport request.",
        "The cost is charged to the player who is being teleported.",
        "Set to '0' to disable."
    );

    public int getRequestTimeout() {
        return this.requestTimeout.get();
    }

    public double getTeleportCost() {
        return this.teleportCost.get();
    }
}
