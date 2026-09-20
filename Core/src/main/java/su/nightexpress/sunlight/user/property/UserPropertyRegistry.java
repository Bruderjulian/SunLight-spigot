package su.nightexpress.sunlight.user.property;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserPropertyRegistry {

    private static final Map<String, UserProperty<?>> REGISTRY = new ConcurrentHashMap<>();
    private static volatile List<UserProperty<?>> PERSISTENT_CACHE = List.of();

    public static <T> UserProperty<T> register(String name, Class<T> type, T defaultValue, boolean persistent) {
        return register(new UserProperty<>(name, type, defaultValue, persistent));
    }

    public static <T> UserProperty<T> register(String name, Type type, Class<T> runtimeType, T defaultValue, boolean persistent) {
        return register(new UserProperty<>(name, type, runtimeType, defaultValue, persistent));
    }

    public static <T> UserProperty<T> register(UserProperty<T> setting) {
        REGISTRY.put(setting.getName(), setting);
        if (setting.isPersistent()) {
            List<UserProperty<?>> copy = new ArrayList<>(PERSISTENT_CACHE);
            copy.removeIf(existing -> existing.getName().equals(setting.getName()));
            copy.add(setting);
            PERSISTENT_CACHE = List.copyOf(copy);
        }
        return setting;
    }

    public static UserProperty<?> getByName(String name) {
        if (name == null) return null;
        UserProperty<?> exact = REGISTRY.get(name);
        if (exact != null) return exact;
        return REGISTRY.get(name.toLowerCase(java.util.Locale.ROOT));
    }

    public static Set<UserProperty<?>> values() {
        return Set.copyOf(REGISTRY.values());
    }

    public static List<UserProperty<?>> persistentValues() {
        return PERSISTENT_CACHE;
    }

    public static Map<String, UserProperty<?>> asMap() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
