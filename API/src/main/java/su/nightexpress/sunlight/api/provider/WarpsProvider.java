package su.nightexpress.sunlight.api.provider;

import java.util.Set;

public interface WarpsProvider {

    Set<String> getWarpIds();

    default boolean hasWarp(String id) {
        return id != null && this.getWarpIds().contains(id.toLowerCase(java.util.Locale.ROOT));
    }
}
