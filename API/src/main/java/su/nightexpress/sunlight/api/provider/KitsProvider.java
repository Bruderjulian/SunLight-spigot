package su.nightexpress.sunlight.api.provider;

import java.util.Set;

public interface KitsProvider {

    Set<String> getKitIdSet();

    default boolean hasKit(String id) {
        return id != null && this.getKitIdSet().contains(id.toLowerCase(java.util.Locale.ROOT));
    }
}
