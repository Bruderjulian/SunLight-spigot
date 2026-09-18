package su.nightexpress.sunlight.user.property;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserPropertyRegistry {

    private static final Map<String, UserProperty<?>> REGISTRY = new HashMap<>();

    public static <T> UserProperty<T> register(String name, Class<T> type, T defaultValue, boolean persistent) {
        return register(new UserProperty<>(name, type, defaultValue, persistent));
    }

    public static <T> UserProperty<T> register(UserProperty<T> setting) {
        REGISTRY.put(setting.getName(), setting);
        return setting;
    }

    public static UserProperty<?> getByName(String name) {
        return REGISTRY.get(name);
    }

    public static Set<UserProperty<?>> values() {
        return Set.copyOf(REGISTRY.values());
    }
}
