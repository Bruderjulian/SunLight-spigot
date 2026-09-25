package su.nightexpress.sunlight.moduleImpl.nametags;

import com.google.gson.reflect.TypeToken;
import su.nightexpress.sunlight.moduleImpl.nametags.model.GrantRecord;
import su.nightexpress.sunlight.user.property.UserProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent per-player nametag state. Stored in the user {@code properties} JSON column,
 * so the declared generic type is what Gson deserializes each value back into.
 */
public class NametagsProperties {

    public static final UserProperty<String> PROFILE = UserProperty.create("nametagProfile", String.class, "", true);
    public static final UserProperty<String> RANK = UserProperty.create("nametagRank", String.class, "", true);
    public static final UserProperty<String> TAG = UserProperty.create("nametagTag", String.class, "", true);

    public static final UserProperty<Map<String, GrantRecord>> GRANTS = createGrantsProperty();

    @SuppressWarnings("unchecked")
    private static UserProperty<Map<String, GrantRecord>> createGrantsProperty() {
        // The raw Map class is the runtime type; the TypeToken supplies the element types Gson needs.
        Class<Map<String, GrantRecord>> runtimeType = (Class<Map<String, GrantRecord>>) (Class<?>) Map.class;
        return new UserProperty<>("nametagGrants",
                new TypeToken<Map<String, GrantRecord>>() { }.getType(),
                runtimeType,
                new HashMap<>(),
                true
        );
    }
}
