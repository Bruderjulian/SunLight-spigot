package su.nightexpress.sunlight.moduleImpl.profiles.dialog.impl;

import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.BOLD;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.SPRITE_ITEM;
import static su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers.YELLOW;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import su.nightexpress.nightcore.bridge.dialog.wrap.WrappedDialog;
import su.nightexpress.nightcore.bridge.dialog.wrap.input.text.WrappedMultilineOptions;
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

public class ProfileDescribeDialog extends Dialog<PlayerProfile> {

    private static final TextLocale TITLE = LangEntry.builder("Profiles.Dialog.Describe.Title")
        .text(title("Profile", "Description"));

    private static final DialogElementLocale BODY = LangEntry.builder("Profiles.Dialog.Describe.Body").dialogElement(400,
        "Enter a short description shown under the profile in the menu.");

    private static final TextLocale INPUT_DESC = LangEntry.builder("Profiles.Dialog.Describe.Input.Name")
        .text(SPRITE_ITEM.apply(Material.WRITABLE_BOOK) + " Description");

    private static final String JSON_DESC = "description";

    private final ProfilesModule module;

    public ProfileDescribeDialog(ProfilesModule module) {
        this.module = module;
    }

    @Override
    public WrappedDialog create(Player player, PlayerProfile profile) {
        ProfileManager manager = this.module.getManager();
        return Dialogs.builder()
            .base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_DESC, INPUT_DESC)
                    .maxLength(200)
                    .width(200)
                    .initial(profile.getDescription())
                    .multiline(new WrappedMultilineOptions(3, 50))
                    .build())
                .build())
            .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
            .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;
                String description = nbtHolder.getText(JSON_DESC).orElse("");
                manager.setDescription(profile.getOwnerId(), profile.getId(), description.replace("\n", " ").trim());
                this.module.sendPrefixed(ProfilesLang.DESCRIBED, player,
                    builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> profile.getName()));
                viewer.callback();
            })
            .build();
    }
}
