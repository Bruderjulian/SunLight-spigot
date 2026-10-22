package su.nightexpress.sunlight.moduleImpl.friends.dialog;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.DialogActions;
import su.nightexpress.nightcore.ui.dialog.build.DialogBases;
import su.nightexpress.nightcore.ui.dialog.build.DialogBodies;
import su.nightexpress.nightcore.ui.dialog.build.DialogButtons;
import su.nightexpress.nightcore.ui.dialog.build.DialogTypes;
import su.nightexpress.nightcore.ui.dialog.wrap.Dialog;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.utils.Utils;

/**
 * Confirms a friend removal. Removal takes the friendship out from both sides, so it asks first.
 */
public class FriendRemoveDialog extends Dialog<String> {

    private static final TextLocale TITLE = LangEntry.builder("Friends.Dialog.Remove.Title").text(title("Friend",
            "Removal"));

    private static final DialogElementLocale BODY = LangEntry.builder("Friends.Dialog.Remove.Body").dialogElement(400,
            "Remove " + CommonPlaceholders.PLAYER_DISPLAY_NAME + " from your friends?",
            "",
            TagWrappers.SOFT_RED.wrap("This cannot be undone!"));

    private final FriendsModule module;

    public FriendRemoveDialog(FriendsModule module) {
        this.module = module;
    }

    @Override
    public WrappedDialog create(Player player, String friendName) {
        return Dialogs.builder()
                .base(DialogBases.builder(TITLE)
                        .body(DialogBodies.plainMessage(BODY.replace(PlaceholderContext.builder()
                                .with(CommonPlaceholders.PLAYER_NAME, () -> friendName)
                                .build())))
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.confirm()).exitAction(DialogButtons.cancel()).build())
                .handleResponse(DialogActions.CONFIRM, (viewer, identifier, nbtHolder) -> {
                    Player friend = Utils.getPlayer(friendName);
                    if (friend == null) {
                        viewer.callback();
                        return;
                    }

                    this.module.removeFriendCommand(player, friend);
                    viewer.callback();
                })
                .build();
    }
}