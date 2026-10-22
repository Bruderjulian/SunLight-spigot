package su.nightexpress.sunlight.moduleImpl.friends;

import com.google.gson.reflect.TypeToken;
import su.nightexpress.sunlight.user.property.UserProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-player friends preferences and counters.
 * <p>
 * The friendship graph and the block list are relational and live in tables, so nothing about a
 * friendship belongs here. What does belong here are the things that have no row to live on: the
 * three opt-outs, and the daily request accounting, which is created at one moment and read at
 * another so "requests sent today" has no single timestamp anywhere.
 */
public class FriendsProperties {

    /** Whether this player accepts incoming friend requests at all. */
    public static final UserProperty<Boolean> REQUESTS_ENABLED = UserProperty.create("friendsRequestsEnabled",
            Boolean.class, true, true);

    /** Whether this player is told when a friend logs in or out. */
    public static final UserProperty<Boolean> LOGIN_ALERTS = UserProperty.create("friendsLoginAlerts",
            Boolean.class, true, true);

    /** Whether friends may teleport to this player without a separate request each time. */
    public static final UserProperty<Boolean> FRIEND_TELEPORT = UserProperty.create("friendsTeleport",
            Boolean.class, true, true);

    /** {@code yyyy-MM-dd} of the day the two counters below refer to. */
    public static final UserProperty<String> REQUEST_DAY_KEY = UserProperty.create("friendsRequestDayKey",
            String.class, "", true);

    /** Requests this player has sent today, server-wide. */
    public static final UserProperty<Integer> REQUEST_DAY_COUNT = UserProperty.create("friendsRequestDayCount",
            Integer.class, 0, true);

    /** Requests this player has sent today, keyed by target uuid. */
    public static final UserProperty<Map<String, Integer>> REQUEST_TARGET_DAY_COUNTS = createTargetCounterProperty();

    private FriendsProperties() {
    }

    @SuppressWarnings("unchecked")
    private static UserProperty<Map<String, Integer>> createTargetCounterProperty() {
        // The raw Map class is the runtime type; the TypeToken supplies the component types, which
        // the raw class alone would lose when the property is deserialized.
        return new UserProperty<>("friendsRequestTargetDayCounts",
                new TypeToken<Map<String, Integer>>() {
                }.getType(),
                (Class<Map<String, Integer>>) (Class<?>) Map.class,
                new HashMap<>(),
                true);
    }
}