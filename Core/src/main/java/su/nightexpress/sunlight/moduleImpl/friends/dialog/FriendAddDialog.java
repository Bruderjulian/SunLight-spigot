package su.nightexpress.sunlight.moduleImpl.friends.dialog;

import org.bukkit.entity.Player;

import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.locale.LangEntry;
import su.nightexpress.nightcore.locale.entry.DialogElementLocale;
import su.nightexpress.nightcore.locale.entry.TextLocale;
import su.nightexpress.nightcore.ui.dialog.Dialogs;
import su.nightexpress.nightcore.ui.dialog.build.DialogActions;
import su.nightexpress.nightcore.ui.dialog.build.DialogBases;
import su.nightexpress.nightcore.ui.dialog.build.DialogBodies;
import su.nightexpress.nightcore.ui.dialog.build.DialogButtons;
import su.nightexpress.nightcore.ui.dialog.build.DialogInputs;
import su.nightexpress.nightcore.ui.dialog.build.DialogTypes;
import su.nightexpress.nightcore.ui.dialog.wrap.Dialog;
import su.nightexpress.nightcore.util.placeholder.CommonPlaceholders;
import su.nightexpress.sunlight.moduleImpl.friends.FriendsModule;
import su.nightexpress.sunlight.utils.Utils;

/**
 * Asks for a name, then queues a request for it.
 * <p>
 * The name is resolved through the user manager, so it works for an offline player as well, and
 * the request goes through the same path the command uses.
 */
public class FriendAddDialog extends Dialog<String> {

    private static final String JSON_NAME = "name";

    private static final TextLocale TITLE = LangEntry.builder("Friends.Dialog.Add.Title").text(title("Friend",
            "Add"));

    private static final DialogElementLocale BODY = LangEntry.builder("Friends.Dialog.Add.Body").dialogElement(400,
            "Enter the name of the player you want to add.",
            "",
            "They will be sent a friend request.");

    private static final TextLocale INPUT_NAME = LangEntry.builder("Friends.Dialog.Add.Input.PlayerName").text(
            "Player Name");

    private final FriendsModule module;

    public FriendAddDialog(FriendsModule module) {
        this.module = module;
    }

    @Override
    public WrappedDialog create(Player player, String unused) {
        return Dialogs.builder()
                .base(DialogBases.builder(TITLE)
                        .body(DialogBodies.plainMessage(BODY))
                        .inputs(DialogInputs.text(JSON_NAME, INPUT_NAME).maxLength(24).build())
                        .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null) {
                        return;
                    }

                    String name = nbtHolder.getText(JSON_NAME, "");
                    if (name.isBlank()) {
                        return;
                    }

                    this.module.userManager().loadTargetProfile(name).thenAcceptAsync(profile -> {
                        Player target = profile == null ? null : Utils.getPlayer(profile.id());
                        if (target == null) {
                            // Requests are a conversation between two online players, so an offline
                            // one has nothing to receive the request.
                            this.module.sendPrefixed(CoreLang.ERROR_INVALID_PLAYER, player, builder -> builder
                                    .with(CommonPlaceholders.PLAYER_NAME, () -> name));
                            return;
                        }

                        if (this.module.sendRequest(player, target)) {
                            viewer.callback();
                        }
                    }, this.module.plugin()::runTask).whenComplete(Utils::printStacktrace);
                })
                .build();
    }
}