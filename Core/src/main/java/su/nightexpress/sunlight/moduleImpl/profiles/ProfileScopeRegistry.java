package su.nightexpress.sunlight.moduleImpl.profiles;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import su.nightexpress.nightcore.config.FileConfig;

/**
 * Resolves the {@link ProfileScope} of every state key.
 *
 * <p>Every known vanilla key and {@code UserProperty} name has a built-in
 * default (see below). Server owners can override any key via
 * {@code settings.yml} under {@code Scope.Vanilla.*} and
 * {@code Scope.Properties.*}. Unknown property names default to
 * {@code GLOBAL} so third-party state never leaks across profiles by
 * accident.
 */
public class ProfileScopeRegistry {

    // Vanilla state keys.
    public static final String VANILLA_INVENTORY = "inventory";
    public static final String VANILLA_ENDERCHEST = "enderchest";
    public static final String VANILLA_LOCATION = "location";
    public static final String VANILLA_HEALTH = "health";
    public static final String VANILLA_FOOD = "food";
    public static final String VANILLA_EXPERIENCE = "experience";
    public static final String VANILLA_GAMEMODE = "gamemode";
    public static final String VANILLA_FLIGHT = "flight";
    public static final String VANILLA_POTIONS = "potion_effects";
    public static final String VANILLA_ECONOMY = "economy";
    public static final String VANILLA_ADVANCEMENTS = "advancements";
    public static final String VANILLA_RECIPES = "recipes";

    private final Map<String, ProfileScope> vanillaScopes = new HashMap<>();
    private final Map<String, ProfileScope> propertyScopes = new HashMap<>();

    public ProfileScopeRegistry() {
        this.resetDefaults();
    }

    public void resetDefaults() {
        this.vanillaScopes.clear();
        this.propertyScopes.clear();

        // Vanilla defaults: everything per-profile except advancements/recipes,
        // which are expensive to snapshot and stay global unless enabled.
        this.vanillaScopes.put(VANILLA_INVENTORY, ProfileScope.PER_PROFILE);
        this.vanillaScopes.put(VANILLA_ENDERCHEST, ProfileScope.PER_PROFILE);
        this.vanillaScopes.put(VANILLA_LOCATION, ProfileScope.PER_PROFILE);
        this.vanillaScopes.put(VANILLA_HEALTH, ProfileScope.PER_PROFILE);
        this.vanillaScopes.put(VANILLA_FOOD, ProfileScope.PER_PROFILE);
        this.vanillaScopes.put(VANILLA_EXPERIENCE, ProfileScope.PER_PROFILE);
        this.vanillaScopes.put(VANILLA_GAMEMODE, ProfileScope.PER_PROFILE);
        this.vanillaScopes.put(VANILLA_FLIGHT, ProfileScope.PER_PROFILE);
        this.vanillaScopes.put(VANILLA_POTIONS, ProfileScope.PER_PROFILE);
        this.vanillaScopes.put(VANILLA_ECONOMY, ProfileScope.PER_PROFILE);
        this.vanillaScopes.put(VANILLA_ADVANCEMENTS, ProfileScope.GLOBAL);
        this.vanillaScopes.put(VANILLA_RECIPES, ProfileScope.GLOBAL);

        // SunLight UserProperty defaults: personal toggles per-profile,
        // moderation/stats identity global. Staff-danger states default to
        // per-profile but are force-cleared on switch (see settings).
        this.setPropertyDefault("god", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("vanish", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("freeze", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("glow", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("glow_enabled", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("glow_presets", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("custom_name", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("nametagprofile", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("nametagrank", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("nametagtag", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("nametaggrants", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("nametagvisibility", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("teleport_requests", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("accept_pm", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("mentions", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("anti_phantom", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("chairs.enabled", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("chairs_enabled", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("chest_sort", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("chest_sort_enabled", ProfileScope.PER_PROFILE);
        this.setPropertyDefault("linksclaimedrewards", ProfileScope.PER_PROFILE);

        this.setPropertyDefault("afk_total_time", ProfileScope.GLOBAL);
        this.setPropertyDefault("afk_count", ProfileScope.GLOBAL);
        this.setPropertyDefault("spy_chat_social", ProfileScope.GLOBAL);
        this.setPropertyDefault("spy_log_social", ProfileScope.GLOBAL);
        this.setPropertyDefault("spy_chat_command", ProfileScope.GLOBAL);
        this.setPropertyDefault("spy_log_command", ProfileScope.GLOBAL);
        this.setPropertyDefault("spy_chat_chat", ProfileScope.GLOBAL);
        this.setPropertyDefault("spy_log_chat", ProfileScope.GLOBAL);
        this.setPropertyDefault("reportsoptout", ProfileScope.GLOBAL);
        this.setPropertyDefault("reportsrewarddaykey", ProfileScope.GLOBAL);
        this.setPropertyDefault("reportsrewarddaycount", ProfileScope.GLOBAL);
        this.setPropertyDefault("reportsrewardlastpaid", ProfileScope.GLOBAL);
        this.setPropertyDefault("playtime_total", ProfileScope.GLOBAL);
        this.setPropertyDefault("playtime_year", ProfileScope.GLOBAL);
        this.setPropertyDefault("playtime_month", ProfileScope.GLOBAL);
        this.setPropertyDefault("playtime_week", ProfileScope.GLOBAL);
        this.setPropertyDefault("playtime_day", ProfileScope.GLOBAL);
        this.setPropertyDefault("profile_active", ProfileScope.GLOBAL);
    }

    private void setPropertyDefault(String name, ProfileScope scope) {
        this.propertyScopes.put(name.toLowerCase(Locale.ROOT), scope);
    }

    /**
     * Applies {@code Scope.Vanilla.*} and {@code Scope.Properties.*} overrides
     * from the module config.
     */
    public void load(FileConfig config) {
        for (String key : this.vanillaScopes.keySet()) {
            String path = "Scope.Vanilla." + key;
            if (!config.contains(path)) {
                config.set(path, this.vanillaScopes.get(key) == ProfileScope.PER_PROFILE ? "per-profile" : "global");
                continue;
            }
            this.vanillaScopes.put(key, ProfileScope.fromString(config.getString(path), this.vanillaScopes.get(key)));
        }
        // Persisted for visibility even when untouched.
        if (!config.contains("Scope.Properties.Note")) {
            config.set("Scope.Properties.Note", "Map any UserProperty name (lowercase) to 'per-profile' or 'global'. Unknown properties default to 'global'.");
        }
        // Read back any custom property overrides present in the file.
        java.util.Set<String> customKeys;
        try {
            customKeys = config.getSection("Scope.Properties");
        } catch (Exception exception) {
            customKeys = null;
        }
        if (customKeys == null) return;
        for (String key : customKeys) {
            if (key.equalsIgnoreCase("Note")) continue;
            String path = "Scope.Properties." + key;
            Object raw = config.get(path);
            if (raw instanceof String value) {
                this.propertyScopes.put(key.toLowerCase(Locale.ROOT),
                    ProfileScope.fromString(value, ProfileScope.GLOBAL));
            }
        }
    }

    public ProfileScope vanillaScope(String key) {
        return this.vanillaScopes.getOrDefault(key, ProfileScope.GLOBAL);
    }

    public boolean isVanillaPerProfile(String key) {
        return this.vanillaScope(key).isPerProfile();
    }

    public ProfileScope propertyScope(String propertyName) {
        if (propertyName == null) return ProfileScope.GLOBAL;
        return this.propertyScopes.getOrDefault(propertyName.toLowerCase(Locale.ROOT), ProfileScope.GLOBAL);
    }

    public boolean isPropertyPerProfile(String propertyName) {
        return this.propertyScope(propertyName).isPerProfile();
    }

    public Map<String, ProfileScope> vanillaScopes() {
        return Map.copyOf(this.vanillaScopes);
    }
}
