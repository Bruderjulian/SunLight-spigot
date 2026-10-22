package su.nightexpress.sunlight.moduleImpl.friends;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.moduleImpl.friends.config.FriendsLang;
import su.nightexpress.sunlight.user.SunUser;
import su.nightexpress.sunlight.user.property.UserProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The three per-player friends toggles, addressed by name.
 * <p>
 * Named rather than numbered because they are user-facing: the command and the settings menu take
 * the same strings, so a typo has to fail loudly in one place only.
 */
public enum FriendToggle {

    REQUESTS("requests", "Friend Requests", FriendsProperties.REQUESTS_ENABLED),
    LOGIN_ALERTS("alerts", "Login Alerts", FriendsProperties.LOGIN_ALERTS),
    TELEPORT("teleport", "Friend Teleport", FriendsProperties.FRIEND_TELEPORT);

    private final String id;
    private final String display;
    private final UserProperty<Boolean> property;

    FriendToggle(String id, String display, UserProperty<Boolean> property) {
        this.id = id;
        this.display = display;
        this.property = property;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplay() {
        return this.display;
    }

    public UserProperty<Boolean> getProperty() {
        return this.property;
    }

    /** Reads the toggle for a loaded user. */
    public boolean get(@NotNull SunUser user) {
        return user.getPropertyOrDefault(this.property);
    }

    /** Records the toggle on a loaded user. The caller persists the user. */
    public void set(@NotNull SunUser user, boolean value) {
        user.setProperty(this.property, value);
        user.markDirty();
    }

    /** The ids, for a command's suggestions and for the "not a friends setting" message. */
    public static List<String> ids() {
        return Arrays.stream(values()).map(FriendToggle::getId).toList();
    }

    @Nullable
    public static FriendToggle byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String key = id.trim().toLowerCase(Locale.ROOT);
        for (FriendToggle toggle : values()) {
            if (toggle.id.equals(key)) {
                return toggle;
            }
        }
        return null;
    }

    /** {@code Yes} / {@code No}, for a menu icon or a message. */
    public String getStateText(boolean value) {
        return su.nightexpress.nightcore.core.config.CoreLang.STATE_ENABLED_DISALBED.get(value);
    }

    /** The localized label shown by the settings menu and in command feedback. */
    public String getLocalizedDisplay() {
        return this.display;
    }
}