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

public class ProfileIconDialog extends Dialog<PlayerProfile> {

    private static final TextLocale TITLE = LangEntry.builder("Profiles.Dialog.Icon.Title")
        .text(title("Profile", "Icon"));

    private static final DialogElementLocale BODY = LangEntry.builder("Profiles.Dialog.Icon.Body").dialogElement(400,
        "Enter a Bukkit material name for the profile icon (e.g. DIAMOND_SWORD, GRASS_BLOCK).",
        "Use PLAYER_HEAD for your own head.");

    private static final TextLocale INPUT_ICON = LangEntry.builder("Profiles.Dialog.Icon.Input.Name")
        .text(SPRITE_ITEM.apply(Material.PAINTING) + " Material");

    private static final String JSON_ICON = "icon";

    private final ProfilesModule module;

    public ProfileIconDialog(ProfilesModule module) {
        this.module = module;
    }

    @Override
    public WrappedDialog create(Player player, PlayerProfile profile) {
        ProfileManager manager = this.module.getManager();
        return Dialogs.builder()
            .base(DialogBases.builder(TITLE)
                .body(DialogBodies.plainMessage(BODY))
                .inputs(DialogInputs.text(JSON_ICON, INPUT_ICON).maxLength(64).initial(profile.getIcon()).build())
                .build())
            .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
            .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                if (nbtHolder == null) return;
                String raw = nbtHolder.getText(JSON_ICON, profile.getIcon()).trim().toUpperCase(java.util.Locale.ROOT);
                if (Material.matchMaterial(raw) == null) {
                    this.module.sendPrefixed(ProfilesLang.ICON_INVALID, player,
                        builder -> builder.with(SLPlaceholders.GENERIC_VALUE, () -> raw));
                    return;
                }
                manager.setIcon(profile.getOwnerId(), profile.getId(), raw);
                this.module.sendPrefixed(ProfilesLang.ICON_SET, player,
                    builder -> builder.with(SLPlaceholders.GENERIC_NAME, () -> profile.getName()));
                viewer.callback();
            })
            .build();
    }
}
