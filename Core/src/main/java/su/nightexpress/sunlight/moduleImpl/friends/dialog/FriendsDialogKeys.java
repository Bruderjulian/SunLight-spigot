package su.nightexpress.sunlight.moduleImpl.friends.dialog;

import su.nightexpress.nightcore.ui.dialog.wrap.DialogKey;

public class FriendsDialogKeys {

    private FriendsDialogKeys() {
    }

    /**
     * Bound to a player name, resolved again when the dialog is answered rather than held as a
     * live reference, because the target may have logged off in the meantime.
     */
    public static final DialogKey<String> FRIEND_ADD    = new DialogKey<>("friend_add");
    public static final DialogKey<String> FRIEND_REMOVE = new DialogKey<>("friend_remove");
    public static final DialogKey<String> FRIEND_BLOCK  = new DialogKey<>("friend_block");
}