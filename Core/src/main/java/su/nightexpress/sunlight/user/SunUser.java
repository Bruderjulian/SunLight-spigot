package su.nightexpress.sunlight.user;

import su.nightexpress.nightcore.user.UserTemplate;
import su.nightexpress.sunlight.command.CommandKey;
import su.nightexpress.sunlight.user.cache.UserCacheContainer;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;
import su.nightexpress.sunlight.utils.TimeUtil;
import su.nightexpress.sunlight.utils.Utils;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SunUser extends UserTemplate {

    private final Map<CommandKey, Long> commandCooldowns;
    private final Map<String, Object> properties;
    private final Map<Class<? extends UserCacheContainer>, UserCacheContainer> caches;

    private final long dateCreated;

    private volatile InetAddress latestAddress;
    private volatile boolean firstTimeJoined;
    private volatile long lastOnline;

    public SunUser(UUID uuid,
            String name,
            long dateCreated,
            long lastOnline,
            InetAddress latestAddress,
            Map<CommandKey, Long> commandCooldowns,
            Map<String, Object> properties) {
        super(uuid, name);

        this.commandCooldowns = new ConcurrentHashMap<>(commandCooldowns);
        this.properties = new ConcurrentHashMap<>(properties);
        this.latestAddress = latestAddress;

        this.dateCreated = dateCreated;
        this.lastOnline = lastOnline;

        this.caches = new ConcurrentHashMap<>();
    }

    public synchronized void updateFrom(SunUser other) {
        this.setName(other.getName());
        this.setLastOnline(other.getLastOnline());
        this.setLatestAddress(other.getLatestAddress().orElse(null));

        this.commandCooldowns.clear();
        this.properties.clear();

        this.commandCooldowns.putAll(other.commandCooldowns);
        this.properties.putAll(other.properties);
    }

    public <T extends UserCacheContainer> Optional<T> getCache(Class<T> type) {
        UserCacheContainer container = this.caches.get(type);
        if (container == null)
            return Optional.empty();

        T cache = type.cast(container);
        cache.clearExpired();

        return Optional.of(cache);
    }

    public <T extends UserCacheContainer> T getCacheOrCreate(Class<T> type, Supplier<T> supplier) {
        UserCacheContainer container = this.caches.computeIfAbsent(type, key -> supplier.get());
        T cache = type.cast(container);
        cache.clearExpired();

        return cache;
    }

    public void clearCaches() {
        this.caches.values().forEach(UserCacheContainer::clear);
        this.caches.clear();
    }

    public void pruneExpiredCooldowns() {
        this.commandCooldowns.values().removeIf(TimeUtil::isPassed);
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(this.properties);
    }

    public Map<String, Object> getPropertiesToSave() {
        Map<String, Object> map = new HashMap<>();
        for (UserProperty<?> property : UserPropertyRegistry.persistentValues()) {
            Object value = this.properties.get(property.getName());
            if (value == null)
                continue;

            map.put(property.getName(), value);
        }
        return map;
    }

    public <T> boolean hasProperty(UserProperty<T> property) {
        return this.properties.containsKey(property.getName());
    }

    public <T> void setProperty(UserProperty<T> property, T value) {
        this.properties.put(property.getName(), value);
        this.markDirty();
    }

    public <T> T getPropertyOrDefault(UserProperty<T> property) {
        return this.getPropertyOr(property, property.getDefaultValue());
    }

    public <T> T getPropertyOr(UserProperty<T> property, T defaultvalue) {
        return this.getProperty(property.getName(), property.getType(), defaultvalue);
    }

    public <T> T getProperty(String name, Class<T> type, T defaultValue) {
        String key = Utils.lowercase(name);

        Object value = this.properties.get(key);
        if (value == null)
            return defaultValue;

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("User property '%s' is defined as %s, not %s".formatted(name,
                    value.getClass().getSimpleName(), type.getSimpleName()));
        }

        return type.cast(value);
    }

    public <T> void removeProperty(UserProperty<T> property) {
        if (this.properties.remove(property.getName()) != null) {
            this.markDirty();
        }
    }

    public void removeProperty(String property) {
        if (this.properties.remove(Utils.lowercase(property)) != null) {
            this.markDirty();
        }
    }

    public Long getCommandCooldown(CommandKey key) {
        Long expireDate = this.commandCooldowns.get(key);
        if (expireDate == null)
            return null;

        if (TimeUtil.isPassed(expireDate)) {
            this.commandCooldowns.remove(key, expireDate);
            return null;
        }

        return expireDate;
    }

    public void setCommandCooldown(CommandKey key, long expireDate) {
        this.commandCooldowns.put(key, expireDate);
        this.markDirty();
    }

    public void removeCommandCooldown(CommandKey key) {
        if (this.commandCooldowns.remove(key) != null) {
            this.markDirty();
        }
    }

    public Map<CommandKey, Long> getCommandCooldowns() {
        return Collections.unmodifiableMap(this.commandCooldowns);
    }

    public Map<CommandKey, Long> getCommandCooldownsToSave() {
        this.pruneExpiredCooldowns();
        return new HashMap<>(this.commandCooldowns);
    }

    public void setFirstTimeJoined(boolean firstTimeJoined) {
        this.firstTimeJoined = firstTimeJoined;
    }

    public boolean isFirstTimeJoined() {
        return this.firstTimeJoined;
    }

    public boolean hasPlayedBefore() {
        return !this.firstTimeJoined;
    }

    public long getDateCreated() {
        return this.dateCreated;
    }

    public long getLastOnline() {
        return this.lastOnline;
    }

    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }

    public Optional<InetAddress> getLatestAddress() {
        return Optional.ofNullable(this.latestAddress);
    }

    public void setLatestAddress(InetAddress address) {
        this.latestAddress = address;
    }
}
