package su.nightexpress.sunlight.moduleImpl.profiles.dialog;

import su.nightexpress.nightcore.ui.dialog.wrap.DialogKey;
import su.nightexpress.sunlight.moduleImpl.profiles.PlayerProfile;

public class ProfilesDialogKeys {

    private ProfilesDialogKeys() {
    }

    public static final DialogKey<PlayerProfile> PROFILE_RENAME     = new DialogKey<>("profile_rename");
    public static final DialogKey<PlayerProfile> PROFILE_ICON       = new DialogKey<>("profile_icon");
    public static final DialogKey<PlayerProfile> PROFILE_DESCRIBE   = new DialogKey<>("profile_describe");
}
