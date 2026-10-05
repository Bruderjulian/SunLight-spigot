package su.nightexpress.sunlight.moduleImpl.profiles.dialog.impl;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.BOLD;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SPRITE_ITEM;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.YELLOW;

import org.bukkit.Material;
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
import su.nightexpress.nightcore.ui.dialog.build.DialogInputs;
import su.nightexpress.nightcore.ui.dialog.build.DialogTypes;
import su.nightexpress.nightcore.ui.dialog.wrap.Dialog;
import su.nightexpress.sunlight.SLPlaceholders;
import su.nightexpress.sunlight.moduleImpl.profiles.PlayerProfile;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfileManager;
import su.nightexpress.sunlight.moduleImpl.profiles.ProfilesModule;
import su.nightexpress.sunlight.moduleImpl.profiles.config.ProfilesLang;

public class ProfileRenameDialog extends Dialog<PlayerProfile> {

    private static final TextLocale TITLE = LangEntry.builder("Profiles.Dialog.Rename.Title")
        .text(title("Profile", "Rename"));

    private static final DialogElementLocale BODY = LangEntry.builder("Profiles.Dialog.Rename.Body").dialogElement(400,
        "Enter a new name for the profile (3-16 characters: letters, numbers, underscores).");

    private static final TextLocale INPUT_NAME = LangEntry.builder("Profiles.Dialog.Rename.Input.Name")
        .text(SPRITE_ITEM.apply(Material.NAME_TAG) + " Name");

    private static final String JSON_NAME = "name";

    private final ProfilesModule module;

    public ProfileRenameDialog(ProfilesModule module) {
        this.module = module;
    }

    @Override
    public WrappedDialog create(Player player, PlayerProfile profile) {
        ProfileManager manager = this.module.getManager();
        return Dialogs.builder()
            .base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_NAME, INPUT_NAME).maxLength(16).initial(profile.getName()).build())
                .build())
            .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
            .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;
                String newName = nbtHolder.getText(JSON_NAME, profile.getName()).trim();
                switch (manager.rename(profile.getOwnerId(), profile.getId(), newName)) {
                    case OK -> this.module.sendPrefixed(ProfilesLang.RENAMED, player,
                        builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> newName));
                    case EXISTS -> this.module.sendPrefixed(ProfilesLang.CREATE_EXISTS, player,
                        builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> newName));
                    case INVALID_NAME -> this.module.sendPrefixed(ProfilesLang.CREATE_INVALID_NAME, player);
                    case NOT_FOUND -> this.module.sendPrefixed(ProfilesLang.NOT_FOUND, player,
                        builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> profile.getName()));
                }
                viewer.callback();
            })
            .build();
    }
}
