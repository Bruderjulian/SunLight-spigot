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
 * Confirms a block. Blocking also removes the friendship, so the body says so rather than leaving
 * the player to discover it afterwards.
 */
public class FriendBlockDialog extends Dialog<String> {

    private static final TextLocale TITLE = LangEntry.builder("Friends.Dialog.Block.Title").text(title("Friend",
            "Block"));

    private static final DialogElementLocale BODY = LangEntry.builder("Friends.Dialog.Block.Body").dialogElement(400,
            "Block " + CommonPlaceholders.PLAYER_DISPLAY_NAME + "?",
            "",
            "They will be removed from your friends and cannot send you",
            "another request. You will not be able to add them again",
            "until you unblock them.");

    private final FriendsModule module;

    public FriendBlockDialog(FriendsModule module) {
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

                    this.module.blockCommand(player, friend);
                    viewer.callback();
                })
                .build();
    }
}