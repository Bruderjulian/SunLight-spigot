package su.nightexpress.sunlight.user.property;

import su.nightexpress.nightcore.util.Strings;

public class UserProperty<T> {

    private final String name;
    private final Class<T> type;
    private final T defaultValue;
    private final boolean persistent;

    public UserProperty(String name, Class<T> type, T defaultValue, boolean persistent) {
        this.name = Strings.varStyle(name).orElseThrow(() -> new IllegalArgumentException("Invalid property name"));
        this.type = type;
        this.defaultValue = defaultValue;
        this.persistent = persistent;
    }

    public static <T> UserProperty<T> create(String name, Class<T> type, T defaultValue, boolean persistent) {
        return new UserProperty<>(name, type, defaultValue, persistent);
    }

    public String getName() {
        return this.name;
    }

    public Class<T> getType() {
        return this.type;
    }

    public T getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isPersistent() {
        return this.persistent;
    }
}
