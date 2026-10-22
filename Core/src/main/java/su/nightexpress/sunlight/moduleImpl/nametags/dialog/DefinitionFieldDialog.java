package su.nightexpress.sunlight.moduleImpl.nametags.dialog;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Generic single-field text editor for any nametag definition, so every string property of
 * a tag, a rank or a profile is reachable from the admin GUI without a bespoke dialog each.
 * <p>
 * The caller supplies how the raw text is applied and how the edited definition is stored,
 * which keeps the string-keyed field switch that used to live here out of the picture.
 *
 * @param <T> the definition type being edited
 */
public class DefinitionFieldDialog<T> extends Dialog<T> {

    private static final DialogElementLocale DEFAULT_BODY = LangEntry.builder("Nametags.Dialog.Field.Body")
            .dialogElement(400, "Enter a new value. Leave empty to clear it.");

    private static final TextLocale INPUT = LangEntry.builder("Nametags.Dialog.Field.Input").text("Value");
    private static final String JSON_VALUE = "value";

    private final NametagsModule module;
    private final String fieldName;
    private final String title;
    private final String currentValue;
    private final @Nullable String hint;
    private final BiConsumer<T, String> applier;
    private final Consumer<T> commit;

    /**
     * @param hint overrides the default body text, e.g. to explain a comma-separated list
     */
    public DefinitionFieldDialog(@NotNull NametagsModule module,
            @NotNull String fieldName,
            @NotNull String title,
            @NotNull String currentValue,
            @Nullable String hint,
            @NotNull BiConsumer<T, String> applier,
            @NotNull Consumer<T> commit
    ) {
        this.module = module;
        this.fieldName = fieldName;
        this.title = title;
        this.currentValue = currentValue;
        this.hint = hint;
        this.applier = applier;
        this.commit = commit;
    }

    @Override
    public WrappedDialog create(Player player, T definition) {
        TextLocale title = LangEntry.builder("Nametags.Dialog.Field.Title." + this.fieldName).text(this.title);
        DialogElementLocale body = this.hint == null
                ? DEFAULT_BODY
                : LangEntry.builder("Nametags.Dialog.Field.Body." + this.fieldName).dialogElement(400, this.hint);

        return Dialogs.builder()
                .base(DialogBases.builder(title)
                    .body(DialogBodies.plainMessage(body))
                    .inputs(DialogInputs.text(JSON_VALUE, INPUT).initial(this.currentValue).maxLength(256).build())
                    .build())
                .type(DialogTypes.multiAction(DialogButtons.ok()).exitAction(DialogButtons.back()).build())
                .handleResponse(DialogActions.OK, (viewer, identifier, nbtHolder) -> {
                    if (nbtHolder == null) return;

                    this.applier.accept(definition, nbtHolder.getText(JSON_VALUE, ""));
                    this.commit.accept(definition);

                    viewer.callback();
                })
                .build();
    }

    // -----------------------------------------------------
    // List helpers, for the comma-separated multi-value fields
    // -----------------------------------------------------

    /** Renders a stored list as the comma-separated form the dialog edits. */
    public static @NotNull String joinList(@NotNull List<String> values) {
        return String.join(", ", values);
    }

    /** Parses the dialog's comma-separated form, dropping blank entries and trimming. */
    public static @NotNull List<String> splitList(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
