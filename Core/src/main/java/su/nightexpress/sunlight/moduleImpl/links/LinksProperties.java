package su.nightexpress.sunlight.moduleImpl.links;

import com.google.gson.reflect.TypeToken;
import su.nightexpress.sunlight.user.property.UserProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * Per-player links state.
 * <p>
 * Only reward eligibility lives here: "has this player claimed link X" is a property of the
 * player, is checked for exactly one player at a time, and therefore fits the user table. The
 * click counters and unique-click sets stay on {@link Link} itself, because those are aggregated
 * across all players and the user store has no table-scan API to compute them from per-user rows.
 */
public class LinksProperties {

    /** IDs of links whose first-click reward this player has already claimed. */
    public static final UserProperty<Set<String>> CLAIMED_REWARDS = createClaimedRewardsProperty();

    @SuppressWarnings("unchecked")
    private static UserProperty<Set<String>> createClaimedRewardsProperty() {
        // The raw Set class is the runtime type; the TypeToken supplies the element
        // type Gson needs, same as NametagsProperties.GRANTS.
        return new UserProperty<>("linksClaimedRewards",
                new TypeToken<Set<String>>() {
                }.getType(),
                (Class<Set<String>>) (Class<?>) Set.class,
                new HashSet<>(),
                true);
    }

    private LinksProperties() {
    }
}
