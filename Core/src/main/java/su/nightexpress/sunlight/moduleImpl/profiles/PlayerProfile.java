package su.nightexpress.sunlight.moduleImpl.profiles;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.google.gson.JsonElement;

import su.nightexpress.sunlight.data.DataHandler;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserProperty;
import su.nightexpress.sunlight.user.property.UserPropertyRegistry;

/**
 * One named save slot of a player.
 */
public class PlayerProfile {

    private final UUID ownerId;
    private final String id;
    private String name;
    private final long createdAt;
    private long lastPlayed;

    private VanillaState state;
    private Map<String, JsonElement> properties;

    public PlayerProfile(UUID ownerId, String id, String name) {
        this.ownerId = ownerId;
        this.id = id;
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.lastPlayed = this.createdAt;
        this.state = new VanillaState();
        this.properties = new HashMap<>();
    }

    public PlayerProfile(UUID ownerId, String id, String name, long createdAt, long lastPlayed,
                         VanillaState state, Map<String, JsonElement> properties) {
        this.ownerId = ownerId;
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.lastPlayed = lastPlayed;
        this.state = state == null ? new VanillaState() : state;
        this.properties = properties == null ? new HashMap<>() : new HashMap<>(properties);
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public long getLastPlayed() {
        return this.lastPlayed;
    }

    public void touch() {
        this.lastPlayed = System.currentTimeMillis();
    }

    public VanillaState getState() {
        return this.state;
    }

    public Map<String, JsonElement> getPropertiesRaw() {
        return Map.copyOf(this.properties);
    }

    public void setState(VanillaState state) {
        this.state = state == null ? new VanillaState() : state;
    }

    /**
     * Captures the per-profile slice of the live player into this profile.
     */
    public void captureFrom(Player player, SunUser user, ProfileScopeRegistry scopes, double economyBalance, boolean economyTracked) {
        this.state = VanillaState.capture(player, scopes, economyBalance, economyTracked);
        this.properties = new HashMap<>();
        for (Map.Entry<String, Object> entry : user.getProperties().entrySet()) {
            if (!scopes.isPropertyPerProfile(entry.getKey())) continue;
            try {
                JsonElement element = DataHandler.GSON.toJsonTree(entry.getValue());
                this.properties.put(entry.getKey(), element);
            } catch (Exception ignored) {
            }
        }
        this.touch();
    }

    /**
     * Writes the per-profile property slice back onto the live user. Global
     * properties are left untouched. Stale per-profile keys that the profile
     * does not contain are reset to their registered defaults.
     */
    public void applyPropertiesTo(SunUser user, ProfileScopeRegistry scopes) {
        for (UserProperty<?> property : UserPropertyRegistry.values()) {
            String key = property.getName();
            if (!scopes.isPropertyPerProfile(key)) continue;
            JsonElement element = this.properties.get(key);
            if (element == null || element.isJsonNull()) {
                user.removeProperty(key);
                continue;
            }
            try {
                Object value = DataHandler.GSON.fromJson(element, property.getGenericType());
                if (value == null) {
                    user.removeProperty(key);
                } else {
                    this.setRaw(user, property, value);
                }
            } catch (Exception ignored) {
                user.removeProperty(key);
            }
        }
        user.markDirty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void setRaw(SunUser user, UserProperty property, Object value) {
        user.setProperty(property, value);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", this.name);
        map.put("createdAt", this.createdAt);
        map.put("lastPlayed", this.lastPlayed);
        map.put("state", this.state.serialize());
        Map<String, String> props = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : this.properties.entrySet()) {
            props.put(entry.getKey(), DataHandler.GSON.toJson(entry.getValue()));
        }
        map.put("properties", props);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static PlayerProfile deserialize(UUID ownerId, String id, Map<String, Object> map) {
        if (map == null) return null;
        Object name = map.get("name");
        long createdAt = map.get("createdAt") instanceof Number number ? number.longValue() : System.currentTimeMillis();
        long lastPlayed = map.get("lastPlayed") instanceof Number number ? number.longValue() : createdAt;
        Object stateRaw = map.get("state");
        VanillaState state = stateRaw instanceof Map
            ? VanillaState.deserialize((Map<String, Object>) stateRaw)
            : new VanillaState();
        Map<String, JsonElement> props = new HashMap<>();
        Object propsRaw = map.get("properties");
        if (propsRaw instanceof Map<?, ?> propsMap) {
            for (Map.Entry<?, ?> entry : propsMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) continue;
                try {
                    String json = String.valueOf(entry.getValue());
                    props.put(key, DataHandler.GSON.fromJson(json, JsonElement.class));
                } catch (Exception ignored) {
                }
            }
        }
        return new PlayerProfile(ownerId, id, name == null ? id : String.valueOf(name), createdAt, lastPlayed, state, props);
    }
}
