package su.nightexpress.sunlight.moduleImpl.nametags.dialog;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
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
import su.nightexpress.sunlight.moduleImpl.nametags.NametagsModule;
import su.nightexpress.sunlight.moduleImpl.nametags.model.TagDefinition;

/**
 * Generic single-field text editor for a tag, so every string property is reachable from
 * the admin GUI without a bespoke dialog per field.
 */
public class TagFieldDialog extends Dialog<TagDefinition> {

    private static final DialogElementLocale BODY = LangEntry.builder("Nametags.Dialog.TagField.Body")
            .dialogElement(400, "Enter a new value. Leave empty to clear it.");

    private static final TextLocale INPUT = LangEntry.builder("Nametags.Dialog.TagField.Input").text("Value");
    private static final String JSON_VALUE = "value";

    private final NametagsModule module;
    private final String fieldName;
    private final String title;
    private final String currentValue;

    public TagFieldDialog(@NotNull NametagsModule module,
            @NotNull String fieldName,
            @NotNull String title,
            @NotNull String currentValue
    ) {
        this.module = module;
        this.fieldName = fieldName;
        this.title = title;
        this.currentValue = currentValue;
    }

    @Override
    public WrappedDialog create(Player player, TagDefinition tag) {
        TextLocale title = LangEntry.builder("Nametags.Dialog.TagField.Title." + this.fieldName).text(this.title);

        return Dialogs.builder()
                .base(DialogBases.builder(title)
                    .body(DialogBodies.plainMessage(BODY))
                    .inputs(DialogInputs.text(JSON_VALUE, INPUT).initial(this.currentValue).maxLength(256).build())
                    .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null) return;

                    this.applyValue(tag, nbtHolder.getText(JSON_VALUE, ""));
                    this.module.getCatalog().putTag(tag);
                    this.module.saveCatalog();

                    viewer.callback();
                })
                .build();
    }

    private void applyValue(@NotNull TagDefinition tag, @NotNull String value) {
        switch (this.fieldName) {
            case "display" -> tag.setDisplay(value);
            case "prefix" -> tag.setPrefix(value);
            case "suffix" -> tag.setSuffix(value);
            case "color" -> tag.setColor(value);
            case "permission" -> tag.setPermission(value);
            case "icon" -> tag.setIconMaterial(value);
            default -> throw new IllegalArgumentException("Unknown tag field: " + this.fieldName);
        }
    }
}
