package su.nightexpress.sunlight.api.provider.dto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A custom text entry, as handed out by {@link su.nightexpress.sunlight.api.provider.TextsProvider}.
 * <p>
 * A record of JDK types rather than the module's own model, because the API module must not depend
 * on Core. The lines are the raw, placeholder-bearing text; use
 * {@link TextsProvider#resolveText(String, org.bukkit.entity.Player)} to get them with the player's
 * placeholders already applied.
 *
 * @param id          the text's id, always lowercase
 * @param description the admin-facing description of what the text is for
 * @param lines       the raw text lines
 */
public record CustomTextHandle(@NotNull String id,
                               @NotNull String description,
                               @NotNull List<String> lines
) {
}