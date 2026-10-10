package su.nightexpress.sunlight.api.provider;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import su.nightexpress.sunlight.api.provider.dto.CustomTextHandle;

import java.util.List;

/**
 * View of the Custom Text module's text library, and sending those texts to players. Obtained via
 * {@code SunLightPlugin#textsProvider()}, which returns an empty {@link java.util.Optional} when
 * the module is disabled, so callers must treat this as a soft dependency.
 * <p>
 * This is the module to reach for when another plugin needs to show a shared message — MOTD-style
 * rules, a rules screen, an announcement — with the server's own formatting and per-text
 * permissions applied.
 * <p>
 * Every type here is a primitive, a JDK type, a Bukkit type or a record declared in this package,
 * because the API module must not depend on Core.
 */
public interface TextsProvider {

    /** Ids of every loaded text, in no particular order. */
    @NotNull List<String> getTextIds();

    /** The text with that id, or {@code null} when there is none. */
    @Nullable CustomTextHandle getText(@NotNull String id);

    /** Every loaded text, in no particular order. */
    @NotNull List<CustomTextHandle> getTexts();

    /** Whether the sender may see that text, according to its per-text permission node. */
    boolean hasPermission(@NotNull CommandSender sender, @NotNull String id);

    /**
     * The text's lines with the given player's placeholders applied, so that
     * {@code %sunlight_player%} and PlaceholderAPI expansions resolve.
     *
     * @param player the player to resolve for, or {@code null} to resolve without one
     * @return an empty list when there is no such text
     */
    @NotNull List<String> resolveText(@NotNull String id, @Nullable Player player);

    /**
     * Sends a text to a sender, resolved for them and permission-checked.
     *
     * @return {@code false} when there is no such text, or the sender may not see it
     */
    boolean showText(@NotNull CommandSender sender, @NotNull String id);
}