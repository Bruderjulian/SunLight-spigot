package su.nightexpress.sunlight.user.property;

import su.nightexpress.nightcore.util.Strings;

import java.lang.reflect.Type;

public class UserProperty<T> {

    private final String name;
    private final Class<T> type;
    private final Type genericType;
    private final T defaultValue;
    private final boolean persistent;

    public UserProperty(String name, Class<T> type, T defaultValue, boolean persistent) {
        this(name, (Type) type, type, defaultValue, persistent);
    }

    public UserProperty(String name, Type genericType, Class<T> runtimeType, T defaultValue, boolean persistent) {
        String normalized = Strings.varStyle(name).orElseThrow(() -> new IllegalArgumentException("Invalid property name"));
        this.name = normalized.toLowerCase(java.util.Locale.ROOT);
        this.genericType = genericType == null ? runtimeType : genericType;
        this.type = runtimeType;
        this.defaultValue = defaultValue;
        this.persistent = persistent;
    }

    public static <T> UserProperty<T> create(String name, Class<T> type, T defaultValue, boolean persistent) {
        return new UserProperty<>(name, type, defaultValue, persistent);
    }

    public static <T> UserProperty<T> create(String name, Type genericType, Class<T> runtimeType, T defaultValue, boolean persistent) {
        return new UserProperty<>(name, genericType, runtimeType, defaultValue, persistent);
    }

    public String getName() {
        return this.name;
    }

    public Class<T> getType() {
        return this.type;
    }

    public Type getGenericType() {
        return this.genericType;
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isPersistent() {
        return this.persistent;
    }
}
